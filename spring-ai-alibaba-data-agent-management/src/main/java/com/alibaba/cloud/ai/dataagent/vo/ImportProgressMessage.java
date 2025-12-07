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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 导入进度消息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportProgressMessage {

    private String taskId;

    private String status; // parsing, creating_table, inserting, completed, failed

    private Integer totalRows;

    private Integer processedRows;

    private Integer progress; // 0-100

    private String message;

    private String errorMessage;

}
