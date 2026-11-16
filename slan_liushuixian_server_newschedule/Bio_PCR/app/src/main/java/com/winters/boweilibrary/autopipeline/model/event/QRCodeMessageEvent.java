package com.winters.boweilibrary.autopipeline.model.event;

import com.winters.boweilibrary.autopipeline.model.item.QRCodeItem;

import java.util.ArrayList;
import java.util.List;

public class QRCodeMessageEvent {

    private String QRCodeMessage;

    private List<QRCodeItem> qRCodeItems = new ArrayList<>();

    public String getQRCodeMessage() {
        return QRCodeMessage;
    }

    public void setQRCodeMessage(String QRCodeMessage) {
        this.QRCodeMessage = QRCodeMessage;
    }

    public List<QRCodeItem> getqRCodeItems() {
        return qRCodeItems;
    }

    public void setqRCodeItems(List<QRCodeItem> qRCodeItems) {
        this.qRCodeItems = qRCodeItems;
    }

    public QRCodeMessageEvent(String QRCodeMessage, List<QRCodeItem> qRCodeItems) {
        this.QRCodeMessage = QRCodeMessage;
        this.qRCodeItems = qRCodeItems;
    }
}
