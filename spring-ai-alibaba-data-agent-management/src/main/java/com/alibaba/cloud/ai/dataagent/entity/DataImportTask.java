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
 * 数据导入任务实体类
 * 用于记录Excel/CSV文件导入任务的状态和进度
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class DataImportTask {

    private String id; // 任务ID（UUID）

    private Integer agentId; // 智能体ID

    private String fileName; // 原始文件名

    private String filePath; // 文件存储路径

    private Long fileSize; // 文件大小（字节）

    private String fileType; // 文件类型：xlsx, xls, csv

    private String targetTableName; // 目标表名

    @Builder.Default
    private String status = "pending"; // 状态：pending, processing, completed, failed

    @Builder.Default
    private Integer totalRows = 0; // 总行数

    @Builder.Default
    private Integer processedRows = 0; // 已处理行数

    private String errorMessage; // 错误信息

    private LocalDateTime startTime; // 开始时间

    private LocalDateTime endTime; // 结束时间

    private LocalDateTime createTime; // 创建时间

}
