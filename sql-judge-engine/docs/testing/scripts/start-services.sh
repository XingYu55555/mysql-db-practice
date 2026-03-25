#!/bin/bash

# ==============================================================================
# SQL Judge Engine - Start Microservices
# ==============================================================================
# Starts all microservices using docker-compose
# ==============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/../../../" && pwd)"

COLOR_RED='\033[0;31m'
COLOR_GREEN='\033[0;32m'
COLOR_YELLOW='\033[1;33m'
COLOR_BLUE='\033[0;34m'
COLOR_RESET='\033[0m'

log_info() {
    echo -e "${COLOR_BLUE}[INFO]${COLOR_RESET} $1"
}

log_success() {
    echo -e "${COLOR_GREEN}[SUCCESS]${COLOR_RESET} $1"
}

log_error() {
    echo -e "${COLOR_RED}[ERROR]${COLOR_RESET} $1"
}

log_warning() {
    echo -e "${COLOR_YELLOW}[WARNING]${COLOR_RESET} $1"
}

# ==============================================================================
# Check Prerequisites
# ==============================================================================

check_prerequisites() {
    log_info "Checking prerequisites..."

    # Check Docker
    if ! command -v docker &> /dev/null; then
        log_error "Docker is not installed"
        exit 1
    fi

    if ! docker info &> /dev/null; then
        log_error "Docker daemon is not running"
        log_error "Please start Docker and try again"
        exit 1
    fi

    # Check Docker Compose
    if ! command -v docker-compose &> /dev/null; then
        log_error "docker-compose is not installed"
        exit 1
    fi

    log_success "Prerequisites check passed"
}

# ==============================================================================
# Stop Existing Services
# ==============================================================================

stop_services() {
    log_info "Stopping existing services..."

    cd "$PROJECT_DIR"

    if docker-compose ps &> /dev/null; then
        docker-compose down 2>/dev/null || true
        log_success "Existing services stopped"
    else
        log_info "No running services found"
    fi
}

# ==============================================================================
# Build Services
# ==============================================================================

build_services() {
    log_info "Building services..."

    cd "$PROJECT_DIR"

    docker-compose build --parallel
    log_success "Services built"
}

# ==============================================================================
# Start Services
# ==============================================================================

start_services() {
    log_info "Starting services..."

    cd "$PROJECT_DIR"

    # Start infrastructure first (MySQL, RabbitMQ, Docker)
    docker-compose up -d mysql rabbitmq docker

    # Wait for MySQL to be ready
    log_info "Waiting for MySQL to be ready..."
    local max_wait=60
    local elapsed=0
    while [ $elapsed -lt $max_wait ]; do
        if docker-compose exec -T mysql mysqladmin ping -h localhost -uroot -proot_password &> /dev/null; then
            log_success "MySQL is ready"
            break
        fi
        sleep 2
        elapsed=$((elapsed + 2))
    done

    if [ $elapsed -ge $max_wait ]; then
        log_warning "MySQL may not be ready, continuing..."
    fi

    # Start application services
    docker-compose up -d gateway user-service problem-service submission-service result-service container-manager judge-service

    log_success "Services started"
}

# ==============================================================================
# Wait for Services to Be Ready
# ==============================================================================

wait_for_services() {
    log_info "Waiting for services to be ready (max 120 seconds)..."

    cd "$SCRIPT_DIR"

    ./check-health.sh wait 120 5
}

# ==============================================================================
# Print Service Status
# ==============================================================================

print_status() {
    cd "$PROJECT_DIR"

    echo ""
    echo -e "${COLOR_BLUE}========================================${COLOR_RESET}"
    echo -e "${COLOR_BLUE}Service Status${COLOR_RESET}"
    echo -e "${COLOR_BLUE}========================================${COLOR_RESET}"

    docker-compose ps

    echo ""
    echo -e "${COLOR_BLUE}========================================${COLOR_RESET}"
    echo -e "${COLOR_BLUE}Port Mappings${COLOR_RESET}"
    echo -e "${COLOR_BLUE}========================================${COLOR_RESET}"
    printf "%-25s %s\n" "Service" "Port"
    echo "----------------------------------------"
    printf "%-25s %s\n" "Gateway" "localhost:8080"
    printf "%-25s %s\n" "user-service" "localhost:8081"
    printf "%-25s %s\n" "problem-service" "localhost:8082"
    printf "%-25s %s\n" "submission-service" "localhost:8083"
    printf "%-25s %s\n" "result-service" "localhost:8086"
    printf "%-25s %s\n" "container-manager" "localhost:8085"
    printf "%-25s %s\n" "judge-service" "localhost:8084"
    printf "%-25s %s\n" "RabbitMQ" "localhost:5672 (AMQP), localhost:15672 (Management)"
    printf "%-25s %s\n" "MySQL" "localhost:3306"
    echo ""
}

# ==============================================================================
# Initialize Database
# ==============================================================================

init_database() {
    log_info "Initializing database..."

    cd "$PROJECT_DIR"

    # Check if users table exists
    local tables=$(docker-compose exec -T mysql mysql -uroot -proot_password business_db -e "SHOW TABLES;" 2>/dev/null || echo "")

    if echo "$tables" | grep -q "users"; then
        log_info "Database already initialized (users table exists)"
    else
        log_info "Running database initialization script..."
        docker-compose exec -T mysql mysql -uroot -proot_password business_db < configs/mysql/init.sql 2>/dev/null || {
            log_warning "Failed to run init.sql, database may already be initialized"
        }
        log_success "Database initialization completed"
    fi
}

# ==============================================================================
# Create Test Users
# ==============================================================================

create_test_users() {
    log_info "Creating test users..."

    cd "$PROJECT_DIR"

    # Check if test users already exist
    local existing=$(docker-compose exec -T mysql mysql -uroot -proot_password business_db -e "SELECT username FROM users WHERE username IN ('teacher1', 'student1');" 2>/dev/null || echo "")

    if echo "$existing" | grep -q "teacher1"; then
        log_info "Test users already exist, skipping..."
    else
        log_info "Inserting test users via SQL..."
        docker-compose exec -T mysql mysql -uroot -proot_password business_db -e "
            INSERT INTO users (username, password, role, email, created_at, updated_at)
            VALUES ('teacher1', '\$2a\$10\$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.rsS7HmGyDYLKwC5S4y', 'TEACHER', 'teacher@test.com', NOW(), NOW())
            ON DUPLICATE KEY UPDATE username=username;
            INSERT INTO users (username, password, role, email, created_at, updated_at)
            VALUES ('student1', '\$2a\$10\$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.rsS7HmGyDYLKwC5S4y', 'STUDENT', 'student@test.com', NOW(), NOW())
            ON DUPLICATE KEY UPDATE username=username;
        " 2>/dev/null || true
        log_success "Test users created"
    fi

    log_success "Test users ready"
}

# ==============================================================================
# Stop Services
# ==============================================================================

stop() {
    log_info "Stopping all services..."

    cd "$PROJECT_DIR"
    docker-compose down

    log_success "All services stopped"
}

# ==============================================================================
# Show Logs
# ==============================================================================

show_logs() {
    cd "$PROJECT_DIR"

    local service="${1:-}"

    if [ -n "$service" ]; then
        docker-compose logs -f "$service"
    else
        docker-compose logs -f
    fi
}

# ==============================================================================
# Main
# ==============================================================================

usage() {
    cat <<EOF
SQL Judge Engine - Service Management Script

Usage: $0 [command]

Commands:
    start       Start all microservices (default)
    stop        Stop all microservices
    restart     Restart all microservices
    build       Build services without starting
    logs        Show logs (use: $0 logs [service])
    status      Show service status
    health      Run health check
    init        Initialize database and test data
    help        Show this help message

Examples:
    $0 start        # Start all services
    $0 stop         # Stop all services
    $0 restart      # Restart all services
    $0 logs         # Show all logs
    $0 logs user-service  # Show user-service logs
    $0 health       # Run health check

EOF
}

main() {
    local command="${1:-start}"

    case "$command" in
        start)
            check_prerequisites
            stop_services
            build_services
            start_services
            init_database
            create_test_users
            wait_for_services
            print_status
            ;;
        stop)
            stop
            ;;
        restart)
            stop
            start
            ;;
        build)
            check_prerequisites
            build_services
            ;;
        logs)
            show_logs "$2"
            ;;
        status)
            cd "$PROJECT_DIR" && docker-compose ps
            ;;
        health)
            cd "$SCRIPT_DIR" && ./check-health.sh all
            ;;
        init)
            init_database
            create_test_users
            ;;
        help|--help|-h)
            usage
            ;;
        *)
            log_error "Unknown command: $command"
            usage
            exit 1
            ;;
    esac
}

main "$@"
