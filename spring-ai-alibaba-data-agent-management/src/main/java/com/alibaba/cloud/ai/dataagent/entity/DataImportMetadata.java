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
package com.alibaba.cloud.ai.dataagent.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 数据导入元数据实体类
 * 用于记录导入数据的表结构信息
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class DataImportMetadata {

    private Integer id; // 主键ID

    private String taskId; // 关联的任务ID

    private String tableName; // 物理表名（唯一，用于CREATE TABLE）

    private String logicalTableName; // 逻辑表名（显示名称，来自文件名）

    private Integer columnCount; // 列数

    private Integer rowCount; // 行数

    private String datasourceType; // 数据库类型：mysql, postgresql, h2

    private Integer datasourceId; // 关联的虚拟数据源ID

    private String tableDescription; // 表的业务描述

    private String columnDescriptions; // 列描述JSON: {"column1": "描述1", "column2": "描述2"}

    private LocalDateTime createTime; // 创建时间

}
