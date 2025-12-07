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

import com.alibaba.cloud.ai.dataagent.vo.ImportProgressMessage;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * 导入进度推送服务
 */
@Slf4j
@Service
@AllArgsConstructor
public class ImportProgressService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 推送进度消息
     *
     * @param taskId  任务ID
     * @param message 进度消息
     */
    public void sendProgress(String taskId, ImportProgressMessage message) {
        try {
            String destination = "/topic/import-progress/" + taskId;
            messagingTemplate.convertAndSend(destination, message);
            log.debug("发送进度消息到 {}: {}", destination, message);
        } catch (Exception e) {
            log.error("发送进度消息失败: taskId=" + taskId, e);
        }
    }

    /**
     * 发送解析中状态
     */
    public void sendParsing(String taskId, String message) {
        ImportProgressMessage msg = ImportProgressMessage.builder().taskId(taskId).status("parsing").progress(10)
                .message(message).build();
        sendProgress(taskId, msg);
    }

    /**
     * 发送建表中状态
     */
    public void sendCreatingTable(String taskId, String tableName) {
        ImportProgressMessage msg = ImportProgressMessage.builder().taskId(taskId).status("creating_table")
                .progress(20).message("正在创建表: " + tableName).build();
        sendProgress(taskId, msg);
    }

    /**
     * 发送插入数据进度
     */
    public void sendInserting(String taskId, int totalRows, int processedRows) {
        int progress = 20 + (int) ((processedRows * 70.0) / totalRows); // 20-90%
        ImportProgressMessage msg = ImportProgressMessage.builder().taskId(taskId).status("inserting")
                .totalRows(totalRows).processedRows(processedRows).progress(progress)
                .message(String.format("正在导入数据: %d/%d", processedRows, totalRows)).build();
        sendProgress(taskId, msg);
    }

    /**
     * 发送完成状态
     */
    public void sendCompleted(String taskId, int totalRows, String tableName) {
        ImportProgressMessage msg = ImportProgressMessage.builder().taskId(taskId).status("completed")
                .totalRows(totalRows).processedRows(totalRows).progress(100)
                .message("导入完成，共 " + totalRows + " 行数据，表名: " + tableName).build();
        sendProgress(taskId, msg);
    }

    /**
     * 发送失败状态
     */
    public void sendFailed(String taskId, String errorMessage) {
        ImportProgressMessage msg = ImportProgressMessage.builder().taskId(taskId).status("failed").progress(0)
                .message("导入失败").errorMessage(errorMessage).build();
        sendProgress(taskId, msg);
    }

}
