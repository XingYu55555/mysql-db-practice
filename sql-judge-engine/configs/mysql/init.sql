-- SQL Judge Engine Database Initialization
-- This script creates the initial database schema

CREATE DATABASE IF NOT EXISTS business_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE business_db;

-- Users Table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码(BCrypt加密)',
    role ENUM('TEACHER', 'STUDENT') NOT NULL COMMENT '角色',
    email VARCHAR(100) COMMENT '邮箱',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username),
    INDEX idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- Problems Table
CREATE TABLE IF NOT EXISTS problems (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '题目ID',
    title VARCHAR(200) NOT NULL COMMENT '题目标题',
    description TEXT COMMENT '题目描述',
    difficulty ENUM('EASY', 'MEDIUM', 'HARD') DEFAULT 'MEDIUM' COMMENT '难度',
    sql_type ENUM('DQL', 'DML', 'DDL', 'DCL') NOT NULL COMMENT 'SQL类型',
    status ENUM('DRAFT', 'READY', 'PUBLISHED', 'ARCHIVED') DEFAULT 'DRAFT' COMMENT '题目状态',
    ai_assisted BOOLEAN DEFAULT FALSE COMMENT '是否启用AI辅助判题',
    init_sql TEXT COMMENT '初始化SQL脚本',
    standard_answer TEXT COMMENT '标准答案SQL',
    expected_result TEXT COMMENT '期望结果(JSON格式)',
    expected_type ENUM('RESULT_SET', 'DATA_SNAPSHOT', 'METADATA', 'PRIVILEGE') COMMENT '期望结果类型',
    teacher_id BIGINT NOT NULL COMMENT '创建教师ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_teacher (teacher_id),
    INDEX idx_sql_type (sql_type),
    INDEX idx_difficulty (difficulty),
    INDEX idx_status (status),
    CONSTRAINT fk_problems_teacher FOREIGN KEY (teacher_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题目表';

-- Test Cases Table
CREATE TABLE IF NOT EXISTS test_cases (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '测试用例ID',
    problem_id BIGINT NOT NULL COMMENT '所属题目ID',
    name VARCHAR(100) NOT NULL COMMENT '测试用例名称',
    init_sql TEXT COMMENT '该用例的初始化SQL',
    expected_result TEXT COMMENT '期望结果(JSON格式)',
    expected_type ENUM('RESULT_SET', 'DATA_SNAPSHOT', 'METADATA', 'PRIVILEGE') NOT NULL COMMENT '期望结果类型',
    weight INT DEFAULT 1 COMMENT '权重',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_problem (problem_id),
    CONSTRAINT fk_test_cases_problem FOREIGN KEY (problem_id) REFERENCES problems(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='测试用例表';

-- Submissions Table
CREATE TABLE IF NOT EXISTS submissions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '提交ID',
    problem_id BIGINT NOT NULL COMMENT '题目ID',
    student_id BIGINT NOT NULL COMMENT '学生ID',
    sql_content TEXT NOT NULL COMMENT '提交的SQL内容',
    status ENUM('PENDING', 'JUDGING', 'SUCCESS', 'FAILED') DEFAULT 'PENDING' COMMENT '状态',
    submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    INDEX idx_problem (problem_id),
    INDEX idx_student (student_id),
    INDEX idx_status (status),
    INDEX idx_submitted_at (submitted_at),
    INDEX idx_student_problem (student_id, problem_id),
    CONSTRAINT fk_submissions_problem FOREIGN KEY (problem_id) REFERENCES problems(id),
    CONSTRAINT fk_submissions_student FOREIGN KEY (student_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='提交记录表';

-- Judge Results Table
CREATE TABLE IF NOT EXISTS judge_results (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '结果ID',
    submission_id BIGINT NOT NULL COMMENT '提交ID',
    test_case_id BIGINT COMMENT '测试用例ID',
    score DECIMAL(5,2) COMMENT '得分',
    status ENUM('CORRECT', 'INCORRECT', 'TIME_LIMIT', 'ERROR', 'AI_APPROVED', 'AI_REJECTED') NOT NULL COMMENT '状态',
    error_message TEXT COMMENT '错误信息',
    execution_time_ms BIGINT COMMENT '执行时间(毫秒)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_submission (submission_id),
    INDEX idx_test_case (test_case_id),
    INDEX idx_status (status),
    CONSTRAINT fk_judge_results_submission FOREIGN KEY (submission_id) REFERENCES submissions(id),
    CONSTRAINT fk_judge_results_test_case FOREIGN KEY (test_case_id) REFERENCES test_cases(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='判题结果表';

-- Containers Table (for container-manager)
CREATE TABLE IF NOT EXISTS containers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '容器ID',
    docker_container_id VARCHAR(255) COMMENT 'Docker容器ID',
    container_name VARCHAR(255) COMMENT '容器名称',
    ip_address VARCHAR(50) COMMENT 'IP地址',
    mysql_port INT COMMENT 'MySQL端口',
    status ENUM('AVAILABLE', 'IN_USE', 'ERROR', 'DESTROYED') DEFAULT 'AVAILABLE' COMMENT '状态',
    use_count INT DEFAULT 0 COMMENT '使用次数',
    max_uses INT DEFAULT 100 COMMENT '最大使用次数',
    connection_token VARCHAR(255) COMMENT '连接令牌',
    token_expires_at TIMESTAMP NULL COMMENT '令牌过期时间',
    problem_id BIGINT COMMENT '当前题目ID',
    difficulty ENUM('EASY', 'MEDIUM', 'HARD') COMMENT '难度',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    last_used_at TIMESTAMP NULL COMMENT '最后使用时间',
    INDEX idx_status (status),
    INDEX idx_docker_container_id (docker_container_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='容器表';

-- Insert test users
INSERT IGNORE INTO users (username, password, role, email, created_at, updated_at)
VALUES ('teacher1', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.rsS7HmGyDYLKwC5S4y', 'TEACHER', 'teacher@test.com', NOW(), NOW());

INSERT IGNORE INTO users (username, password, role, email, created_at, updated_at)
VALUES ('student1', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.rsS7HmGyDYLKwC5S4y', 'STUDENT', 'student@test.com', NOW(), NOW());
