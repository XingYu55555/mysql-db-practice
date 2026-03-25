package com.sqljudge.judgeservice.service.impl;

import com.sqljudge.judgeservice.model.client.ContainerInfo;
import com.sqljudge.judgeservice.model.client.SqlExecutionResult;
import com.sqljudge.judgeservice.service.SqlExecutorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SqlExecutorServiceImpl implements SqlExecutorService {

    private static final String JDBC_URL_TEMPLATE = "jdbc:mysql://%s:%d/problem_%d?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&connectTimeout=%d&socketTimeout=%d";

    @Override
    public SqlExecutionResult executeInitSql(ContainerInfo container, Long problemId, String initSql, Integer timeout) {
        return executeSql(container, problemId, initSql, timeout, "INIT");
    }

    @Override
    public SqlExecutionResult executeStudentSql(ContainerInfo container, Long problemId, String studentSql, Integer timeout) {
        String sqlType = detectSqlType(studentSql);
        return executeSql(container, problemId, studentSql, timeout, sqlType);
    }

    private SqlExecutionResult executeSql(ContainerInfo container, Long problemId, String sql, Integer timeout, String sqlType) {
        long startTime = System.currentTimeMillis();
        int connectTimeout = timeout != null ? timeout : 30000;
        int socketTimeout = timeout != null ? timeout : 30000;

        String jdbcUrl = String.format(JDBC_URL_TEMPLATE,
                container.getIpAddress(),
                container.getMysqlPort(),
                problemId,
                connectTimeout,
                socketTimeout);

        String username = container.getMysqlUser();
        String password = container.getConnectionToken();

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password)) {
            return executeSqlStatement(conn, sql, sqlType, startTime);
        } catch (SQLException e) {
            log.error("SQL execution failed: {}", e.getMessage(), e);
            return SqlExecutionResult.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .executionTimeMs(System.currentTimeMillis() - startTime)
                    .sqlType(sqlType)
                    .build();
        }
    }

    private SqlExecutionResult executeSqlStatement(Connection conn, String sql, String sqlType, long startTime) {
        try (Statement stmt = conn.createStatement()) {
            boolean isSelect = sql.trim().toUpperCase().startsWith("SELECT");

            if (isSelect) {
                try (ResultSet rs = stmt.executeQuery(sql)) {
                    List<Map<String, Object>> resultSet = extractResultSet(rs);
                    return SqlExecutionResult.builder()
                            .success(true)
                            .resultSet(resultSet)
                            .executionTimeMs(System.currentTimeMillis() - startTime)
                            .sqlType(sqlType)
                            .build();
                }
            } else {
                int affectedRows = stmt.executeUpdate(sql);
                return SqlExecutionResult.builder()
                        .success(true)
                        .affectedRows(affectedRows)
                        .executionTimeMs(System.currentTimeMillis() - startTime)
                        .sqlType(sqlType)
                        .build();
            }
        } catch (SQLException e) {
            log.error("SQL statement execution failed: {}", e.getMessage(), e);
            return SqlExecutionResult.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .executionTimeMs(System.currentTimeMillis() - startTime)
                    .sqlType(sqlType)
                    .build();
        }
    }

    private List<Map<String, Object>> extractResultSet(ResultSet rs) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        while (rs.next()) {
            Map<String, Object> row = new HashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnLabel(i);
                Object value = rs.getObject(i);
                row.put(columnName, value);
            }
            results.add(row);
        }
        return results;
    }

    private String detectSqlType(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return "UNKNOWN";
        }
        String upperSql = sql.trim().toUpperCase();
        if (upperSql.startsWith("SELECT")) {
            return "DQL";
        } else if (upperSql.startsWith("INSERT") || upperSql.startsWith("UPDATE") || upperSql.startsWith("DELETE")) {
            return "DML";
        } else if (upperSql.startsWith("CREATE") || upperSql.startsWith("ALTER") || upperSql.startsWith("DROP") || upperSql.startsWith("TRUNCATE")) {
            return "DDL";
        } else if (upperSql.startsWith("GRANT") || upperSql.startsWith("REVOKE")) {
            return "DCL";
        }
        return "UNKNOWN";
    }
}