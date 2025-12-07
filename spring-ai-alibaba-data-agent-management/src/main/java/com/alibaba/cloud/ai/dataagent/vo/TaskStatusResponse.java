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
package com.alibaba.cloud.ai.dataagent.vo;

import com.alibaba.cloud.ai.dataagent.entity.DataImportTask;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 任务状态响应实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskStatusResponse {

    private String taskId;

    private String fileName;

    private String fileType;

    private String tableName;

    private String status;

    private Integer totalRows;

    private Integer processedRows;

    private Integer progress; // 进度百分比 0-100

    private String errorMessage;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private LocalDateTime createTime;

    /**
     * 从DataImportTask实体转换为响应对象
     */
    public static TaskStatusResponse fromEntity(DataImportTask task) {
        int progress = 0;
        if (task.getTotalRows() != null && task.getTotalRows() > 0) {
            progress = (int) ((task.getProcessedRows() * 100.0) / task.getTotalRows());
        }

        return TaskStatusResponse.builder().taskId(task.getId()).fileName(task.getFileName())
                .fileType(task.getFileType()).tableName(task.getTargetTableName()).status(task.getStatus())
                .totalRows(task.getTotalRows()).processedRows(task.getProcessedRows()).progress(progress)
                .errorMessage(task.getErrorMessage()).startTime(task.getStartTime()).endTime(task.getEndTime())
                .createTime(task.getCreateTime()).build();
    }

}
