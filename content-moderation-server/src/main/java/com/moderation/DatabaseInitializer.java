package com.moderation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * 数据库初始化器
 * 在应用启动时执行数据库初始化脚本
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseInitializer implements CommandLineRunner {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting database initialization...");
        
        try {
            // 测试数据库连接
            testConnection();
            
            // 执行初始化脚本
            executeInitScript();
            
            log.info("Database initialization completed successfully!");
        } catch (Exception e) {
            log.error("Database initialization failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 测试数据库连接
     */
    private void testConnection() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            log.info("Database connection successful!");
            log.info("Database URL: {}", connection.getMetaData().getURL());
            log.info("Database User: {}", connection.getMetaData().getUserName());
            log.info("Database Product: {} {}", 
                connection.getMetaData().getDatabaseProductName(),
                connection.getMetaData().getDatabaseProductVersion());
        }
    }

    /**
     * 执行初始化脚本
     */
    private void executeInitScript() {
        String scriptPath = "db/init.sql";
        
        try {
            // 读取初始化脚本
            String sqlScript = new String(
                Files.readAllBytes(Paths.get(scriptPath)), 
                StandardCharsets.UTF_8
            );
            
            // 分割 SQL 语句（按分号分割）
            List<String> sqlStatements = splitSqlStatements(sqlScript);
            
            // 执行每个 SQL 语句
            int executedCount = 0;
            for (String sql : sqlStatements) {
                String trimmedSql = sql.trim();
                if (!trimmedSql.isEmpty() && !trimmedSql.startsWith("--")) {
                    try {
                        jdbcTemplate.execute(trimmedSql);
                        executedCount++;
                    } catch (Exception e) {
                        // 忽略已存在的表错误
                        if (!e.getMessage().contains("already exists")) {
                            log.warn("SQL execution warning: {}", e.getMessage());
                        }
                    }
                }
            }
            
            log.info("Executed {} SQL statements from {}", executedCount, scriptPath);
            
            // 验证表是否创建成功
            verifyTables();
            verifyCriticalColumns();
            
        } catch (IOException e) {
            log.error("Failed to read init script: {}", e.getMessage());
            throw new RuntimeException("Failed to read database init script", e);
        }
    }

    /**
     * 分割 SQL 语句
     */
    private List<String> splitSqlStatements(String sqlScript) {
        // 简单按分号分割，实际生产环境可能需要更复杂的解析
        return List.of(sqlScript.split(";"));
    }

    /**
     * 验证表是否创建成功
     */
    private void verifyTables() {
        List<String> expectedTables = List.of(
            "video_analysis_task",
            "violation_event",
            "moderation_record",
            "creator_health_score",
            "health_score_record"
        );
        
        for (String tableName : expectedTables) {
            try {
                Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + tableName, 
                    Integer.class
                );
                log.info("Table '{}' exists with {} rows", tableName, count);
            } catch (Exception e) {
                log.error("Table '{}' verification failed: {}", tableName, e.getMessage());
            }
        }
    }

    /**
     * 严格校验关键字段，防止代码已发布但迁移未生效导致运行期 SQL 报错
     */
    private void verifyCriticalColumns() {
        verifyColumnOrThrow("video_analysis_task", "draft_payload_json");
    }

    private void verifyColumnOrThrow(String tableName, String columnName) {
        String sql = """
                SELECT 1
                FROM information_schema.columns
                WHERE table_name = ?
                  AND column_name = ?
                LIMIT 1
                """;
        try {
            Integer exists = jdbcTemplate.query(connection -> {
                var ps = connection.prepareStatement(sql);
                ps.setString(1, tableName);
                ps.setString(2, columnName);
                return ps;
            }, (ResultSet rs) -> rs.next() ? 1 : 0);

            if (exists == null || exists != 1) {
                throw new IllegalStateException("Missing required column: " + tableName + "." + columnName);
            }
            log.info("Verified column '{}.{}' exists", tableName, columnName);
        } catch (Exception e) {
            throw new IllegalStateException("Critical schema verification failed for " + tableName + "." + columnName, e);
        }
    }
}
