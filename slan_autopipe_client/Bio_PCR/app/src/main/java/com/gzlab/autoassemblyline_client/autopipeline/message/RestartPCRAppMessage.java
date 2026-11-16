package com.gzlab.autoassemblyline_client.autopipeline.message;

import com.gzlab.autoassemblyline_client.autopipeline.message.base.BaseMessage;

/**
 * 主控下发：重启本 App（释放 slansdk 泄露的内存）
 */
public class RestartPCRAppMessage extends BaseMessage {

    private String Command;

    // 0：实验运行中拒绝重启；1：强制重启（先停止正在运行的实验）
    private int Force;

    public RestartPCRAppMessage() {
        super();
    }

    public String getCommand() {
        return Command;
    }

    public void setCommand(String command) {
        this.Command = command;
    }

    public int getForce() {
        return Force;
    }

    public void setForce(int force) {
        this.Force = force;
    }
}
