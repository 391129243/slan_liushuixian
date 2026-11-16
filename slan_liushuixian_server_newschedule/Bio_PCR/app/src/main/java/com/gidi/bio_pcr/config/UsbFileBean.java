package com.gidi.bio_pcr.config;


import me.jahnen.libaums.core.fs.UsbFile;

public class UsbFileBean {
    private UsbFile usbFile;
    private boolean isCheck;

    private boolean isShow;

    public UsbFileBean(UsbFile usbFile, boolean isCheck,boolean isShow) {
        this.usbFile = usbFile;
        this.isCheck = isCheck;
        this.isShow = isShow;
    }

    public UsbFile getUsbFile() {
        return usbFile;
    }

    public void setUsbFile(UsbFile usbFile) {
        this.usbFile = usbFile;
    }

    public boolean isCheck() {
        return isCheck;
    }

    public void setCheck(boolean check) {
        isCheck = check;
    }

    public boolean isShow() {
        return isShow;
    }

    public void setShow(boolean show) {
        isShow = show;
    }
}
