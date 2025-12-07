/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.dataagent.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

/**
 * 数据库类型检测工具
 * 用于动态获取当前应用连接的数据库类型
 */
@Slf4j
@Component
public class DatabaseTypeDetector {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Environment environment;

    /**
     * 获取当前数据库类型
     * 
     * @return 数据库类型：mysql, postgresql, h2等
     */
    public String getDatabaseType() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String databaseProductName = metaData.getDatabaseProductName().toLowerCase();

            if (databaseProductName.contains("mysql")) {
                return "mysql";
            } else if (databaseProductName.contains("postgresql")) {
                return "postgresql";
            } else if (databaseProductName.contains("h2")) {
                return "h2";
            } else if (databaseProductName.contains("dameng")) {
                return "dameng";
            } else {
                log.warn("Unknown database type: {}, defaulting to mysql", databaseProductName);
                return "mysql";
            }
        } catch (Exception e) {
            log.error("Failed to detect database type", e);
            // Fallback: 从配置中获取
            String url = environment.getProperty("spring.datasource.url", "");
            if (url.contains("mysql")) {
                return "mysql";
            } else if (url.contains("postgresql")) {
                return "postgresql";
            } else if (url.contains("h2")) {
                return "h2";
            }
            return "mysql"; // 默认
        }
    }

    /**
     * 获取当前数据库名称
     */
    public String getDatabaseName() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.getCatalog();
        } catch (Exception e) {
            log.error("Failed to get database name", e);
            return "unknown";
        }
    }

    /**
     * 获取当前连接URL
     */
    public String getConnectionUrl() {
        return environment.getProperty("spring.datasource.url", "");
    }

    /**
     * 获取当前数据库用户名
     */
    public String getUsername() {
        return environment.getProperty("spring.datasource.username", "");
    }

    /**
     * 获取当前数据库密码
     */
    public String getPassword() {
        return environment.getProperty("spring.datasource.password", "");
    }
}
