#!/bin/bash

# ==============================================================================
# SQL Judge Engine - User Service Black Box Tests
# ==============================================================================
# Tests for user-service API endpoints
# Reference: integration-test-architecture.md Section 2.1
# ==============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/config.sh"
source "${SCRIPT_DIR}/common.sh"

# Test result tracking
TESTS_PASSED=0
TESTS_FAILED=0

# ==============================================================================
# TC-USER-001: User Registration Success
# ==============================================================================

test_user_register_success() {
    log_test_start "TC-USER-001: User Registration Success"

    local username="testteacher_${RANDOM}_${RANDOM}"
    local password="Password123!"
    local email="teacher_${RANDOM}_${RANDOM}@test.com"

    local request_body=$(cat <<EOF
{
    "username": "${username}",
    "password": "${password}",
    "role": "TEACHER",
    "email": "${email}"
}
EOF
)

    local url=$(get_url "user" "${API_USER}/register")

    print_subsection "Sending POST request"
    # Capture both status and response in one request
    local response_file=$(mktemp)
    local status=$(curl -s -o "$response_file" -w "%{http_code}" -X POST \
        -H "Content-Type: application/json" \
        --max-time "$HTTP_TIMEOUT" \
        -d "$request_body" \
        "$url")
    local response=$(cat "$response_file")
    rm -f "$response_file"

    log_info "Response: $response"

    print_subsection "Verifying Response"

    # Check status code is 201 Created
    if ! assert_status "201" "$status" "Registration should return 201"; then
        log_error "Response: $response"
        record_fail "TC-USER-001"
        return 1
    fi

    # Extract and verify user ID (API returns 'userId' instead of 'id')
    if ! json_has_key "$response" "userId"; then
        log_error "Response missing 'userId' field"
        log_error "Response: $response"
        record_fail "TC-USER-001"
        return 1
    fi

    local user_id=$(json_get "$response" "userId")
    save_test_data "REGISTERED_TEACHER_ID" "$user_id"
    save_test_data "REGISTERED_TEACHER_USERNAME" "$username"
    save_test_data "REGISTERED_TEACHER_PASSWORD" "$password"

    # Verify username in response
    local response_username=$(json_get "$response" "username")
    if ! assert_equals "$username" "$response_username" "Username mismatch"; then
        record_fail "TC-USER-001"
        return 1
    fi

    # Verify role in response
    local response_role=$(json_get "$response" "role")
    if ! assert_equals "TEACHER" "$response_role" "Role mismatch"; then
        record_fail "TC-USER-001"
        return 1
    fi

    record_pass "TC-USER-001"
    return 0
}

# ==============================================================================
# TC-USER-002: User Registration Failure - Username Already Exists
# ==============================================================================

test_user_register_duplicate() {
    log_test_start "TC-USER-002: User Registration Failure - Duplicate Username"

    # First, register a user
    local username="duplicate_user_${RANDOM}"
    local request_body=$(cat <<EOF
{
    "username": "${username}",
    "password": "Password123!",
    "role": "STUDENT",
    "email": "first@test.com"
}
EOF
)

    local url=$(get_url "user" "${API_USER}/register")

    # First registration should succeed
    print_subsection "First registration (should succeed)"
    http_post "$url" "$request_body" > /dev/null

    # Second registration with same username should fail
    print_subsection "Second registration with same username (should fail)"
    local status=$(http_status_post "$url" "$request_body")

    if ! assert_status "409" "$status" "Duplicate registration should return 409"; then
        record_fail "TC-USER-002"
        return 1
    fi

    record_pass "TC-USER-002"
    return 0
}

# ==============================================================================
# TC-USER-003: User Login Success
# ==============================================================================

test_user_login_success() {
    log_test_start "TC-USER-003: User Login Success"

    # Use existing test user from config
    local url=$(get_url "user" "${API_USER}/login")

    local request_body=$(cat <<EOF
{
    "username": "${EXISTING_TEACHER_USERNAME}",
    "password": "${EXISTING_TEACHER_PASSWORD}"
}
EOF
)

    print_subsection "Sending login request"
    local response=$(http_post "$url" "$request_body")

    print_subsection "Verifying Response"

    local status=$(http_status_post "$url" "$request_body")

    if ! assert_status "200" "$status" "Login should return 200"; then
        log_error "Response: $response"
        record_fail "TC-USER-003"
        return 1
    fi

    # Verify token is returned
    if ! json_has_key "$response" "token"; then
        log_error "Response missing 'token' field"
        log_error "Response: $response"
        record_fail "TC-USER-003"
        return 1
    fi

    local token=$(json_get "$response" "token")

    # Verify token is not empty and looks like JWT
    if [ -z "$token" ] || [ ${#token} -lt 20 ]; then
        log_error "Token appears invalid: $token"
        record_fail "TC-USER-003"
        return 1
    fi

    save_test_data "TEACHER_TOKEN" "$token"

    log_success "Token received: ${token:0:50}..."
    record_pass "TC-USER-003"
    return 0
}

# ==============================================================================
# TC-USER-004: User Login Failure - Wrong Password
# ==============================================================================

test_user_login_wrong_password() {
    log_test_start "TC-USER-004: User Login Failure - Wrong Password"

    local url=$(get_url "user" "${API_USER}/login")

    local request_body=$(cat <<EOF
{
    "username": "${EXISTING_TEACHER_USERNAME}",
    "password": "WrongPassword!"
}
EOF
)

    print_subsection "Sending login request with wrong password"
    local status=$(http_status_post "$url" "$request_body")

    if ! assert_status "401" "$status" "Wrong password should return 401"; then
        record_fail "TC-USER-004"
        return 1
    fi

    record_pass "TC-USER-004"
    return 0
}

# ==============================================================================
# TC-USER-005: User Login Failure - User Not Found
# ==============================================================================

test_user_login_not_found() {
    log_test_start "TC-USER-005: User Login Failure - User Not Found"

    local url=$(get_url "user" "${API_USER}/login")

    local request_body=$(cat <<EOF
{
    "username": "nonexistent_user_12345",
    "password": "SomePassword123!"
}
EOF
)

    print_subsection "Sending login request for non-existent user"
    local status=$(http_status_post "$url" "$request_body")

    if ! assert_status "401" "$status" "Non-existent user should return 401"; then
        record_fail "TC-USER-005"
        return 1
    fi

    record_pass "TC-USER-005"
    return 0
}

# ==============================================================================
# TC-USER-006: Get Current User Info (with valid JWT)
# ==============================================================================

test_user_me() {
    log_test_start "TC-USER-006: Get Current User Info"

    # First, login to get token
    local login_url=$(get_url "user" "${API_USER}/login")
    local login_body=$(cat <<EOF
{
    "username": "${EXISTING_TEACHER_USERNAME}",
    "password": "${EXISTING_TEACHER_PASSWORD}"
}
EOF
)

    print_subsection "Logging in to get token"
    local login_response=$(http_post "$login_url" "$login_body")
    local token=$(json_get "$login_response" "token")

    if [ -z "$token" ]; then
        log_error "Failed to get token"
        record_fail "TC-USER-006"
        return 1
    fi

    # Now get user info
    local url=$(get_url "user" "${API_USER}/me")

    print_subsection "Getting current user info"
    local response=$(http_get "$url" "Authorization: Bearer $token")
    local status=$(http_status_get "$url" "Authorization: Bearer $token")

    if ! assert_status "200" "$status" "Get user info should return 200"; then
        log_error "Response: $response"
        record_fail "TC-USER-006"
        return 1
    fi

    # Verify response contains user info
    if ! json_has_key "$response" "userId"; then
        log_error "Response missing 'userId' field"
        record_fail "TC-USER-006"
        return 1
    fi

    local user_id=$(json_get "$response" "userId")
    save_test_data "TEACHER_ID" "$user_id"

    log_success "User ID: $user_id"
    record_pass "TC-USER-006"
    return 0
}

# ==============================================================================
# TC-USER-007: Get User Info by ID
# ==============================================================================

test_user_get_by_id() {
    log_test_start "TC-USER-007: Get User Info by ID"

    # First login to get token
    local login_url=$(get_url "user" "${API_USER}/login")
    local login_body=$(cat <<EOF
{
    "username": "${EXISTING_TEACHER_USERNAME}",
    "password": "${EXISTING_TEACHER_PASSWORD}"
}
EOF
)

    local login_response=$(http_post "$login_url" "$login_body")
    local token=$(json_get "$login_response" "token")

    # Get user ID from login response or use default
    local user_id=$(json_get "$login_response" "userId")
    if [ -z "$user_id" ]; then
        user_id="1"  # Fallback to default teacher ID
    fi

    local url=$(get_url "user" "${API_USER}/${user_id}")

    print_subsection "Getting user info for ID: $user_id"
    local response=$(http_get "$url" "Authorization: Bearer $token")
    local status=$(http_status_get "$url" "Authorization: Bearer $token")

    if ! assert_status "200" "$status" "Get user by ID should return 200"; then
        log_error "Response: $response"
        record_fail "TC-USER-007"
        return 1
    fi

    # Verify user ID matches
    local response_id=$(json_get "$response" "userId")
    if ! assert_equals "$user_id" "$response_id" "User ID mismatch"; then
        record_fail "TC-USER-007"
        return 1
    fi

    record_pass "TC-USER-007"
    return 0
}

# ==============================================================================
# TC-USER-008: Get User Info without Token (should fail)
# ==============================================================================

test_user_me_unauthorized() {
    log_test_start "TC-USER-008: Get User Info without Token (should fail)"

    local url=$(get_url "user" "${API_USER}/me")

    print_subsection "Getting user info without Authorization header"
    local status=$(http_status_get "$url")

    # Should return 401 or 403
    if [ "$status" = "401" ] || [ "$status" = "403" ]; then
        log_success "Correctly denied access without token (status: $status)"
        record_pass "TC-USER-008"
        return 0
    else
        log_error "Expected 401 or 403, got: $status"
        record_fail "TC-USER-008"
        return 1
    fi
}

# ==============================================================================
# Run All User Service Tests
# ==============================================================================

run_all_user_tests() {
    print_section "USER-SERVICE BLACK BOX TESTS"

    # Load any existing test data
    load_test_data

    local tests=(
        "test_user_register_success"
        "test_user_register_duplicate"
        "test_user_login_success"
        "test_user_login_wrong_password"
        "test_user_login_not_found"
        "test_user_me"
        "test_user_get_by_id"
        "test_user_me_unauthorized"
    )

    for test in "${tests[@]}"; do
        if $test; then
            :
        fi
        echo ""
    done

    print_section "USER-SERVICE TEST SUMMARY"
    echo "Passed: $TESTS_PASSED"
    echo "Failed: $TESTS_FAILED"
    echo ""

    if [ $TESTS_FAILED -eq 0 ]; then
        log_success "ALL USER-SERVICE TESTS PASSED!"
        return 0
    else
        log_error "SOME USER-SERVICE TESTS FAILED!"
        return 1
    fi
}

# Run tests if executed directly
if [ "${BASH_SOURCE[0]}" = "$0" ]; then
    run_all_user_tests
fi
