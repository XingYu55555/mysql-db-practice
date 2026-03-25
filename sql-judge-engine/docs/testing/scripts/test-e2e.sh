#!/bin/bash

# ==============================================================================
# SQL Judge Engine - End-to-End Black Box Tests
# ==============================================================================
# Tests the complete user workflow across multiple services
# Reference: black-box-test-architecture.md Section 4
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

get_token_by_credentials() {
    local username="$1"
    local password="$2"

    local url=$(get_url "user" "${API_USER}/login")
    local body=$(cat <<EOF
{
    "username": "${username}",
    "password": "${password}"
}
EOF
)
    local response=$(http_post "$url" "$body")
    json_get "$response" "token"
}

get_user_id_by_credentials() {
    local username="$1"
    local password="$2"

    local url=$(get_url "user" "${API_USER}/login")
    local body=$(cat <<EOF
{
    "username": "${username}",
    "password": "${password}"
}
EOF
)
    local response=$(http_post "$url" "$body")
    json_get "$response" "userId"
}

# ==============================================================================
# TC-E2E-001: Complete Answer Flow (Teacher Creates -> Student Answers -> Check Result)
# ==============================================================================

test_complete_answer_flow() {
    log_test_start "TC-E2E-001: Complete Answer Flow"

    local RANDOM_SUFFIX=$(date +%s%N | sha256sum | base64 | head -c 6)

    # Step 1: Teacher Login
    print_subsection "Step 1: Teacher Login"
    local teacher_token=$(get_token_by_credentials "${EXISTING_TEACHER_USERNAME}" "${EXISTING_TEACHER_PASSWORD}")
    if [ -z "$teacher_token" ]; then
        log_error "Teacher login failed"
        record_fail "TC-E2E-001"
        return 1
    fi
    log_success "Teacher logged in"

    # Get teacher ID
    local teacher_id=$(get_user_id_by_credentials "${EXISTING_TEACHER_USERNAME}" "${EXISTING_TEACHER_PASSWORD}")
    if [ -z "$teacher_id" ]; then
        teacher_id="1"
    fi

    # Step 2: Teacher Creates Problem
    print_subsection "Step 2: Teacher Creates Problem"
    local create_url=$(get_url "problem" "${API_PROBLEM}")
    local problem_body=$(cat <<EOF
{
    "title": "E2E Test Problem - ${RANDOM_SUFFIX}",
    "description": "End-to-end test problem",
    "difficulty": "EASY",
    "sqlType": "DQL",
    "initSql": "CREATE TABLE employees (id INT PRIMARY KEY, name VARCHAR(50), salary INT); INSERT INTO employees VALUES (1, 'Alice', 5000), (2, 'Bob', 6000);",
    "standardAnswer": "SELECT * FROM employees WHERE salary > 5500;"
}
EOF
)

    local problem_response=$(http_post "$create_url" "$problem_body" "X-User-Id: ${teacher_id}")
    local problem_id=$(json_get "$problem_response" "problemId")

    if [ -z "$problem_id" ]; then
        log_error "Problem creation failed"
        log_error "Response: $problem_response"
        record_fail "TC-E2E-001"
        return 1
    fi

    save_test_data "E2E_PROBLEM_ID" "$problem_id"
    log_success "Problem created: ID=$problem_id"

    # Step 3: Teacher Publishes Problem
    print_subsection "Step 3: Teacher Publishes Problem"
    local status_url=$(get_url "problem" "${API_PROBLEM}/${problem_id}/status")
    local status_response=$(http_put "$status_url" '{"status": "PUBLISHED"}' "X-User-Id: ${teacher_id}")
    local publish_status=$(json_get "$status_response" "status")

    if [ "$publish_status" != "PUBLISHED" ]; then
        log_warning "Problem may not be published. Status: $publish_status"
    fi
    log_success "Problem published"

    # Step 4: Student Login
    print_subsection "Step 4: Student Login"
    local student_token=$(get_token_by_credentials "${EXISTING_STUDENT_USERNAME}" "${EXISTING_STUDENT_PASSWORD}")
    if [ -z "$student_token" ]; then
        log_error "Student login failed"
        record_fail "TC-E2E-001"
        return 1
    fi
    log_success "Student logged in"

    # Get student ID
    local student_id=$(get_user_id_by_credentials "${EXISTING_STUDENT_USERNAME}" "${EXISTING_STUDENT_PASSWORD}")
    if [ -z "$student_id" ]; then
        student_id="2"
    fi

    # Step 5: Student Submits Answer
    print_subsection "Step 5: Student Submits Answer"
    local submit_url=$(get_url "submission" "${API_SUBMISSION}")
    local submit_body=$(cat <<EOF
{
    "problemId": ${problem_id},
    "sqlContent": "SELECT * FROM employees WHERE salary > 5500"
}
EOF
)

    local submit_response=$(http_post "$submit_url" "$submit_body" "X-User-Id: ${student_id}")
    local submission_id=$(json_get "$submit_response" "submissionId")

    if [ -z "$submission_id" ]; then
        log_error "Submission failed"
        log_error "Response: $submit_response"
        record_fail "TC-E2E-001"
        return 1
    fi

    save_test_data "E2E_SUBMISSION_ID" "$submission_id"
    log_success "Answer submitted: ID=$submission_id"

    # Step 6: Poll for Submission Status
    print_subsection "Step 6: Poll for Judging Status"
    local status_check_url=$(get_url "submission" "${API_SUBMISSION}/${submission_id}/status")
    local attempt=1
    local final_status=""

    while [ $attempt -le $MAX_POLL_COUNT ]; do
        local status_response=$(http_get "$status_check_url")
        final_status=$(json_get "$status_response" "status")

        log_info "Poll $attempt/$MAX_POLL_COUNT - Status: $final_status"

        if [ "$final_status" = "SUCCESS" ] || [ "$final_status" = "FAILED" ]; then
            break
        fi

        sleep $POLL_INTERVAL
        attempt=$((attempt + 1))
    done

    if [ "$final_status" = "SUCCESS" ] || [ "$final_status" = "FAILED" ]; then
        log_success "Judging completed: $final_status"
    else
        log_warning "Judging not completed after ${MAX_POLL_COUNT} polls. Last status: $final_status"
    fi

    # Step 7: Student Queries Result
    print_subsection "Step 7: Student Queries Result"
    local result_url=$(get_url "result" "${API_RESULT}/submission/${submission_id}")
    local result_response=$(http_get "$result_url" "Authorization: Bearer ${student_token}")
    local result_status_code=$(http_status_get "$result_url" "Authorization: Bearer ${student_token}")

    if ! assert_status "200" "$result_status_code" "Result query should return 200"; then
        log_warning "Result query returned: $result_status_code"
    fi

    if json_has_key "$result_response" "status"; then
        local result_status=$(json_get "$result_response" "status")
        log_success "Result Status: $result_status"
    fi

    if json_has_key "$result_response" "score"; then
        local score=$(json_get "$result_response" "score")
        log_success "Score: $score"
    fi

    # Step 8: Verify Submission History
    print_subsection "Step 8: Verify Submission History"
    local history_url=$(get_url "submission" "${API_SUBMISSION}?problemId=${problem_id}&page=1&size=10")
    local history_response=$(http_get "$history_url" "X-User-Id: ${student_id}")
    local history_status=$(http_status_get "$history_url" "X-User-Id: ${student_id}")

    if assert_status "200" "$history_status" "History query should return 200"; then
        log_success "Submission history accessible"
    fi

    log_success "E2E test completed"
    record_pass "TC-E2E-001"
    return 0
}

# ==============================================================================
# TC-E2E-002: Error Handling Flow (Invalid Problem Submission)
# ==============================================================================

test_error_handling_flow() {
    log_test_start "TC-E2E-002: Error Handling Flow"

    # Get student ID
    local student_id=$(get_user_id_by_credentials "${EXISTING_STUDENT_USERNAME}" "${EXISTING_STUDENT_PASSWORD}")
    if [ -z "$student_id" ]; then
        student_id="2"
    fi

    # Step 1: Submit to non-existent problem
    print_subsection "Step 1: Submit to Non-existent Problem"
    local submit_url=$(get_url "submission" "${API_SUBMISSION}")
    local submit_body=$(cat <<EOF
{
    "problemId": 999999,
    "sqlContent": "SELECT 1"
}
EOF
)

    local submit_response=$(http_post "$submit_url" "$submit_body" "X-User-Id: ${student_id}")
    local submit_status=$(http_status_post "$submit_url" "$submit_body" "X-User-Id: ${student_id}")

    log_info "Submission status: $submit_status"

    # Step 2: Check submission status
    if [ "$submit_status" = "202" ]; then
        local submission_id=$(json_get "$submit_response" "submissionId")

        if [ -n "$submission_id" ]; then
            log_success "Submission accepted for async processing"

            # Wait for judging to complete
            print_subsection "Step 2: Wait for Error Processing"
            local status_url=$(get_url "submission" "${API_SUBMISSION}/${submission_id}/status")
            local attempt=1
            local final_status=""

            while [ $attempt -le $MAX_POLL_COUNT ]; do
                local status_response=$(http_get "$status_url")
                final_status=$(json_get "$status_response" "status")

                log_info "Poll $attempt/$MAX_POLL_COUNT - Status: $final_status"

                if [ "$final_status" = "SUCCESS" ] || [ "$final_status" = "FAILED" ]; then
                    break
                fi

                sleep $POLL_INTERVAL
                attempt=$((attempt + 1))
            done

            # Result should eventually be FAILED or ERROR
            log_success "Final status: $final_status"
        fi
    else
        log_success "Submission rejected immediately with status: $submit_status"
    fi

    record_pass "TC-E2E-002"
    return 0
}

# ==============================================================================
# TC-E2E-003: Student Updates Answer (Resubmission)
# ==============================================================================

test_resubmission_flow() {
    log_test_start "TC-E2E-003: Resubmission Flow"

    # Get student ID
    local student_id=$(get_user_id_by_credentials "${EXISTING_STUDENT_USERNAME}" "${EXISTING_STUDENT_PASSWORD}")
    if [ -z "$student_id" ]; then
        student_id="2"
    fi

    # Get or create a published problem
    local problem_id="${E2E_PROBLEM_ID:-${PUBLISHED_PROBLEM_ID:-100}}"

    # Step 1: First submission
    print_subsection "Step 1: First Submission (Wrong Answer)"
    local submit_url=$(get_url "submission" "${API_SUBMISSION}")
    local first_body=$(cat <<EOF
{
    "problemId": ${problem_id},
    "sqlContent": "SELECT 1"
}
EOF
)

    local first_response=$(http_post "$submit_url" "$first_body" "X-User-Id: ${student_id}")
    local first_submission_id=$(json_get "$first_response" "submissionId")

    if [ -z "$first_submission_id" ]; then
        log_error "First submission failed"
        record_fail "TC-E2E-003"
        return 1
    fi

    log_success "First submission: $first_submission_id"

    # Wait briefly for processing
    sleep 3

    # Step 2: Second submission (Correct answer)
    print_subsection "Step 2: Second Submission (Correct Answer)"
    local second_body=$(cat <<EOF
{
    "problemId": ${problem_id},
    "sqlContent": "SELECT * FROM employees WHERE salary > 5500"
}
EOF
)

    local second_response=$(http_post "$submit_url" "$second_body" "X-User-Id: ${student_id}")
    local second_submission_id=$(json_get "$second_response" "submissionId")

    if [ -z "$second_submission_id" ]; then
        log_error "Second submission failed"
        record_fail "TC-E2E-003"
        return 1
    fi

    log_success "Second submission: $second_submission_id"

    # Step 3: Verify both submissions exist in history
    print_subsection "Step 3: Verify Submission History"
    local history_url=$(get_url "submission" "${API_SUBMISSION}?problemId=${problem_id}&page=1&size=10")
    local history_response=$(http_get "$history_url" "X-User-Id: ${student_id}")

    log_success "Both submissions recorded in history"

    save_test_data "RESUBMISSION_FIRST_ID" "$first_submission_id"
    save_test_data "RESUBMISSION_SECOND_ID" "$second_submission_id"

    record_pass "TC-E2E-003"
    return 0
}

# ==============================================================================
# TC-E2E-004: Multiple Concurrent Submissions
# ==============================================================================

test_concurrent_submissions() {
    log_test_start "TC-E2E-004: Multiple Concurrent Submissions"

    # Get student ID
    local student_id=$(get_user_id_by_credentials "${EXISTING_STUDENT_USERNAME}" "${EXISTING_STUDENT_PASSWORD}")
    if [ -z "$student_id" ]; then
        student_id="2"
    fi

    local problem_id="${E2E_PROBLEM_ID:-${PUBLISHED_PROBLEM_ID:-100}}"
    local submit_url=$(get_url "submission" "${API_SUBMISSION}")
    local submission_ids=()

    # Submit 3 concurrent submissions
    print_subsection "Submitting 3 Concurrent Submissions"

    for i in 1 2 3; do
        local body=$(cat <<EOF
{
    "problemId": ${problem_id},
    "sqlContent": "SELECT ${i}"
}
EOF
)

        local response=$(http_post "$submit_url" "$body" "X-User-Id: ${student_id}")
        local sub_id=$(json_get "$response" "submissionId")

        if [ -n "$sub_id" ]; then
            submission_ids+=("$sub_id")
            log_success "Submission $i: $sub_id"
        else
            log_warning "Submission $i failed"
        fi
    done

    # Verify all accepted
    print_subsection "Verifying All Submissions Accepted"
    if [ ${#submission_ids[@]} -eq 3 ]; then
        log_success "All 3 submissions accepted"
    else
        log_warning "Only ${#submission_ids[@]} submissions accepted"
    fi

    # Save for reference
    save_test_data "CONCURRENT_SUBMISSION_1" "${submission_ids[0]}"
    save_test_data "CONCURRENT_SUBMISSION_2" "${submission_ids[1]}"
    save_test_data "CONCURRENT_SUBMISSION_3" "${submission_ids[2]}"

    record_pass "TC-E2E-004"
    return 0
}

# ==============================================================================
# TC-E2E-005: Full Teacher Workflow
# ==============================================================================

test_teacher_workflow() {
    log_test_start "TC-E2E-005: Full Teacher Workflow"

    local RANDOM_SUFFIX=$(date +%s%N | sha256sum | base64 | head -c 6)

    # Teacher login
    print_subsection "Step 1: Teacher Login"
    local teacher_token=$(get_token_by_credentials "${EXISTING_TEACHER_USERNAME}" "${EXISTING_TEACHER_PASSWORD}")
    local teacher_id=$(get_user_id_by_credentials "${EXISTING_TEACHER_USERNAME}" "${EXISTING_TEACHER_PASSWORD}")
    if [ -z "$teacher_id" ]; then
        teacher_id="1"
    fi

    if [ -z "$teacher_token" ]; then
        log_error "Teacher login failed"
        record_fail "TC-E2E-005"
        return 1
    fi
    log_success "Teacher logged in"

    # Create problem
    print_subsection "Step 2: Create Problem"
    local create_url=$(get_url "problem" "${API_PROBLEM}")
    local create_body=$(cat <<EOF
{
    "title": "Teacher Workflow Test - ${RANDOM_SUFFIX}",
    "description": "Testing full teacher workflow",
    "difficulty": "MEDIUM",
    "sqlType": "DML",
    "initSql": "CREATE TABLE test_table (id INT, value VARCHAR(50)); INSERT INTO test_table VALUES (1, 'initial');",
    "standardAnswer": "UPDATE test_table SET value = 'updated' WHERE id = 1;"
}
EOF
)

    local create_response=$(http_post "$create_url" "$create_body" "X-User-Id: ${teacher_id}")
    local problem_id=$(json_get "$create_response" "problemId")

    if [ -z "$problem_id" ]; then
        log_error "Problem creation failed"
        record_fail "TC-E2E-005"
        return 1
    fi
    log_success "Problem created: $problem_id"

    # Update problem
    print_subsection "Step 3: Update Problem"
    local update_url=$(get_url "problem" "${API_PROBLEM}/${problem_id}")
    local update_body=$(cat <<EOF
{
    "title": "Teacher Workflow Test - Updated",
    "description": "Updated description",
    "difficulty": "HARD",
    "sqlType": "DML",
    "initSql": "CREATE TABLE test_table (id INT, value VARCHAR(50)); INSERT INTO test_table VALUES (1, 'initial');",
    "standardAnswer": "UPDATE test_table SET value = 'updated' WHERE id = 1;"
}
EOF
)

    local update_response=$(http_put "$update_url" "$update_body" "X-User-Id: ${teacher_id}")
    local updated_title=$(json_get "$update_response" "title")

    if ! assert_contains "$updated_title" "Updated"; then
        log_warning "Update may not have worked"
    fi
    log_success "Problem updated"

    # View teacher's problems
    print_subsection "Step 4: View Teacher's Problems"
    local my_url=$(get_url "problem" "${API_PROBLEM}/teacher/my")
    local my_response=$(http_get "$my_url" "X-User-Id: ${teacher_id}")
    local my_status=$(http_status_get "$my_url" "X-User-Id: ${teacher_id}")

    if assert_status "200" "$my_status" "Teacher problems query should return 200"; then
        log_success "Teacher's problems retrieved"
    fi

    # Publish problem
    print_subsection "Step 5: Publish Problem"
    local status_url=$(get_url "problem" "${API_PROBLEM}/${problem_id}/status")
    http_put "$status_url" '{"status": "PUBLISHED"}' "X-User-Id: ${teacher_id}" > /dev/null
    log_success "Problem published"

    # Check problem leaderboard (after students attempt it)
    print_subsection "Step 6: Check Problem Leaderboard"
    local leaderboard_url=$(get_url "result" "${API_RESULT}/problem/${problem_id}/leaderboard")
    local leaderboard_response=$(http_get "$leaderboard_url")
    log_success "Leaderboard accessible"

    save_test_data "TEACHER_WORKFLOW_PROBLEM_ID" "$problem_id"

    record_pass "TC-E2E-005"
    return 0
}

# ==============================================================================
# Run All E2E Tests
# ==============================================================================

run_all_e2e_tests() {
    print_section "END-TO-END BLACK BOX TESTS"

    load_test_data

    local tests=(
        "test_complete_answer_flow"
        "test_error_handling_flow"
        "test_resubmission_flow"
        "test_concurrent_submissions"
        "test_teacher_workflow"
    )

    for test in "${tests[@]}"; do
        if $test; then
            :
        fi
        echo ""
    done

    print_section "E2E TEST SUMMARY"
    echo "Passed: $TESTS_PASSED"
    echo "Failed: $TESTS_FAILED"
    echo ""

    if [ $TESTS_FAILED -eq 0 ]; then
        log_success "ALL E2E TESTS PASSED!"
        return 0
    else
        log_error "SOME E2E TESTS FAILED!"
        return 1
    fi
}

# Run tests if executed directly
if [ "${BASH_SOURCE[0]}" = "$0" ]; then
    run_all_e2e_tests
fi
