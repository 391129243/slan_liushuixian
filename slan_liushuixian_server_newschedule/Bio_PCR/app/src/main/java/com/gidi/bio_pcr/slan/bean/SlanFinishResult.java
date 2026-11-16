package com.gidi.bio_pcr.slan.bean;


import com.winters.boweilibrary.autopipeline.model.message.PCRMessage.SLAN_PCRFinishMessage;

public class SlanFinishResult {
    String filePath;

    SLAN_PCRFinishMessage slan_pcrFinishMessage;

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public SLAN_PCRFinishMessage getSlan_pcrFinishMessage() {
        return slan_pcrFinishMessage;
    }

    public void setSlan_pcrFinishMessage(SLAN_PCRFinishMessage slan_pcrFinishMessage) {
        this.slan_pcrFinishMessage = slan_pcrFinishMessage;
    }
}
