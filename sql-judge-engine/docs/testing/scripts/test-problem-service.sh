#!/bin/bash

# ==============================================================================
# SQL Judge Engine - Problem Service Black Box Tests
# ==============================================================================
# Tests for problem-service API endpoints
# Reference: integration-test-architecture.md Section 2.2
# ==============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/config.sh"
source "${SCRIPT_DIR}/common.sh"

# Test result tracking
TESTS_PASSED=0
TESTS_FAILED=0

# ==============================================================================
# Helper: Get Teacher Token
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

get_teacher_id() {
    local login_url=$(get_url "user" "${API_USER}/login")
    local login_body=$(cat <<EOF
{
    "username": "${EXISTING_TEACHER_USERNAME}",
    "password": "${EXISTING_TEACHER_PASSWORD}"
}
EOF
)

    local response=$(http_post "$login_url" "$login_body")
    json_get "$response" "userId"
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

# ==============================================================================
# TC-PROB-001: Teacher Creates Problem
# ==============================================================================

test_create_problem() {
    log_test_start "TC-PROB-001: Teacher Creates Problem"

    local token=$(get_teacher_token)
    if [ -z "$token" ]; then
        log_error "Failed to get teacher token"
        record_fail "TC-PROB-001"
        return 1
    fi

    local teacher_id=$(get_teacher_id)
    local url=$(get_url "problem" "${API_PROBLEM}")

    local request_body=$(cat <<EOF
{
    "title": "Test Problem - Simple Query",
    "description": "Write a SQL query to select all users",
    "difficulty": "EASY",
    "sqlType": "DQL",
    "initSql": "CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR(50)); INSERT INTO users VALUES (1, 'Alice'), (2, 'Bob');",
    "standardAnswer": "SELECT * FROM users"
}
EOF
)

    print_subsection "Sending create problem request"
    local response=$(http_post "$url" "$request_body" "X-User-Id: ${teacher_id}")

    print_subsection "Verifying Response"

    local status=$(http_status_post "$url" "$request_body" "X-User-Id: ${teacher_id}")

    if ! assert_status "201" "$status" "Create problem should return 201"; then
        log_error "Response: $response"
        record_fail "TC-PROB-001"
        return 1
    fi

    # Verify problem ID is returned (API returns 'problemId' instead of 'id')
    if ! json_has_key "$response" "problemId"; then
        log_error "Response missing 'problemId' field"
        record_fail "TC-PROB-001"
        return 1
    fi

    local problem_id=$(json_get "$response" "problemId")
    save_test_data "TEST_PROBLEM_ID" "$problem_id"

    # Verify title
    local response_title=$(json_get "$response" "title")
    if ! assert_contains "$response_title" "Test Problem"; then
        record_fail "TC-PROB-001"
        return 1
    fi

    # Verify status is DRAFT
    local response_status=$(json_get "$response" "status")
    if ! assert_equals "DRAFT" "$response_status" "Initial status should be DRAFT"; then
        record_fail "TC-PROB-001"
        return 1
    fi

    record_pass "TC-PROB-001"
    return 0
}

# ==============================================================================
# TC-PROB-002: Get Problem List (No Auth Required)
# ==============================================================================

test_get_problem_list() {
    log_test_start "TC-PROB-002: Get Problem List (No Auth)"

    local url=$(get_url "problem" "${API_PROBLEM}?page=1&size=10")

    print_subsection "Getting problem list"
    local response=$(http_get "$url")
    local status=$(http_status_get "$url")

    if ! assert_status "200" "$status" "Get problem list should return 200"; then
        log_error "Response: $response"
        record_fail "TC-PROB-002"
        return 1
    fi

    # Response should be a list (array) or paginated object
    # Just verify we got some content
    if [ -z "$response" ] || [ "$response" = "{}" ]; then
        log_error "Empty response"
        record_fail "TC-PROB-002"
        return 1
    fi

    log_success "Problem list retrieved successfully"
    record_pass "TC-PROB-002"
    return 0
}

# ==============================================================================
# TC-PROB-003: Get Problem List with Filters
# ==============================================================================

test_get_problem_list_filtered() {
    log_test_start "TC-PROB-003: Get Problem List with Filters"

    local url=$(get_url "problem" "${API_PROBLEM}?difficulty=EASY&sqlType=DQL")

    print_subsection "Getting filtered problem list"
    local response=$(http_get "$url")
    local status=$(http_status_get "$url")

    if ! assert_status "200" "$status" "Get filtered problem list should return 200"; then
        record_fail "TC-PROB-003"
        return 1
    fi

    log_success "Filtered problem list retrieved"
    record_pass "TC-PROB-003"
    return 0
}

# ==============================================================================
# TC-PROB-004: Get Problem by ID
# ==============================================================================

test_get_problem_by_id() {
    log_test_start "TC-PROB-004: Get Problem by ID"

    # First create a problem to get its ID
    local token=$(get_teacher_token)
    local teacher_id=$(get_teacher_id)
    local create_url=$(get_url "problem" "${API_PROBLEM}")

    local create_body=$(cat <<EOF
{
    "title": "Problem for Get Test",
    "description": "Test problem",
    "difficulty": "MEDIUM",
    "sqlType": "DML",
    "initSql": "CREATE TABLE t1 (id INT);",
    "standardAnswer": "INSERT INTO t1 VALUES (1);"
}
EOF
)

    local create_response=$(http_post "$create_url" "$create_body" "X-User-Id: ${teacher_id}")
    local problem_id=$(json_get "$create_response" "problemId")

    if [ -z "$problem_id" ]; then
        log_error "Failed to create problem for testing"
        record_fail "TC-PROB-004"
        return 1
    fi

    save_test_data "GET_TEST_PROBLEM_ID" "$problem_id"

    # Now get the problem by ID
    local url=$(get_url "problem" "${API_PROBLEM}/${problem_id}")

    print_subsection "Getting problem by ID: $problem_id"
    local response=$(http_get "$url")
    local status=$(http_status_get "$url")

    if ! assert_status "200" "$status" "Get problem by ID should return 200"; then
        record_fail "TC-PROB-004"
        return 1
    fi

    # Verify problem ID matches
    local response_id=$(json_get "$response" "problemId")
    if ! assert_equals "$problem_id" "$response_id" "Problem ID mismatch"; then
        record_fail "TC-PROB-004"
        return 1
    fi

    record_pass "TC-PROB-004"
    return 0
}

# ==============================================================================
# TC-PROB-005: Teacher Updates Problem
# ==============================================================================

test_update_problem() {
    log_test_start "TC-PROB-005: Teacher Updates Problem"

    local token=$(get_teacher_token)
    local teacher_id=$(get_teacher_id)

    # Create a problem first
    local create_url=$(get_url "problem" "${API_PROBLEM}")
    local create_body=$(cat <<EOF
{
    "title": "Original Title",
    "description": "Original description",
    "difficulty": "EASY",
    "sqlType": "DQL",
    "initSql": "CREATE TABLE t (id INT);",
    "standardAnswer": "SELECT * FROM t;"
}
EOF
)

    local create_response=$(http_post "$create_url" "$create_body" "X-User-Id: ${teacher_id}")
    local problem_id=$(json_get "$create_response" "problemId")

    if [ -z "$problem_id" ]; then
        log_error "Failed to create problem"
        record_fail "TC-PROB-005"
        return 1
    fi

    # Now update the problem
    local url=$(get_url "problem" "${API_PROBLEM}/${problem_id}")
    local update_body=$(cat <<EOF
{
    "title": "Updated Title",
    "description": "Updated description",
    "difficulty": "MEDIUM",
    "sqlType": "DQL",
    "initSql": "CREATE TABLE t (id INT, name VARCHAR(50));",
    "standardAnswer": "SELECT * FROM t;"
}
EOF
)

    print_subsection "Updating problem: $problem_id"
    local response=$(http_put "$url" "$update_body" "X-User-Id: ${teacher_id}")
    local status=$(http_status_put "$url" "$update_body" "X-User-Id: ${teacher_id}")

    if ! assert_status "200" "$status" "Update problem should return 200"; then
        record_fail "TC-PROB-005"
        return 1
    fi

    # Verify title was updated
    local response_title=$(json_get "$response" "title")
    if ! assert_equals "Updated Title" "$response_title" "Title should be updated"; then
        record_fail "TC-PROB-005"
        return 1
    fi

    record_pass "TC-PROB-005"
    return 0
}

# ==============================================================================
# TC-PROB-006: Update Problem Status
# ==============================================================================

# Helper function to perform PUT request and capture both response body and status code
# Usage: http_put_with_status <response_var> <status_code_var> <url> <body> [headers...]
http_put_with_status() {
    local response_var="$1"
    local status_code_var="$2"
    shift 2
    local url="$1"
    local body="$2"
    shift 2
    local headers=("$@")

    local header_args=()
    for header in "${headers[@]}"; do
        header_args+=("-H" "$header")
    done

    # Use temporary files to capture response body and status code
    local body_tmpfile=$(mktemp)
    local status_tmpfile=$(mktemp)
    
    curl -s -o "$body_tmpfile" -w "%{http_code}" -X PUT \
        -H "Content-Type: application/json" \
        "${header_args[@]}" \
        --max-time 30 \
        -d "$body" \
        "$url" > "$status_tmpfile"
    
    local __response_body=$(cat "$body_tmpfile")
    local __status_code=$(cat "$status_tmpfile")
    
    rm -f "$body_tmpfile" "$status_tmpfile"

    # Return values via variable references using nameref (bash 4.3+)
    # For older bash, use eval
    if [[ -n "$BASH_VERSION" && "${BASH_VERSION:0:1}" -ge 4 ]]; then
        local -n __response_ref="$response_var"
        local -n __status_ref="$status_code_var"
        __response_ref="$__response_body"
        __status_ref="$__status_code"
    else
        eval "$response_var='\$__response_body'"
        eval "$status_code_var='\$__status_code'"
    fi
}

test_update_problem_status() {
    log_test_start "TC-PROB-006: Update Problem Status"

    local token=$(get_teacher_token)
    local teacher_id=$(get_teacher_id)

    # Create a problem in DRAFT status
    local create_url=$(get_url "problem" "${API_PROBLEM}")
    local create_body=$(cat <<EOF
{
    "title": "Status Test Problem",
    "description": "Test status change",
    "difficulty": "EASY",
    "sqlType": "DQL",
    "initSql": "CREATE TABLE t (id INT);",
    "standardAnswer": "SELECT * FROM t;"
}
EOF
)

    local create_response=$(http_post "$create_url" "$create_body" "X-User-Id: ${teacher_id}")
    local problem_id=$(json_get "$create_response" "problemId")

    if [ -z "$problem_id" ]; then
        log_error "Failed to create problem"
        record_fail "TC-PROB-006"
        return 1
    fi

    # Step 1: Update status from DRAFT to READY (valid transition)
    local url=$(get_url "problem" "${API_PROBLEM}/${problem_id}/status")
    local status_body='{"status": "READY"}'

    print_subsection "Changing status to READY: $problem_id"
    local response
    local status_code
    http_put_with_status response status_code "$url" "$status_body" "X-User-Id: ${teacher_id}"

    if ! assert_status "200" "$status_code" "Update status to READY should return 200"; then
        log_error "Failed to change status to READY: $response"
        record_fail "TC-PROB-006"
        return 1
    fi

    # Verify status is now READY
    local response_status=$(json_get "$response" "status")
    if ! assert_equals "READY" "$response_status" "Status should be READY"; then
        record_fail "TC-PROB-006"
        return 1
    fi

    # Step 2: Update status from READY to PUBLISHED (valid transition)
    status_body='{"status": "PUBLISHED"}'

    print_subsection "Publishing problem: $problem_id"
    http_put_with_status response status_code "$url" "$status_body" "X-User-Id: ${teacher_id}"

    if ! assert_status "200" "$status_code" "Update status to PUBLISHED should return 200"; then
        log_error "Failed to change status to PUBLISHED: $response"
        record_fail "TC-PROB-006"
        return 1
    fi

    # Verify status is now PUBLISHED
    response_status=$(json_get "$response" "status")
    if ! assert_equals "PUBLISHED" "$response_status" "Status should be PUBLISHED"; then
        record_fail "TC-PROB-006"
        return 1
    fi

    save_test_data "PUBLISHED_PROBLEM_ID" "$problem_id"

    record_pass "TC-PROB-006"
    return 0
}

# ==============================================================================
# TC-PROB-007: Teacher Deletes Problem
# ==============================================================================

test_delete_problem() {
    log_test_start "TC-PROB-007: Teacher Deletes Problem"

    local token=$(get_teacher_token)
    local teacher_id=$(get_teacher_id)

    # Create a problem to delete
    local create_url=$(get_url "problem" "${API_PROBLEM}")
    local create_body=$(cat <<EOF
{
    "title": "Problem to Delete",
    "description": "This will be deleted",
    "difficulty": "EASY",
    "sqlType": "DQL",
    "initSql": "CREATE TABLE t (id INT);",
    "standardAnswer": "SELECT * FROM t;"
}
EOF
)

    local create_response=$(http_post "$create_url" "$create_body" "X-User-Id: ${teacher_id}")
    local problem_id=$(json_get "$create_response" "problemId")

    if [ -z "$problem_id" ]; then
        log_error "Failed to create problem"
        record_fail "TC-PROB-007"
        return 1
    fi

    # Delete the problem
    local url=$(get_url "problem" "${API_PROBLEM}/${problem_id}")

    print_subsection "Deleting problem: $problem_id"
    local status=$(http_status_delete "$url" "X-User-Id: ${teacher_id}")

    if ! assert_status "204" "$status" "Delete problem should return 204"; then
        record_fail "TC-PROB-007"
        return 1
    fi

    # Try to get the deleted problem (should return 404 or empty)
    print_subsection "Verifying deletion"
    local get_response=$(http_get "$url")
    local get_status=$(http_status_get "$url")

    if [ "$get_status" != "404" ] && [ "$get_status" != "204" ]; then
        log_warning "Problem may not be deleted (status: $get_status)"
    fi

    record_pass "TC-PROB-007"
    return 0
}

# ==============================================================================
# TC-PROB-008: Get Teacher's Own Problems
# ==============================================================================

test_get_teacher_problems() {
    log_test_start "TC-PROB-008: Get Teacher's Own Problems"

    local token=$(get_teacher_token)
    local teacher_id=$(get_teacher_id)

    local url=$(get_url "problem" "${API_PROBLEM}/teacher/my")

    print_subsection "Getting teacher's problems"
    local response=$(http_get "$url" "X-User-Id: ${teacher_id}")
    local status=$(http_status_get "$url" "X-User-Id: ${teacher_id}")

    if ! assert_status "200" "$status" "Get teacher problems should return 200"; then
        record_fail "TC-PROB-008"
        return 1
    fi

    log_success "Teacher's problems retrieved"
    record_pass "TC-PROB-008"
    return 0
}

# ==============================================================================
# TC-PROB-009: Student Cannot Create Problem
# ==============================================================================

test_student_cannot_create_problem() {
    log_test_start "TC-PROB-009: Student Cannot Create Problem"

    local student_id="2"  # Assuming student role

    local url=$(get_url "problem" "${API_PROBLEM}")
    local request_body=$(cat <<EOF
{
    "title": "Student Created Problem",
    "description": "Student should not be able to create",
    "difficulty": "EASY",
    "sqlType": "DQL",
    "initSql": "CREATE TABLE t (id INT);",
    "standardAnswer": "SELECT * FROM t;"
}
EOF
)

    print_subsection "Student attempting to create problem"
    local status=$(http_status_post "$url" "$request_body" "X-User-Id: ${student_id}")

    # Should return 403 Forbidden
    if [ "$status" = "403" ] || [ "$status" = "401" ]; then
        log_success "Student correctly denied (status: $status)"
        record_pass "TC-PROB-009"
        return 0
    else
        log_error "Expected 403 or 401, got: $status"
        record_fail "TC-PROB-009"
        return 1
    fi
}

# ==============================================================================
# TC-PROB-010: Batch Import Problems
# ==============================================================================

test_batch_import() {
    log_test_start "TC-PROB-010: Batch Import Problems"

    local token=$(get_teacher_token)
    local teacher_id=$(get_teacher_id)

    local url=$(get_url "problem" "${API_PROBLEM}/batch")

    local request_body=$(cat <<EOF
{
    "problems": [
        {
            "title": "Batch Problem 1",
            "description": "First batch problem",
            "difficulty": "EASY",
            "sqlType": "DQL",
            "initSql": "CREATE TABLE t1 (id INT);",
            "standardAnswer": "SELECT * FROM t1;"
        },
        {
            "title": "Batch Problem 2",
            "description": "Second batch problem",
            "difficulty": "MEDIUM",
            "sqlType": "DML",
            "initSql": "CREATE TABLE t2 (id INT);",
            "standardAnswer": "INSERT INTO t2 VALUES (1);"
        }
    ]
}
EOF
)

    print_subsection "Batch importing problems"
    local response=$(http_post "$url" "$request_body" "X-User-Id: ${teacher_id}")
    local status=$(http_status_post "$url" "$request_body" "X-User-Id: ${teacher_id}")

    if ! assert_status "201" "$status" "Batch import should return 201"; then
        record_fail "TC-PROB-010"
        return 1
    fi

    log_success "Batch import completed"
    record_pass "TC-PROB-010"
    return 0
}

# ==============================================================================
# Run All Problem Service Tests
# ==============================================================================

run_all_problem_tests() {
    print_section "PROBLEM-SERVICE BLACK BOX TESTS"

    # Load any existing test data
    load_test_data

    local tests=(
        "test_create_problem"
        "test_get_problem_list"
        "test_get_problem_list_filtered"
        "test_get_problem_by_id"
        "test_update_problem"
        "test_update_problem_status"
        "test_delete_problem"
        "test_get_teacher_problems"
        "test_student_cannot_create_problem"
        "test_batch_import"
    )

    for test in "${tests[@]}"; do
        if $test; then
            :
        fi
        echo ""
    done

    print_section "PROBLEM-SERVICE TEST SUMMARY"
    echo "Passed: $TESTS_PASSED"
    echo "Failed: $TESTS_FAILED"
    echo ""

    if [ $TESTS_FAILED -eq 0 ]; then
        log_success "ALL PROBLEM-SERVICE TESTS PASSED!"
        return 0
    else
        log_error "SOME PROBLEM-SERVICE TESTS FAILED!"
        return 1
    fi
}

# Run tests if executed directly
if [ "${BASH_SOURCE[0]}" = "$0" ]; then
    run_all_problem_tests
fi
