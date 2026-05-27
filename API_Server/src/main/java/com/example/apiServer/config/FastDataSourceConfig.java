package com.example.apiServer.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import com.zaxxer.hikari.HikariDataSource;

@Configuration
public class FastDataSourceConfig {

    @Bean(name = "fastDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.fast")
    public HikariDataSource fastDataSource() {
        return new HikariDataSource();
    }

    @Bean(name = "fastJdbcTemplate")
    public JdbcTemplate fastJdbcTemplate(
            @Qualifier("fastDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}