package com.shop.db;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

@Component
public class DB {
    private static final String SCHEMA_RESOURCE = "db/schema.sql";  //file chứa schema

    private final String url;
    private final String username;
    private final String password;

    public DB(
            @Value("${app.db.url:jdbc:postgresql://localhost:5432/auction_shop}") String url,  //nếu không tìm thay cấu hình app.db.url thì sẽ dùng sau dấu hai chấm
            @Value("${app.db.username:postgres}") String username,
            @Value("${app.db.password:}") String password
    ) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    @PostConstruct  // ngay sau khi khởi tạo class thì tự động chạy phương thức này
    public void initializeSchema() {
        List<String> statements;
        try {
            statements = loadSchemaStatements();  // đọc file schema.sql
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load database schema resource", e);
        }

        if (statements.isEmpty()) {
            return;
        }

        try (Connection connection = getConnection()) {
            for (String statement : statements) {
                try (Statement sql = connection.createStatement()) { //gửi lenh sql xuống database
                    sql.execute(statement);  // database thực hiện câu lệnh sql
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to initialize database schema", e);
        }
    }
    //phương thức mở kết nối
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    private List<String> loadSchemaStatements() throws IOException {
        ClassPathResource resource = new ClassPathResource(SCHEMA_RESOURCE); //tìm file schema.sql
        if (!resource.exists()) {
            return List.of();
        }

        try (InputStream inputStream = resource.getInputStream()) {
            String sql = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8); // đọc file
            String withoutComments = sql.replaceAll("(?m)^\\s*--.*$", "");// xóa kí tự đặc biệt1--
            return Arrays.stream(withoutComments.split(";"))
                    .map(String::trim)
                    .filter(statement -> !statement.isBlank())
                    .toList();
        }
    }
}
