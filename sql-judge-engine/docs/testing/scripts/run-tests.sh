#!/bin/bash

# ==============================================================================
# SQL Judge Engine - Black Box Testing Runner
# ==============================================================================
# Main entry point for running black box tests
# Usage: ./run-tests.sh [options]
#        ./run-tests.sh [user|problem|submission|result|e2e|all]
# ==============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/config.sh"
source "${SCRIPT_DIR}/common.sh"

# Colors (override from config if needed)
COLOR_RED='\033[0;31m'
COLOR_GREEN='\033[0;32m'
COLOR_YELLOW='\033[1;33m'
COLOR_BLUE='\033[0;34m'
COLOR_RESET='\033[0m'

# ==============================================================================
# Print Usage
# ==============================================================================

print_usage() {
    cat <<EOF
SQL Judge Engine - Black Box Testing Runner

Usage: $0 [OPTIONS] [TEST_SUITE]

Options:
    -h, --help              Show this help message
    -v, --verbose           Verbose output
    -s, --service URL       Override service URL (e.g., http://localhost:8080)
    -t, --timeout SECONDS   HTTP timeout (default: 30)

Test Suites:
    user                    Run user-service tests only
    problem                 Run problem-service tests only
    submission              Run submission-service tests only
    result                  Run result-service tests only
    e2e                     Run end-to-end tests only
    all                     Run all test suites (default)

Examples:
    $0 all                  Run all tests
    $0 user problem          Run user and problem tests
    $0 -s http://prod:8080 e2e    Run E2E tests against production

EOF
}

# ==============================================================================
# Check Prerequisites
# ==============================================================================

check_prerequisites() {
    print_section "CHECKING PREREQUISITES"

    local failed=0

    # Check curl
    if command -v curl &> /dev/null; then
        log_success "curl is installed"
    else
        log_error "curl is not installed"
        failed=1
    fi

    # Check jq (for better JSON parsing, optional)
    if command -v jq &> /dev/null; then
        log_success "jq is installed (optional, will use grep/sed fallback)"
    else
        log_warning "jq is not installed (optional, will use grep/sed fallback)"
    fi

    # Check if at least one service is reachable
    log_info "Checking service connectivity..."

    local services=(
        "user:${USER_SERVICE_URL}"
        "problem:${PROBLEM_SERVICE_URL}"
    )

    for svc in "${services[@]}"; do
        local name="${svc%%:*}"
        local url="${svc##*:}"

        if curl -s --max-time 5 "${url}/api/user/login" &> /dev/null; then
            log_success "${name}-service is reachable"
        else
            log_warning "${name}-service is not reachable at ${url}"
        fi
    done

    if [ $failed -eq 1 ]; then
        log_error "Prerequisites check failed"
        exit 1
    fi

    log_success "Prerequisites check passed"
}

# ==============================================================================
# Setup Test Environment
# ==============================================================================

setup_environment() {
    print_section "SETTING UP TEST ENVIRONMENT"

    # Create test results directory
    mkdir -p "${TEST_RESULT_DIR}"

    # Clear previous test data
    rm -f "${TEST_RESULT_DIR}/test-data.env"

    # Record test start time
    date > "${TEST_RESULT_DIR}/test-start-time.txt"

    log_success "Test environment ready"
    log_info "Test results will be saved to: ${TEST_RESULT_DIR}"
}

# ==============================================================================
# Run Test Suites
# ==============================================================================

run_user_tests() {
    print_section "RUNNING USER-SERVICE TESTS"
    "${SCRIPT_DIR}/test-user-service.sh"
}

run_problem_tests() {
    print_section "RUNNING PROBLEM-SERVICE TESTS"
    "${SCRIPT_DIR}/test-problem-service.sh"
}

run_submission_tests() {
    print_section "RUNNING SUBMISSION-SERVICE TESTS"
    "${SCRIPT_DIR}/test-submission-service.sh"
}

run_result_tests() {
    print_section "RUNNING RESULT-SERVICE TESTS"
    "${SCRIPT_DIR}/test-result-service.sh"
}

run_e2e_tests() {
    print_section "RUNNING END-TO-END TESTS"
    "${SCRIPT_DIR}/test-e2e.sh"
}

# ==============================================================================
# Print Final Summary
# ==============================================================================

print_summary() {
    local total_passed=0
    local total_failed=0

    print_section "FINAL TEST SUMMARY"

    # Collect results from all test runs
    if [ -f "${TEST_RESULT_DIR}/user-tests.txt" ]; then
        source "${TEST_RESULT_DIR}/user-tests.txt" 2>/dev/null || true
    fi

    echo ""
    echo -e "${COLOR_BLUE}========================================${COLOR_RESET}"
    echo -e "${COLOR_BLUE}TEST EXECUTION COMPLETED${COLOR_RESET}"
    echo -e "${COLOR_BLUE}========================================${COLOR_RESET}"
    echo ""
    echo "Results saved to: ${TEST_RESULT_DIR}"
    echo ""

    # Record test end time
    date > "${TEST_RESULT_DIR}/test-end-time.txt"

    if [ $total_failed -eq 0 ]; then
        echo -e "${COLOR_GREEN}ALL TESTS PASSED!${COLOR_RESET}"
        return 0
    else
        echo -e "${COLOR_RED}SOME TESTS FAILED!${COLOR_RESET}"
        return 1
    fi
}

# ==============================================================================
# Interactive Mode
# ==============================================================================

interactive_menu() {
    clear
    echo -e "${COLOR_BLUE}========================================${COLOR_RESET}"
    echo -e "${COLOR_BLUE}SQL Judge Engine - Black Box Testing${COLOR_RESET}"
    echo -e "${COLOR_BLUE}========================================${COLOR_RESET}"
    echo ""
    echo "Please select a test suite to run:"
    echo ""
    echo "  1. User Service Tests"
    echo "  2. Problem Service Tests"
    echo "  3. Submission Service Tests"
    echo "  4. Result Service Tests"
    echo "  5. End-to-End Tests"
    echo "  6. All Tests"
    echo "  0. Exit"
    echo ""
    read -p "Enter your choice [0-6]: " choice

    case $choice in
        1) run_user_tests ;;
        2) run_problem_tests ;;
        3) run_submission_tests ;;
        4) run_result_tests ;;
        5) run_e2e_tests ;;
        6) run_all ;;
        0) exit 0 ;;
        *) echo "Invalid choice" ;;
    esac
}

# ==============================================================================
# Run All Tests
# ==============================================================================

run_all() {
    print_section "RUNNING ALL BLACK BOX TESTS"

    local exit_code=0

    run_user_tests || exit_code=1
    echo ""

    run_problem_tests || exit_code=1
    echo ""

    run_submission_tests || exit_code=1
    echo ""

    run_result_tests || exit_code=1
    echo ""

    run_e2e_tests || exit_code=1
    echo ""

    if [ $exit_code -eq 0 ]; then
        log_success "ALL TEST SUITES COMPLETED SUCCESSFULLY"
    else
        log_error "SOME TEST SUITES HAD FAILURES"
    fi

    return $exit_code
}

# ==============================================================================
# Main Entry Point
# ==============================================================================

main() {
    local verbose=false
    local test_suites=("all")

    # Parse arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            -h|--help)
                print_usage
                exit 0
                ;;
            -v|--verbose)
                verbose=true
                shift
                ;;
            -s|--service)
                export GATEWAY_URL="$2"
                shift 2
                ;;
            -t|--timeout)
                export HTTP_TIMEOUT="$2"
                shift 2
                ;;
            user|problem|submission|result|e2e|all)
                test_suites=("$1")
                shift
                ;;
            *)
                log_error "Unknown option: $1"
                print_usage
                exit 1
                ;;
        esac
    done

    # Check prerequisites
    check_prerequisites

    # Setup environment
    setup_environment

    # Run selected test suites
    local exit_code=0

    for suite in "${test_suites[@]}"; do
        case $suite in
            user)
                run_user_tests || exit_code=1
                ;;
            problem)
                run_problem_tests || exit_code=1
                ;;
            submission)
                run_submission_tests || exit_code=1
                ;;
            result)
                run_result_tests || exit_code=1
                ;;
            e2e)
                run_e2e_tests || exit_code=1
                ;;
            all)
                run_all || exit_code=1
                ;;
        esac
    done

    # Print summary
    print_summary

    exit $exit_code
}

# Run main if executed directly
if [ "${BASH_SOURCE[0]}" = "$0" ]; then
    # If no arguments, show interactive menu
    if [ $# -eq 0 ]; then
        interactive_menu
    else
        main "$@"
    fi
fi
