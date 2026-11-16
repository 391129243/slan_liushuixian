package com.gzlab.autoassemblyline_client.autopipeline.message;

import com.gzlab.autoassemblyline_client.autopipeline.message.base.BaseMessage;

public class QueryDeviceMessage extends BaseMessage {

    private String Command;//主控下发设备的命令

    public QueryDeviceMessage() {
        super();
    }

    public String getCommand() {
        return Command;
    }

    public void setCommand(String command) {
        this.Command = command;
    }


}
