package com.gzlab.autoassemblyline_client.autopipeline.model.message;

import com.gzlab.autoassemblyline_client.autopipeline.message.base.BinBaseMessage;

/**
 * PCR 日志消息
 * 用于上报增量日志到主控
 *
 * 消息格式：
 * {
 *   "Type": "PCRLog",
 *   "SeqId": 1,
 *   "DeviceID": "PCR1111",
 *   "DeviceType": "PCR",
 *   "Position": 1,
 *   "FileName": "2026-09-07/14-30-15.log",
 *   "FileSize": 4096,
 *   "CheckSum": "md5hash",
 *   "fileData": [binary data]
 * }
 */
public class PCRLogMessage extends BinBaseMessage {

    public PCRLogMessage() {
        super();
    }
}
