#!/bin/bash

# ==============================================================================
# SQL Judge Engine - Common Function Library
# ==============================================================================
# This file contains reusable functions for black box testing.
# ==============================================================================

# Source configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/config.sh"

# ==============================================================================
# Logging Functions
# ==============================================================================

log_info() {
    echo -e "${COLOR_BLUE}[INFO]${COLOR_RESET} $1"
}

log_success() {
    echo -e "${COLOR_GREEN}[SUCCESS]${COLOR_RESET} $1"
}

log_warning() {
    echo -e "${COLOR_YELLOW}[WARNING]${COLOR_RESET} $1"
}

log_error() {
    echo -e "${COLOR_RED}[ERROR]${COLOR_RESET} $1"
}

log_test_start() {
    echo ""
    echo -e "${COLOR_BLUE}========================================${COLOR_RESET}"
    echo -e "${COLOR_BLUE}TEST: $1${COLOR_RESET}"
    echo -e "${COLOR_BLUE}========================================${COLOR_RESET}"
}

log_test_end() {
    echo -e "${COLOR_BLUE}----------------------------------------${COLOR_RESET}"
}

# ==============================================================================
# HTTP Request Functions
# ==============================================================================

# Send HTTP GET request
# Usage: http_get <url> [headers...]
http_get() {
    local url="$1"
    shift
    local headers=("$@")

    log_info "GET $url"

    local header_args=()
    for header in "${headers[@]}"; do
        header_args+=("-H" "$header")
    done

    curl -s -X GET \
        -H "Content-Type: application/json" \
        "${header_args[@]}" \
        --max-time "$HTTP_TIMEOUT" \
        "$url"
}

# Send HTTP POST request
# Usage: http_post <url> <body> [headers...]
http_post() {
    local url="$1"
    local body="$2"
    shift 2
    local headers=("$@")

    log_info "POST $url" >&2
    log_info "Body: $body" >&2

    local header_args=()
    for header in "${headers[@]}"; do
        header_args+=("-H" "$header")
    done

    response=$(curl -s -X POST \
        -H "Content-Type: application/json" \
        "${header_args[@]}" \
        --max-time "$HTTP_TIMEOUT" \
        -d "$body" \
        "$url")

    echo "$response"
}

# Send HTTP PUT request
# Usage: http_put <url> <body> [headers...]
http_put() {
    local url="$1"
    local body="$2"
    shift 2
    local headers=("$@")

    log_info "PUT $url" >&2
    log_info "Body: $body" >&2

    local header_args=()
    for header in "${headers[@]}"; do
        header_args+=("-H" "$header")
    done

    response=$(curl -s -X PUT \
        -H "Content-Type: application/json" \
        "${header_args[@]}" \
        --max-time "$HTTP_TIMEOUT" \
        -d "$body" \
        "$url")

    echo "$response"
}

# Send HTTP DELETE request
# Usage: http_delete <url> [headers...]
http_delete() {
    local url="$1"
    shift
    local headers=("$@")

    log_info "DELETE $url"

    local header_args=()
    for header in "${headers[@]}"; do
        header_args+=("-H" "$header")
    done

    response=$(curl -s -X DELETE \
        -H "Content-Type: application/json" \
        "${header_args[@]}" \
        --max-time "$HTTP_TIMEOUT" \
        "$url")

    echo "$response"
}

# Get HTTP status code only
# Usage: http_status_get <url> [headers...]
http_status_get() {
    local url="$1"
    shift
    local headers=("$@")

    local header_args=()
    for header in "${headers[@]}"; do
        header_args+=("-H" "$header")
    done

    curl -s -o /dev/null -w "%{http_code}" -X GET \
        -H "Content-Type: application/json" \
        "${header_args[@]}" \
        --max-time "$HTTP_TIMEOUT" \
        "$url"
}

# Get HTTP status code for POST
http_status_post() {
    local url="$1"
    local body="$2"
    shift 2
    local headers=("$@")

    local header_args=()
    for header in "${headers[@]}"; do
        header_args+=("-H" "$header")
    done

    curl -s -o /dev/null -w "%{http_code}" -X POST \
        -H "Content-Type: application/json" \
        "${header_args[@]}" \
        --max-time "$HTTP_TIMEOUT" \
        -d "$body" \
        "$url"
}

# Get HTTP status code for PUT
http_status_put() {
    local url="$1"
    local body="$2"
    shift 2
    local headers=("$@")

    local header_args=()
    for header in "${headers[@]}"; do
        header_args+=("-H" "$header")
    done

    curl -s -o /dev/null -w "%{http_code}" -X PUT \
        -H "Content-Type: application/json" \
        "${header_args[@]}" \
        --max-time "$HTTP_TIMEOUT" \
        -d "$body" \
        "$url"
}

# Get HTTP status code for DELETE
http_status_delete() {
    local url="$1"
    shift
    local headers=("$@")

    local header_args=()
    for header in "${headers[@]}"; do
        header_args+=("-H" "$header")
    done

    curl -s -o /dev/null -w "%{http_code}" -X DELETE \
        -H "Content-Type: application/json" \
        "${header_args[@]}" \
        --max-time "$HTTP_TIMEOUT" \
        "$url"
}

# ==============================================================================
# JSON Response Functions (using grep and sed for simple extraction)
# ==============================================================================

# Extract value from JSON by key (simple regex-based parser)
# Usage: json_get <json_string> <key>
json_get() {
    local json="$1"
    local key="$2"

    echo "$json" | grep -o "\"${key}\"[[:space:]]*:[[:space:]]*[^,}]*" | \
        sed 's/.*:[[:space:]]*//' | \
        tr -d '"' | \
        tr -d "'"
}

# Check if JSON contains a specific key
json_has_key() {
    local json="$1"
    local key="$2"

    echo "$json" | grep -q "\"${key}\""
}

# Check if JSON indicates success (no "error" or "message" with error)
json_is_success() {
    local json="$1"

    if echo "$json" | grep -q '"error"'; then
        return 1
    fi

    return 0
}

# ==============================================================================
# Assertion Functions
# ==============================================================================

# Assert HTTP status code
# Usage: assert_status <expected> <actual> [message]
assert_status() {
    local expected="$1"
    local actual="$2"
    local message="${3:-Status code mismatch}"

    if [ "$expected" = "$actual" ]; then
        log_success "Status: $actual (expected: $expected)"
        return 0
    else
        log_error "$message"
        log_error "Expected: $expected, Actual: $actual"
        return 1
    fi
}

# Assert JSON field exists
# Usage: assert_json_has <json> <key> [message]
assert_json_has() {
    local json="$1"
    local key="$2"
    local message="${3:-JSON field missing}"

    if json_has_key "$json" "$key"; then
        local value=$(json_get "$json" "$key")
        log_success "Field '$key' = '$value'"
        return 0
    else
        log_error "$message"
        log_error "JSON: $json"
        return 1
    fi
}

# Assert string contains substring
# Usage: assert_contains <string> <substring> [message]
assert_contains() {
    local string="$1"
    local substring="$2"
    local message="${3:-String does not contain substring}"

    if echo "$string" | grep -q "$substring"; then
        log_success "Contains: '$substring'"
        return 0
    else
        log_error "$message"
        log_error "String: $string"
        return 1
    fi
}

# Assert string equals expected
# Usage: assert_equals <expected> <actual> [message]
assert_equals() {
    local expected="$1"
    local actual="$2"
    local message="${3:-String mismatch}"

    if [ "$expected" = "$actual" ]; then
        log_success "Equals: '$expected'"
        return 0
    else
        log_error "$message"
        log_error "Expected: '$expected', Actual: '$actual'"
        return 1
    fi
}

# Assert numeric greater than
# Usage: assert_gt <actual> <min> [message]
assert_gt() {
    local actual="$1"
    local min="$2"
    local message="${3:-Value not greater than minimum}"

    if [ "$actual" -gt "$min" ] 2>/dev/null; then
        log_success "$actual > $min"
        return 0
    else
        log_error "$message"
        log_error "Expected: > $min, Actual: $actual"
        return 1
    fi
}

# ==============================================================================
# Utility Functions
# ==============================================================================

# Wait for condition with polling
# Usage: wait_for <condition_func> <max_attempts> <interval_seconds>
wait_for() {
    local condition_func="$1"
    local max_attempts="$2"
    local interval="$3"
    local attempt=1

    log_info "Waiting for condition (max ${max_attempts} attempts, ${interval}s interval)..."

    while [ $attempt -le $max_attempts ]; do
        if $condition_func; then
            log_success "Condition met after $attempt attempt(s)"
            return 0
        fi

        log_info "Attempt $attempt/$max_attempts - waiting..."
        sleep "$interval"
        attempt=$((attempt + 1))
    done

    log_error "Condition not met after $max_attempts attempts"
    return 1
}

# Generate random string
generate_random() {
    date +%s%N | sha256sum | base64 | head -c 8
}

# Save test data to file
save_test_data() {
    local key="$1"
    local value="$2"
    echo "${key}=${value}" >> "${TEST_RESULT_DIR}/test-data.env"
}

# Load test data from file
load_test_data() {
    if [ -f "${TEST_RESULT_DIR}/test-data.env" ]; then
        source "${TEST_RESULT_DIR}/test-data.env"
    fi
}

# Clear test data
clear_test_data() {
    rm -f "${TEST_RESULT_DIR}/test-data.env"
}

# Print section header
print_section() {
    echo ""
    echo -e "${COLOR_YELLOW}========================================${COLOR_RESET}"
    echo -e "${COLOR_YELLOW}$1${COLOR_RESET}"
    echo -e "${COLOR_YELLOW}========================================${COLOR_RESET}"
}

# Print subsection header
print_subsection() {
    echo ""
    echo -e "${COLOR_BLUE}--- $1 ---${COLOR_RESET}"
}

# ==============================================================================
# Test Result Functions
# ==============================================================================

TESTS_PASSED=0
TESTS_FAILED=0
TESTS_SKIPPED=0

record_pass() {
    TESTS_PASSED=$((TESTS_PASSED + 1))
    log_success "TEST PASSED: $1"
}

record_fail() {
    TESTS_FAILED=$((TESTS_FAILED + 1))
    log_error "TEST FAILED: $1"
}

record_skip() {
    TESTS_SKIPPED=$((TESTS_SKIPPED + 1))
    log_warning "TEST SKIPPED: $1"
}

print_test_summary() {
    print_section "TEST SUMMARY"
    echo "Passed: $TESTS_PASSED"
    echo "Failed: $TESTS_FAILED"
    echo "Skipped: $TESTS_SKIPPED"
    echo ""

    if [ $TESTS_FAILED -eq 0 ]; then
        log_success "ALL TESTS PASSED!"
        return 0
    else
        log_error "SOME TESTS FAILED!"
        return 1
    fi
}
