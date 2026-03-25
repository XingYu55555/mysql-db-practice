package com.sqljudge.judgeservice.model.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SqlExecutionResult {
    private boolean success;
    private String errorMessage;
    private Long executionTimeMs;
    private List<Map<String, Object>> resultSet;
    private Integer affectedRows;
    private String sqlType;
}