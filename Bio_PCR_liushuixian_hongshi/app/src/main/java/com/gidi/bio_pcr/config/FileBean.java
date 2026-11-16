package com.gidi.bio_pcr.config;

import java.io.File;

public class FileBean implements Comparable<FileBean>{
    private File file;
    boolean isCheck = false;
    boolean isShow = false;


    public FileBean(File file, boolean isCheck, boolean isShow) {
        this.file = file;
        this.isCheck = isCheck;
        this.isShow = isShow;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
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

    @Override
    public int compareTo(FileBean o) {
//        if(this.file.getName().compareTo(o.getFile().getName())>0){
//            return 1;
//        }
//        if(this.file.getName().compareTo(o.getFile().getName())<0){
//            return -1;
//        }
//        return 0;
        if(this.file.isDirectory() && !o.getFile().isDirectory()){
            return -1;
        }else if(!this.file.isDirectory() && o.getFile().isDirectory()){
            return 1;
        }else{
//            long lastModified1 = this.file.lastModified();
//            long lastModified2 = o.getFile().lastModified();
//            return Long.compare(lastModified2, lastModified1);
            return o.getFile().getName().compareTo(this.file.getName());
        }
    }
}
