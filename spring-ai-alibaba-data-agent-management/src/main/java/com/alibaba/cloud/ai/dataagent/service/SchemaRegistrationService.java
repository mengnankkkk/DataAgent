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
package com.alibaba.cloud.ai.dataagent.service;

import com.alibaba.cloud.ai.dataagent.constant.DocumentMetadataConstant;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Schema注册服务
 * 负责将导入的表和列信息注册到向量库
 */
@Slf4j
@Service
@AllArgsConstructor
public class SchemaRegistrationService {

    /**
     * 注册表的Schema到向量库
     * 
     * @param agentId            智能体ID
     * @param datasourceId       数据源ID
     * @param tableName          表名
     * @param columnNames        列名列表
     * @param tableDescription   表描述（可选）
     * @param columnDescriptions 列描述Map（可选）
     * @return 创建的文档列表
     */
    public List<Document> createSchemaDocuments(String agentId, Integer datasourceId,
            String tableName, List<String> columnNames,
            String tableDescription,
            Map<String, String> columnDescriptions) {
        List<Document> documents = new ArrayList<>();

        // 创建表文档
        Document tableDoc = createTableDocument(agentId, datasourceId, tableName, tableDescription);
        documents.add(tableDoc);

        // 创建列文档
        List<Document> columnDocs = columnNames.stream()
                .map(col -> createColumnDocument(agentId, datasourceId, tableName, col,
                        columnDescriptions != null ? columnDescriptions.get(col) : null))
                .collect(Collectors.toList());
        documents.addAll(columnDocs);

        log.info("Created {} schema documents for table: {} (1 table + {} columns)",
                documents.size(), tableName, columnNames.size());

        return documents;
    }

    /**
     * 创建表文档
     */
    private Document createTableDocument(String agentId, Integer datasourceId,
            String tableName, String description) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("agentId", agentId);
        metadata.put("datasourceId", datasourceId);
        metadata.put("tableName", tableName);
        metadata.put("vectorType", DocumentMetadataConstant.TABLE);
        metadata.put("description", description != null ? description : "");
        metadata.put("source", "excel_import"); // 标记来源

        // 文档内容：表名 + 描述
        String content = "表名: " + tableName;
        if (description != null && !description.isEmpty()) {
            content += "\n描述: " + description;
        }

        return new Document(content, metadata);
    }

    /**
     * 创建列文档
     */
    private Document createColumnDocument(String agentId, Integer datasourceId,
            String tableName, String columnName, String description) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("agentId", agentId);
        metadata.put("datasourceId", datasourceId);
        metadata.put("tableName", tableName);
        metadata.put("name", columnName);
        metadata.put("vectorType", DocumentMetadataConstant.COLUMN);
        metadata.put("description", description != null ? description : "");
        metadata.put("source", "excel_import"); // 标记来源

        // 文档内容：表名.列名 + 描述
        String content = "表: " + tableName + ", 列: " + columnName;
        if (description != null && !description.isEmpty()) {
            content += "\n描述: " + description;
        }

        return new Document(content, metadata);
    }
}
