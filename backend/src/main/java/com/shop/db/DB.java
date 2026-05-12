package com.shop.db;

import io.r2dbc.spi.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;

@Configuration
public class DB {
    private static final String SCHEMA_RESOURCE = "db/schema.sql"; // file chứa mã SQL

    @Bean
    ConnectionFactoryInitializer connectionFactoryInitializer(ConnectionFactory connectionFactory) {  //Spring Boot tự động tạo ra một ConnectionFactory (chứa thông tin kết nối tới database PostgreSQL dựa trên cấu hình trong application.yml) và truyền nó vào method này. Bạn không cần phải tự new ConnectionFactory().
        ConnectionFactoryInitializer initializer = new ConnectionFactoryInitializer(); //tạo đối tượng initializer
        initializer.setConnectionFactory(connectionFactory); //Gắn kết nối database (đã được Spring tiêm vào ở trên) cho cái initializer này, để nó biết cần phải chạy script SQL vào đâu.

        Resource schema = new ClassPathResource(SCHEMA_RESOURCE);  // tạo đối tượng để đọc file schema.sql
        if (schema.exists()) { // kiểm tra xem schema có tồn tại ko
            initializer.setDatabasePopulator(new ResourceDatabasePopulator(schema)); // nếu có thì ResourceDatabasePopulator(schema) sẽ đọc nó và đưa vào innitializer.
        }

        return initializer;
    }
}
