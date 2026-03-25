#!/bin/bash

# ==============================================================================
# SQL Judge Engine - Health Check & Service Readiness Script
# ==============================================================================
# Checks if all microservices are running and healthy
# ==============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/config.sh"

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
# Service Health Checks
# ==============================================================================

SERVICES=(
    "user-service:${USER_SERVICE_URL}"
    "problem-service:${PROBLEM_SERVICE_URL}"
    "submission-service:${SUBMISSION_SERVICE_URL}"
    "result-service:${RESULT_SERVICE_URL}"
    "container-manager:${CONTAINER_MANAGER_URL:-http://localhost:8085}"
    "judge-service:${JUDGE_SERVICE_URL:-http://localhost:8084}"
)

HEALTH_ENDPOINTS=(
    "user-service:${USER_SERVICE_URL}/api/user/me"
    "problem-service:${PROBLEM_SERVICE_URL}/api/problem"
    "submission-service:${SUBMISSION_SERVICE_URL}/api/submission"
    "result-service:${RESULT_SERVICE_URL}/api/result/leaderboard"
    "container-manager:${CONTAINER_MANAGER_URL:-http://localhost:8085}/health"
    "judge-service:${JUDGE_SERVICE_URL:-http://localhost:8084}/health"
)

# ==============================================================================
# Check Service Connectivity (TCP)
# ==============================================================================

check_service_connectivity() {
    echo ""
    echo -e "${COLOR_BLUE}========================================${COLOR_RESET}"
    echo -e "${COLOR_BLUE}Checking Service Connectivity${COLOR_RESET}"
    echo -e "${COLOR_BLUE}========================================${COLOR_RESET}"

    local all_passed=true

    for svc in "${SERVICES[@]}"; do
        local name="${svc%%:*}"
        local url="${svc##*:}"
        local port="${url##*:}"
        port="${port%/}"  # Remove trailing slash

        # Extract host and port
        local host="${url%:*}"
        host="${host#http://}"
        host="${host#https://}"

        echo -n "Checking $name ($host:$port)... "

        if timeout 5 bash -c "echo > /dev/tcp/$host/${port}" 2>/dev/null; then
            log_success "CONNECTED"
        else
            log_error "NOT REACHABLE"
            all_passed=false
        fi
    done

    echo ""
    if [ "$all_passed" = true ]; then
        log_success "All services are reachable"
        return 0
    else
        log_error "Some services are not reachable"
        return 1
    fi
}

# ==============================================================================
# Check HTTP Health Endpoints
# ==============================================================================

check_http_health() {
    echo ""
    echo -e "${COLOR_BLUE}========================================${COLOR_RESET}"
    echo -e "${COLOR_BLUE}Checking HTTP Health Endpoints${COLOR_RESET}"
    echo -e "${COLOR_BLUE}========================================${COLOR_RESET}"

    local all_passed=true

    for endpoint in "${HEALTH_ENDPOINTS[@]}"; do
        local name="${endpoint%%:*}"
        local url="${endpoint##*:}"

        echo -n "Checking $name ($url)... "

        # Try with unauthenticated endpoint first
        local status=$(curl -s -o /dev/null -w "%{http_code}" --max-time 10 "$url" 2>/dev/null || echo "000")

        case "$status" in
            200|201|202|204)
                log_success "HTTP $status"
                ;;
            401|403)
                # Auth required, but service is responding
                log_success "HTTP $status (auth required, service is UP)"
                ;;
            000)
                log_error "CONNECTION FAILED"
                all_passed=false
                ;;
            *)
                log_warning "HTTP $status"
                ;;
        esac
    done

    echo ""
    if [ "$all_passed" = true ]; then
        log_success "All services are responding"
        return 0
    else
        log_error "Some services are not responding"
        return 1
    fi
}

# ==============================================================================
# Check Infrastructure Services
# ==============================================================================

check_infrastructure() {
    echo ""
    echo -e "${COLOR_BLUE}========================================${COLOR_RESET}"
    echo -e "${COLOR_BLUE}Checking Infrastructure${COLOR_RESET}"
    echo -e "${COLOR_BLUE}========================================${COLOR_RESET}"

    local all_passed=true

    # Check MySQL
    echo -n "Checking MySQL (localhost:3306)... "
    if timeout 5 bash -c "echo > /dev/tcp/localhost/3306" 2>/dev/null; then
        log_success "MySQL is running"
    else
        log_error "MySQL is not reachable"
        all_passed=false
    fi

    # Check RabbitMQ Management
    echo -n "Checking RabbitMQ (localhost:5672)... "
    if timeout 5 bash -c "echo > /dev/tcp/localhost/5672" 2>/dev/null; then
        log_success "RabbitMQ is running"
    else
        log_error "RabbitMQ is not reachable"
        all_passed=false
    fi

    # Check Docker
    echo -n "Checking Docker (localhost:2375)... "
    if command -v docker &> /dev/null; then
        if docker info &> /dev/null; then
            log_success "Docker is running"
        else
            log_warning "Docker is not accessible"
        fi
    else
        log_warning "Docker CLI not found"
    fi

    echo ""
    if [ "$all_passed" = true ]; then
        log_success "All infrastructure services are ready"
        return 0
    else
        log_warning "Some infrastructure services may not be ready"
        return 0  # Don't fail, as docker-compose might still be starting
    fi
}

# ==============================================================================
# Wait for Services to Be Ready
# ==============================================================================

wait_for_services() {
    local max_wait="${1:-60}"
    local interval="${2:-5}"
    local elapsed=0

    echo ""
    echo -e "${COLOR_BLUE}========================================${COLOR_RESET}"
    echo -e "${COLOR_BLUE}Waiting for Services (max ${max_wait}s)${COLOR_RESET}"
    echo -e "${COLOR_BLUE}========================================${COLOR_RESET}"

    while [ $elapsed -lt $max_wait ]; do
        local all_ready=true

        for endpoint in "${HEALTH_ENDPOINTS[@]}"; do
            local name="${endpoint%%:*}"
            local url="${endpoint##*:}"

            status=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 "$url" 2>/dev/null || echo "000")

            if [ "$status" = "000" ]; then
                all_ready=false
                break
            fi
        done

        if [ "$all_ready" = true ]; then
            log_success "All services are ready!"
            return 0
        fi

        echo "Waiting... ${elapsed}s / ${max_wait}s"
        sleep $interval
        elapsed=$((elapsed + interval))
    done

    log_error "Timeout waiting for services"
    return 1
}

# ==============================================================================
# Print Service Status Summary
# ==============================================================================

print_status_summary() {
    echo ""
    echo -e "${COLOR_BLUE}========================================${COLOR_RESET}"
    echo -e "${COLOR_BLUE}Service Status Summary${COLOR_RESET}"
    echo -e "${COLOR_BLUE}========================================${COLOR_RESET}"
    printf "%-25s %-15s %s\n" "Service" "Status" "Endpoint"
    echo "------------------------------------------------------------"

    for endpoint in "${HEALTH_ENDPOINTS[@]}"; do
        local name="${endpoint%%:*}"
        local url="${endpoint##*:}"

        status=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 "$url" 2>/dev/null || echo "000")

        case "$status" in
            200|201|202|204)
                status_text="${COLOR_GREEN}UP${COLOR_RESET}"
                ;;
            401|403)
                status_text="${COLOR_GREEN}UP (auth)${COLOR_RESET}"
                ;;
            000)
                status_text="${COLOR_RED}DOWN${COLOR_RESET}"
                ;;
            *)
                status_text="${COLOR_YELLOW}UNKNOWN${COLOR_RESET}"
                ;;
        esac

        printf "%-25s %-25s %s\n" "$name" -e "$status_text" "$url"
    done

    echo ""
}

# ==============================================================================
# Main
# ==============================================================================

main() {
    echo ""
    echo -e "${COLOR_BLUE}========================================${COLOR_RESET}"
    echo -e "${COLOR_BLUE}SQL Judge Engine - Health Check${COLOR_RESET}"
    echo -e "${COLOR_BLUE}========================================${COLOR_RESET}"

    local check_type="${1:-all}"

    case "$check_type" in
        connectivity)
            check_service_connectivity
            ;;
        http)
            check_http_health
            ;;
        infra)
            check_infrastructure
            ;;
        wait)
            wait_for_services "${2:-60}" "${3:-5}"
            ;;
        all)
            check_infrastructure
            check_service_connectivity
            check_http_health
            print_status_summary
            ;;
        *)
            echo "Usage: $0 [connectivity|http|infra|wait|all]"
            exit 1
            ;;
    esac
}

main "$@"
