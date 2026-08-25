package com.shopzone.product_service.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;

@Configuration
public class MongoConfig extends AbstractMongoClientConfiguration {

    @Override
    protected String getDatabaseName() {
        return "shopzone";
    }

    @Override
    @Bean
    public MongoClient mongoClient() {
        // Hardcoded connection string ensures Spring Boot MUST connect to Atlas
        String connectionString = "mongodb+srv://fahadhassan2131_db_user:fahad2131@shopzone.l7arjgb.mongodb.net/shopzone?retryWrites=true&w=majority&appName=shopzone";
        return MongoClients.create(connectionString);
    }
}