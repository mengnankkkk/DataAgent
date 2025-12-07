/*
 * Copyright 2025 the original author or authors.
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

import axios from 'axios';

const API_BASE_URL = '/api/data-import';

export interface DataImportResponse {
    success: boolean;
    message: string;
    taskId: string;
    fileName: string;
}

export interface TaskStatusResponse {
    taskId: string;
    fileName: string;
    fileType: string;
    tableName: string;
    status: string;
    totalRows: number;
    processedRows: number;
    progress: number;
    errorMessage?: string;
    startTime?: string;
    endTime?: string;
    createTime: string;
}

export interface ImportProgressMessage {
    taskId: string;
    status: string;
    totalRows?: number;
    processedRows?: number;
    progress: number;
    message: string;
    errorMessage?: string;
}

class DataImportService {
    /**
     * 上传Excel/CSV文件
     */
    async uploadFile(agentId: number, file: File): Promise<DataImportResponse> {
        const formData = new FormData();
        formData.append('agentId', agentId.toString());
        formData.append('file', file);

        const response = await axios.post(`${API_BASE_URL}/upload`, formData, {
            headers: {
                'Content-Type': 'multipart/form-data',
            },
        });

        return response.data;
    }

    /**
     * 查询任务状态
     */
    async getTaskStatus(taskId: string): Promise<TaskStatusResponse> {
        const response = await axios.get(`${API_BASE_URL}/task/${taskId}`);
        return response.data;
    }

    /**
     * 查询智能体的所有导入任务
     */
    async getTasksByAgentId(agentId: number): Promise<TaskStatusResponse[]> {
        const response = await axios.get(`${API_BASE_URL}/tasks`, {
            params: { agentId },
        });
        return response.data;
    }
}

export default new DataImportService();
