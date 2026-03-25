#!/bin/bash

# ==============================================================================
# SQL Judge Engine - Black Box Testing Configuration
# ==============================================================================
# This file contains all configuration for black box testing.
# Modify these values according to your environment.
# ==============================================================================

# Service Base URLs (through Gateway)
export GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
export USER_SERVICE_URL="${USER_SERVICE_URL:-http://localhost:8081}"
export PROBLEM_SERVICE_URL="${PROBLEM_SERVICE_URL:-http://localhost:8082}"
export SUBMISSION_SERVICE_URL="${SUBMISSION_SERVICE_URL:-http://localhost:8083}"
export RESULT_SERVICE_URL="${RESULT_SERVICE_URL:-http://localhost:8086}"

# Use direct service URL (bypass gateway) for testing
# In production, always use gateway. For testing, direct access is acceptable.
export USE_DIRECT_URL="${USE_DIRECT_URL:-true}"

# API Base Paths
export API_USER="/api/user"
export API_PROBLEM="/api/problem"
export API_SUBMISSION="/api/submission"
export API_RESULT="/api/result"

# Test Data - Users
export TEST_TEACHER_USERNAME="testteacher_$(date +%s)"
export TEST_TEACHER_PASSWORD="Password123!"
export TEST_TEACHER_EMAIL="teacher@test.com"
export TEST_TEACHER_ROLE="TEACHER"

export TEST_STUDENT_USERNAME="teststudent_$(date +%s)"
export TEST_STUDENT_PASSWORD="Password123!"
export TEST_STUDENT_EMAIL="student@test.com"
export TEST_STUDENT_ROLE="STUDENT"

# Pre-existing test users (should be created by setup script)
export EXISTING_TEACHER_USERNAME="teacher1"
export EXISTING_TEACHER_PASSWORD="password"
export EXISTING_STUDENT_USERNAME="student1"
export EXISTING_STUDENT_PASSWORD="password"

# Test Timeouts (seconds)
export HTTP_TIMEOUT=30
export POLL_INTERVAL=2
export MAX_POLL_COUNT=30

# Colors for output
export COLOR_RED='\033[0;31m'
export COLOR_GREEN='\033[0;32m'
export COLOR_YELLOW='\033[1;33m'
export COLOR_BLUE='\033[0;34m'
export COLOR_RESET='\033[0m'

# Test Result Directory
export TEST_RESULT_DIR="${TEST_RESULT_DIR:-./test-results}"
mkdir -p "$TEST_RESULT_DIR"

# Global variables to store test data
TEACHER_ID=""
STUDENT_ID=""
TEACHER_TOKEN=""
STUDENT_TOKEN=""
PROBLEM_ID=""
SUBMISSION_ID=""

# Get the appropriate URL based on configuration
get_url() {
    local service="$1"
    local path="$2"

    if [ "$USE_DIRECT_URL" = "true" ]; then
        case "$service" in
            "user") echo "${USER_SERVICE_URL}${path}" ;;
            "problem") echo "${PROBLEM_SERVICE_URL}${path}" ;;
            "submission") echo "${SUBMISSION_SERVICE_URL}${path}" ;;
            "result") echo "${RESULT_SERVICE_URL}${path}" ;;
            *) echo "${GATEWAY_URL}${path}" ;;
        esac
    else
        echo "${GATEWAY_URL}${path}"
    fi
}
