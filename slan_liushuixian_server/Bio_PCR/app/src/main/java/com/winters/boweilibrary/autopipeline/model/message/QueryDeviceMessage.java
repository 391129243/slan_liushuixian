package com.winters.boweilibrary.autopipeline.model.message;

public class QueryDeviceMessage extends BaseMessage {

    private String Command;

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
