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

/**
 * 文件类型校验工具类
 * 使用Magic Number（文件头字节）检测真实文件类型
 */
@Slf4j
public class FileTypeValidator {

    // Excel 2007+ (.xlsx) - ZIP格式
    private static final byte[] XLSX_MAGIC = { 0x50, 0x4B, 0x03, 0x04 };

    // Excel 97-2003 (.xls) - OLE2格式
    private static final byte[] XLS_MAGIC = { (byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1,
            0x1A, (byte) 0xE1 };

    /**
     * 验证文件类型是否为Excel或CSV
     *
     * @param fileBytes 文件字节数组（至少前8个字节）
     * @param fileName  文件名
     * @return 真实的文件类型（xlsx, xls, csv）
     */
    public static String validateAndDetectType(byte[] fileBytes, String fileName) {
        if (fileBytes == null || fileBytes.length < 4) {
            throw new IllegalArgumentException("文件太小，无法识别类型");
        }

        // 检测XLSX格式（ZIP格式）
        if (matchesMagicNumber(fileBytes, XLSX_MAGIC)) {
            log.debug("检测到XLSX文件格式");
            return "xlsx";
        }

        // 检测XLS格式（OLE2格式）
        if (matchesMagicNumber(fileBytes, XLS_MAGIC)) {
            log.debug("检测到XLS文件格式");
            return "xls";
        }

        // CSV文件没有固定的Magic Number，通过文件扩展名判断
        String extension = getFileExtension(fileName).toLowerCase();
        if ("csv".equals(extension)) {
            log.debug("根据扩展名判断为CSV文件");
            return "csv";
        }

        // 如果都不匹配，抛出异常
        throw new IllegalArgumentException("不支持的文件格式。仅支持Excel (.xlsx, .xls) 和 CSV (.csv) 文件");
    }

    /**
     * 匹配Magic Number
     */
    private static boolean matchesMagicNumber(byte[] header, byte[] magic) {
        if (header.length < magic.length) {
            return false;
        }
        for (int i = 0; i < magic.length; i++) {
            if (header[i] != magic[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * 获取文件扩展名
     */
    private static String getFileExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }

}
