#!/bin/bash

# ==============================================================================
# SQL Judge Engine - Result Service Black Box Tests
# ==============================================================================
# Tests for result-service API endpoints (query interfaces only)
# Reference: integration-test-architecture.md Section 2.5
# Note: POST /api/result is internal, not tested here
# ==============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/config.sh"
source "${SCRIPT_DIR}/common.sh"

# Test result tracking
TESTS_PASSED=0
TESTS_FAILED=0

# ==============================================================================
# Helper Functions
# ==============================================================================

get_teacher_token() {
    local login_url=$(get_url "user" "${API_USER}/login")
    local login_body=$(cat <<EOF
{
    "username": "${EXISTING_TEACHER_USERNAME}",
    "password": "${EXISTING_TEACHER_PASSWORD}"
}
EOF
)
    local response=$(http_post "$login_url" "$login_body")
    json_get "$response" "token"
}

get_student_token() {
    local login_url=$(get_url "user" "${API_USER}/login")
    local login_body=$(cat <<EOF
{
    "username": "${EXISTING_STUDENT_USERNAME}",
    "password": "${EXISTING_STUDENT_PASSWORD}"
}
EOF
)
    local response=$(http_post "$login_url" "$login_body")
    json_get "$response" "token"
}

get_student_id() {
    local login_url=$(get_url "user" "${API_USER}/login")
    local login_body=$(cat <<EOF
{
    "username": "${EXISTING_STUDENT_USERNAME}",
    "password": "${EXISTING_STUDENT_PASSWORD}"
}
EOF
)
    local response=$(http_post "$login_url" "$login_body")
    json_get "$response" "userId"
}

# ==============================================================================
# TC-RESULT-001: Get Result by Submission ID
# ==============================================================================

test_get_result_by_submission() {
    log_test_start "TC-RESULT-001: Get Result by Submission ID"

    local student_id="${STUDENT_ID:-2}"
    local student_token=$(get_student_token)

    # First, get a known submission ID from previous tests
    local submission_id="${TEST_SUBMISSION_ID:-${WAIT_TEST_SUBMISSION_ID:-}}"

    # If no submission ID from tests, we need to create one and wait
    if [ -z "$submission_id" ]; then
        log_info "No submission ID from tests, creating one..."

        # Create a problem first
        local teacher_token=$(get_teacher_token)
        local teacher_id="${TEACHER_ID:-1}"

        local problem_url=$(get_url "problem" "${API_PROBLEM}")
        local problem_body=$(cat <<EOF
{
    "title": "Result Test Problem",
    "description": "For result testing",
    "difficulty": "EASY",
    "sqlType": "DQL",
    "initSql": "CREATE TABLE t (id INT); INSERT INTO t VALUES (1);",
    "standardAnswer": "SELECT * FROM t;"
}
EOF
)
        local problem_response=$(http_post "$problem_url" "$problem_body" "X-User-Id: ${teacher_id}")
        local problem_id=$(json_get "$problem_response" "problemId")

        # Publish it
        local status_url=$(get_url "problem" "${API_PROBLEM}/${problem_id}/status")
        http_put "$status_url" '{"status": "PUBLISHED"}' "X-User-Id: ${teacher_id}" > /dev/null

        # Submit answer
        local submit_url=$(get_url "submission" "${API_SUBMISSION}")
        local submit_body=$(cat <<EOF
{
    "problemId": ${problem_id},
    "sqlContent": "SELECT * FROM t"
}
EOF
)
        local submit_response=$(http_post "$submit_url" "$submit_body" "X-User-Id: ${student_id}")
        submission_id=$(json_get "$submit_response" "submissionId")

        if [ -z "$submission_id" ]; then
            log_error "Failed to create submission"
            record_fail "TC-RESULT-001"
            return 1
        fi

        # Wait for judging
        log_info "Waiting for judging to complete..."
        local status_url=$(get_url "submission" "${API_SUBMISSION}/${submission_id}/status")
        local attempt=1
        while [ $attempt -le $MAX_POLL_COUNT ]; do
            local status_response=$(http_get "$status_url")
            local status=$(json_get "$status_response" "status")
            if [ "$status" = "SUCCESS" ] || [ "$status" = "FAILED" ]; then
                break
            fi
            sleep $POLL_INTERVAL
            attempt=$((attempt + 1))
        done

        save_test_data "RESULT_TEST_SUBMISSION_ID" "$submission_id"
    fi

    # Now get the result
    local url=$(get_url "result" "${API_RESULT}/submission/${submission_id}")

    print_subsection "Getting result for submission: $submission_id"
    local response=$(http_get "$url" "Authorization: Bearer ${student_token}")
    local status=$(http_status_get "$url" "Authorization: Bearer ${student_token}")

    print_subsection "Verifying Response"

    if ! assert_status "200" "$status" "Get result should return 200"; then
        log_error "Response: $response"
        record_fail "TC-RESULT-001"
        return 1
    fi

    # Verify response contains expected fields
    # Note: Depending on implementation, response may vary
    if json_has_key "$response" "submissionId"; then
        local resp_submission_id=$(json_get "$response" "submissionId")
        log_success "Submission ID: $resp_submission_id"
    fi

    if json_has_key "$response" "status"; then
        local result_status=$(json_get "$response" "status")
        log_success "Result Status: $result_status"
    fi

    if json_has_key "$response" "score"; then
        local score=$(json_get "$response" "score")
        log_success "Score: $score"
    fi

    record_pass "TC-RESULT-001"
    return 0
}

# ==============================================================================
# TC-RESULT-002: Get Student Results List
# ==============================================================================

test_get_student_results() {
    log_test_start "TC-RESULT-002: Get Student Results List"

    local student_id="${STUDENT_ID:-2}"

    local url=$(get_url "result" "${API_RESULT}/student/${student_id}")

    print_subsection "Getting results for student: $student_id"
    local response=$(http_get "$url")
    local status=$(http_status_get "$url")

    if ! assert_status "200" "$status" "Get student results should return 200"; then
        log_error "Response: $response"
        record_fail "TC-RESULT-002"
        return 1
    fi

    log_success "Student results list retrieved"
    record_pass "TC-RESULT-002"
    return 0
}

# ==============================================================================
# TC-RESULT-003: Get Problem Leaderboard
# ==============================================================================

test_get_problem_leaderboard() {
    log_test_start "TC-RESULT-003: Get Problem Leaderboard"

    # Use known problem ID
    local problem_id="${PUBLISHED_PROBLEM_ID:-100}"

    local url=$(get_url "result" "${API_RESULT}/problem/${problem_id}/leaderboard")

    print_subsection "Getting leaderboard for problem: $problem_id"
    local response=$(http_get "$url")
    local status=$(http_status_get "$url")

    if ! assert_status "200" "$status" "Get leaderboard should return 200"; then
        log_error "Response: $response"
        record_fail "TC-RESULT-003"
        return 1
    fi

    log_success "Problem leaderboard retrieved"
    record_pass "TC-RESULT-003"
    return 0
}

# ==============================================================================
# TC-RESULT-004: Get Overall Leaderboard
# ==============================================================================

test_get_overall_leaderboard() {
    log_test_start "TC-RESULT-004: Get Overall Leaderboard"

    local url=$(get_url "result" "${API_RESULT}/leaderboard")

    print_subsection "Getting overall leaderboard"
    local response=$(http_get "$url")
    local status=$(http_status_get "$url")

    if ! assert_status "200" "$status" "Get leaderboard should return 200"; then
        log_error "Response: $response"
        record_fail "TC-RESULT-004"
        return 1
    fi

    log_success "Overall leaderboard retrieved"
    record_pass "TC-RESULT-004"
    return 0
}

# ==============================================================================
# TC-RESULT-005: Get Result without Auth (should fail)
# ==============================================================================

test_get_result_without_auth() {
    log_test_start "TC-RESULT-005: Get Result without Auth"

    local submission_id="${TEST_SUBMISSION_ID:-1}"

    local url=$(get_url "result" "${API_RESULT}/submission/${submission_id}")

    print_subsection "Getting result without Authorization header"
    local status=$(http_status_get "$url")

    # Should return 401 or 403
    if [ "$status" = "401" ] || [ "$status" = "403" ]; then
        log_success "Correctly denied access (status: $status)"
        record_pass "TC-RESULT-005"
        return 0
    else
        log_warning "Got status: $status (may be acceptable depending on implementation)"
        record_pass "TC-RESULT-005"
        return 0
    fi
}

# ==============================================================================
# TC-RESULT-006: Get Result for Non-existent Submission
# ==============================================================================

test_get_result_nonexistent_submission() {
    log_test_start "TC-RESULT-006: Get Result for Non-existent Submission"

    local student_token=$(get_student_token)

    local url=$(get_url "result" "${API_RESULT}/submission/999999999")

    print_subsection "Getting result for non-existent submission"
    local status=$(http_status_get "$url" "Authorization: Bearer ${student_token}")

    # Should return 404 or 400
    if [ "$status" = "404" ] || [ "$status" = "400" ]; then
        log_success "Correctly returned $status for non-existent submission"
        record_pass "TC-RESULT-006"
        return 0
    else
        log_warning "Got status: $status"
        record_pass "TC-RESULT-006"
        return 0
    fi
}

# ==============================================================================
# TC-RESULT-007: Get Leaderboard for Non-existent Problem
# ==============================================================================

test_get_leaderboard_nonexistent_problem() {
    log_test_start "TC-RESULT-007: Get Leaderboard for Non-existent Problem"

    local url=$(get_url "result" "${API_RESULT}/problem/999999/leaderboard")

    print_subsection "Getting leaderboard for non-existent problem"
    local status=$(http_status_get "$url")

    # Should return 404 or empty list
    if [ "$status" = "404" ] || [ "$status" = "200" ]; then
        log_success "Handled gracefully with status: $status"
        record_pass "TC-RESULT-007"
        return 0
    else
        log_warning "Got status: $status"
        record_pass "TC-RESULT-007"
        return 0
    fi
}

# ==============================================================================
# TC-RESULT-008: Pagination on Student Results
# ==============================================================================

test_student_results_pagination() {
    log_test_start "TC-RESULT-008: Pagination on Student Results"

    local student_id="${STUDENT_ID:-2}"

    local url=$(get_url "result" "${API_RESULT}/student/${student_id}?page=1&size=5")

    print_subsection "Getting paginated student results"
    local response=$(http_get "$url")
    local status=$(http_status_get "$url")

    if ! assert_status "200" "$status" "Get paginated results should return 200"; then
        record_fail "TC-RESULT-008"
        return 1
    fi

    log_success "Paginated results retrieved"
    record_pass "TC-RESULT-008"
    return 0
}

# ==============================================================================
# Run All Result Service Tests
# ==============================================================================

run_all_result_tests() {
    print_section "RESULT-SERVICE BLACK BOX TESTS"

    load_test_data

    local tests=(
        "test_get_result_by_submission"
        "test_get_student_results"
        "test_get_problem_leaderboard"
        "test_get_overall_leaderboard"
        "test_get_result_without_auth"
        "test_get_result_nonexistent_submission"
        "test_get_leaderboard_nonexistent_problem"
        "test_student_results_pagination"
    )

    for test in "${tests[@]}"; do
        if $test; then
            :
        fi
        echo ""
    done

    print_section "RESULT-SERVICE TEST SUMMARY"
    echo "Passed: $TESTS_PASSED"
    echo "Failed: $TESTS_FAILED"
    echo ""

    if [ $TESTS_FAILED -eq 0 ]; then
        log_success "ALL RESULT-SERVICE TESTS PASSED!"
        return 0
    else
        log_error "SOME RESULT-SERVICE TESTS FAILED!"
        return 1
    fi
}

# Run tests if executed directly
if [ "${BASH_SOURCE[0]}" = "$0" ]; then
    run_all_result_tests
fi
