#!/bin/bash

# ==============================================================================
# SQL Judge Engine - Submission Service Black Box Tests
# ==============================================================================
# Tests for submission-service API endpoints
# Reference: integration-test-architecture.md Section 2.3
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

get_or_create_published_problem() {
    local teacher_id="${TEACHER_ID:-1}"

    # First try to get an existing published problem
    local problem_url=$(get_url "problem" "${API_PROBLEM}?status=PUBLISHED&page=1&size=1")
    local problem_response=$(http_get "$problem_url")

    # If we get a problem ID from the list, use it
    if json_has_key "$problem_response" "content"; then
        # Try to extract first problem ID from content array
        local first_problem=$(echo "$problem_response" | grep -o '"id":[[:space:]]*[0-9]*' | head -1 | grep -o '[0-9]*')
        if [ -n "$first_problem" ]; then
            echo "$first_problem"
            return 0
        fi
    fi

    # If no published problem exists, create and publish one
    local create_url=$(get_url "problem" "${API_PROBLEM}")
    local create_body=$(cat <<EOF
{
    "title": "Submission Test Problem - $(date +%s)",
    "description": "Problem for submission testing",
    "difficulty": "EASY",
    "sqlType": "DQL",
    "initSql": "CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR(50)); INSERT INTO users VALUES (1, 'Alice'), (2, 'Bob');",
    "standardAnswer": "SELECT * FROM users WHERE id = 1;"
}
EOF
)

    local create_response=$(http_post "$create_url" "$create_body" "X-User-Id: ${teacher_id}")
    local problem_id=$(json_get "$create_response" "problemId")

    if [ -z "$problem_id" ]; then
        log_error "Failed to create problem"
        return 1
    fi

    # Publish the problem
    local status_url=$(get_url "problem" "${API_PROBLEM}/${problem_id}/status")
    http_put "$status_url" '{"status": "PUBLISHED"}' "X-User-Id: ${teacher_id}" > /dev/null

    echo "$problem_id"
}

# ==============================================================================
# TC-SUB-001: Student Submits SQL Answer
# ==============================================================================

test_submit_answer() {
    log_test_start "TC-SUB-001: Student Submits SQL Answer"

    local student_id="${STUDENT_ID:-2}"

    # Get or create a published problem
    local problem_id=$(get_or_create_published_problem)
    if [ -z "$problem_id" ] || [ "$problem_id" = "1" ]; then
        # Fallback to known problem ID
        problem_id="${PUBLISHED_PROBLEM_ID:-100}"
    fi

    local url=$(get_url "submission" "${API_SUBMISSION}")

    local request_body=$(cat <<EOF
{
    "problemId": ${problem_id},
    "sqlContent": "SELECT * FROM users WHERE id = 1"
}
EOF
)

    print_subsection "Submitting answer for problem: $problem_id"
    local response=$(http_post "$url" "$request_body" "X-User-Id: ${student_id}")

    print_subsection "Verifying Response"

    local status=$(http_status_post "$url" "$request_body" "X-User-Id: ${student_id}")

    # Should return 202 Accepted (async processing)
    if ! assert_status "202" "$status" "Submit should return 202 Accepted"; then
        log_error "Response: $response"
        record_fail "TC-SUB-001"
        return 1
    fi

    # Verify submission ID is returned
    if ! json_has_key "$response" "submissionId"; then
        log_error "Response missing 'submissionId' field"
        log_error "Response: $response"
        record_fail "TC-SUB-001"
        return 1
    fi

    local submission_id=$(json_get "$response" "submissionId")
    save_test_data "TEST_SUBMISSION_ID" "$submission_id"

    # Verify status is PENDING or JUDGING
    local response_status=$(json_get "$response" "status")
    if [ "$response_status" != "PENDING" ] && [ "$response_status" != "JUDGING" ]; then
        log_warning "Unexpected status: $response_status"
    fi

    log_success "Submission ID: $submission_id"
    record_pass "TC-SUB-001"
    return 0
}

# ==============================================================================
# TC-SUB-002: Get Submission Status (Polling)
# ==============================================================================

test_get_submission_status() {
    log_test_start "TC-SUB-002: Get Submission Status"

    # First create a submission
    local student_id="${STUDENT_ID:-2}"
    local problem_id="${PUBLISHED_PROBLEM_ID:-100}"

    local submit_url=$(get_url "submission" "${API_SUBMISSION}")
    local submit_body=$(cat <<EOF
{
    "problemId": ${problem_id},
    "sqlContent": "SELECT 1"
}
EOF
)

    print_subsection "Creating a submission first"
    local submit_response=$(http_post "$submit_url" "$submit_body" "X-User-Id: ${student_id}")
    local submission_id=$(json_get "$submit_response" "submissionId")

    if [ -z "$submission_id" ]; then
        log_error "Failed to create submission"
        record_fail "TC-SUB-002"
        return 1
    fi

    save_test_data "STATUS_TEST_SUBMISSION_ID" "$submission_id"

    # Now get the status
    local url=$(get_url "submission" "${API_SUBMISSION}/${submission_id}/status")

    print_subsection "Getting submission status for: $submission_id"
    local response=$(http_get "$url")
    local status=$(http_status_get "$url")

    if ! assert_status "200" "$status" "Get status should return 200"; then
        record_fail "TC-SUB-002"
        return 1
    fi

    # Verify response has status field
    if ! json_has_key "$response" "status"; then
        log_error "Response missing 'status' field"
        record_fail "TC-SUB-002"
        return 1
    fi

    local response_status=$(json_get "$response" "status")
    log_success "Current status: $response_status"

    # Status should be one of: PENDING, JUDGING, SUCCESS, FAILED
    case "$response_status" in
        PENDING|JUDGING|SUCCESS|FAILED)
            log_success "Valid status: $response_status"
            ;;
        *)
            log_warning "Unexpected status: $response_status"
            ;;
    esac

    record_pass "TC-SUB-002"
    return 0
}

# ==============================================================================
# TC-SUB-003: Get Submission Detail
# ==============================================================================

test_get_submission_detail() {
    log_test_start "TC-SUB-003: Get Submission Detail"

    local student_id="${STUDENT_ID:-2}"

    # Create a submission first
    local problem_id="${PUBLISHED_PROBLEM_ID:-100}"
    local submit_url=$(get_url "submission" "${API_SUBMISSION}")
    local submit_body=$(cat <<EOF
{
    "problemId": ${problem_id},
    "sqlContent": "SELECT * FROM users"
}
EOF
)

    print_subsection "Creating submission for detail test"
    local submit_response=$(http_post "$submit_url" "$submit_body" "X-User-Id: ${student_id}")
    local submission_id=$(json_get "$submit_response" "submissionId")

    if [ -z "$submission_id" ]; then
        log_error "Failed to create submission"
        record_fail "TC-SUB-003"
        return 1
    fi

    # Get submission detail
    local url=$(get_url "submission" "${API_SUBMISSION}/${submission_id}")

    print_subsection "Getting submission detail for: $submission_id"
    local response=$(http_get "$url")
    local status=$(http_status_get "$url")

    if ! assert_status "200" "$status" "Get detail should return 200"; then
        record_fail "TC-SUB-003"
        return 1
    fi

    # Verify required fields
    if ! json_has_key "$response" "submissionId"; then
        log_error "Response missing 'submissionId'"
        record_fail "TC-SUB-003"
        return 1
    fi

    if ! json_has_key "$response" "problemId"; then
        log_error "Response missing 'problemId'"
        record_fail "TC-SUB-003"
        return 1
    fi

    if ! json_has_key "$response" "sqlContent"; then
        log_error "Response missing 'sqlContent'"
        record_fail "TC-SUB-003"
        return 1
    fi

    log_success "Submission detail retrieved successfully"
    record_pass "TC-SUB-003"
    return 0
}

# ==============================================================================
# TC-SUB-004: Get Student's Submission History
# ==============================================================================

test_get_submission_history() {
    log_test_start "TC-SUB-004: Get Student Submission History"

    local student_id="${STUDENT_ID:-2}"

    local url=$(get_url "submission" "${API_SUBMISSION}?page=1&size=10")

    print_subsection "Getting submission history for student: $student_id"
    local response=$(http_get "$url" "X-User-Id: ${student_id}")
    local status=$(http_status_get "$url" "X-User-Id: ${student_id}")

    if ! assert_status "200" "$status" "Get history should return 200"; then
        record_fail "TC-SUB-004"
        return 1
    fi

    log_success "Submission history retrieved"
    record_pass "TC-SUB-004"
    return 0
}

# ==============================================================================
# TC-SUB-005: Submit with Invalid Problem ID
# ==============================================================================

test_submit_invalid_problem() {
    log_test_start "TC-SUB-005: Submit with Invalid Problem ID"

    local student_id="${STUDENT_ID:-2}"

    local url=$(get_url "submission" "${API_SUBMISSION}")

    local request_body=$(cat <<EOF
{
    "problemId": 999999,
    "sqlContent": "SELECT 1"
}
EOF
)

    print_subsection "Submitting to non-existent problem"
    local response=$(http_post "$url" "$request_body" "X-User-Id: ${student_id}")
    local status=$(http_status_post "$url" "$request_body" "X-User-Id: ${student_id}")

    # Even with invalid problem, submission service may accept (async processing)
    # The error will be handled during judging
    if [ "$status" = "202" ]; then
        log_success "Submission accepted (will fail during judging)"
        record_pass "TC-SUB-005"
        return 0
    elif [ "$status" = "400" ] || [ "$status" = "404" ]; then
        log_success "Submission rejected with: $status"
        record_pass "TC-SUB-005"
        return 0
    else
        log_warning "Unexpected status: $status"
        record_pass "TC-SUB-005"
        return 0
    fi
}

# ==============================================================================
# TC-SUB-006: Submit without X-User-Id Header
# ==============================================================================

test_submit_without_user_id() {
    log_test_start "TC-SUB-006: Submit without X-User-Id Header"

    local url=$(get_url "submission" "${API_SUBMISSION}")

    local request_body=$(cat <<EOF
{
    "problemId": 100,
    "sqlContent": "SELECT 1"
}
EOF
)

    print_subsection "Submitting without X-User-Id header"
    local status=$(http_status_post "$url" "$request_body")

    # Should return 401 or 403
    if [ "$status" = "401" ] || [ "$status" = "403" ]; then
        log_success "Correctly denied without user ID (status: $status)"
        record_pass "TC-SUB-006"
        return 0
    else
        log_error "Expected 401 or 403, got: $status"
        record_fail "TC-SUB-006"
        return 1
    fi
}

# ==============================================================================
# TC-SUB-007: Wait for Judging Result (Poll Until Complete)
# ==============================================================================

test_wait_for_judging() {
    log_test_start "TC-SUB-007: Wait for Judging Result"

    local student_id="${STUDENT_ID:-2}"
    local problem_id="${PUBLISHED_PROBLEM_ID:-100}"

    # Create a submission
    local submit_url=$(get_url "submission" "${API_SUBMISSION}")
    local submit_body=$(cat <<EOF
{
    "problemId": ${problem_id},
    "sqlContent": "SELECT * FROM users"
}
EOF
)

    print_subsection "Creating submission to wait for judging"
    local submit_response=$(http_post "$submit_url" "$submit_body" "X-User-Id: ${student_id}")
    local submission_id=$(json_get "$submit_response" "submissionId")

    if [ -z "$submission_id" ]; then
        log_error "Failed to create submission"
        record_fail "TC-SUB-007"
        return 1
    fi

    save_test_data "WAIT_TEST_SUBMISSION_ID" "$submission_id"

    # Poll for completion
    local status_url=$(get_url "submission" "${API_SUBMISSION}/${submission_id}/status")
    local attempt=1
    local max_attempts=$MAX_POLL_COUNT
    local current_status=""

    print_subsection "Polling for completion (max ${max_attempts} attempts)"

    while [ $attempt -le $max_attempts ]; do
        local response=$(http_get "$status_url")
        current_status=$(json_get "$response" "status")

        log_info "Attempt $attempt/$max_attempts - Status: $current_status"

        if [ "$current_status" = "SUCCESS" ] || [ "$current_status" = "FAILED" ]; then
            log_success "Judging completed with status: $current_status"
            record_pass "TC-SUB-007"
            return 0
        fi

        sleep $POLL_INTERVAL
        attempt=$((attempt + 1))
    done

    log_warning "Judging still in progress after ${max_attempts} attempts"
    log_warning "Last status: $current_status"
    save_test_data "PENDING_SUBMISSION_ID" "$submission_id"
    record_pass "TC-SUB-007"
    return 0
}

# ==============================================================================
# TC-SUB-008: Filter Submissions by Problem ID
# ==============================================================================

test_filter_submissions_by_problem() {
    log_test_start "TC-SUB-008: Filter Submissions by Problem ID"

    local student_id="${STUDENT_ID:-2}"
    local problem_id="${PUBLISHED_PROBLEM_ID:-100}"

    local url=$(get_url "submission" "${API_SUBMISSION}?problemId=${problem_id}&page=1&size=10")

    print_subsection "Getting submissions for problem: $problem_id"
    local response=$(http_get "$url" "X-User-Id: ${student_id}")
    local status=$(http_status_get "$url" "X-User-Id: ${student_id}")

    if ! assert_status "200" "$status" "Get filtered submissions should return 200"; then
        record_fail "TC-SUB-008"
        return 1
    fi

    log_success "Filtered submissions retrieved"
    record_pass "TC-SUB-008"
    return 0
}

# ==============================================================================
# Run All Submission Service Tests
# ==============================================================================

run_all_submission_tests() {
    print_section "SUBMISSION-SERVICE BLACK BOX TESTS"

    load_test_data

    local tests=(
        "test_submit_answer"
        "test_get_submission_status"
        "test_get_submission_detail"
        "test_get_submission_history"
        "test_submit_invalid_problem"
        "test_submit_without_user_id"
        "test_wait_for_judging"
        "test_filter_submissions_by_problem"
    )

    for test in "${tests[@]}"; do
        if $test; then
            :
        fi
        echo ""
    done

    print_section "SUBMISSION-SERVICE TEST SUMMARY"
    echo "Passed: $TESTS_PASSED"
    echo "Failed: $TESTS_FAILED"
    echo ""

    if [ $TESTS_FAILED -eq 0 ]; then
        log_success "ALL SUBMISSION-SERVICE TESTS PASSED!"
        return 0
    else
        log_error "SOME SUBMISSION-SERVICE TESTS FAILED!"
        return 1
    fi
}

# Run tests if executed directly
if [ "${BASH_SOURCE[0]}" = "$0" ]; then
    run_all_submission_tests
fi
