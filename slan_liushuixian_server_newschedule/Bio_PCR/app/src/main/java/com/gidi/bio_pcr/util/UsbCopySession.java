package com.gidi.bio_pcr.util;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import me.jahnen.libaums.core.fs.UsbFile;

/**
 * 一次"本地文件复制到 U 盘"的过程。
 *
 * 背景:libaums 只支持 FAT32,且 createFile/createDirectory 在同名项已存在时直接抛异常,
 * 原来的做法是复制前先把 U 盘上的同名项 delete() 掉——没有确认,U 盘里原有的数据会被整个删掉。
 * 这里改为在当前目录下新建一个带时间戳的导出目录,所有内容都写进去,U 盘上已有的内容原样保留。
 *
 * 另外原来的复制过程把异常都吞掉、最后一律返回 true,界面上看起来像复制成功。
 * 这里把失败的文件逐个记下来,调用方据此决定提示成功还是失败。
 */
public class UsbCopySession {

    /** 复制被取消(用户点了取消,或复制过程中 U 盘被拔出) */
    public static final int RESULT_CANCELLED = -1;

    private final UsbHelper usbHelper;
    private final List<File> sources;
    /** 本次导出的目标目录(U 盘当前目录下新建的时间戳目录) */
    private final UsbFile destDir;

    private final List<String> failedFiles = new ArrayList<>();
    private boolean cancelled = false;

    /**
     * @param destParent U 盘上的目标父目录,一般为当前浏览的目录
     * @throws Exception 父目录不可用,或导出目录创建失败
     */
    public UsbCopySession(UsbHelper usbHelper, List<File> sources, UsbFile destParent) throws Exception {
        this.usbHelper = usbHelper;
        this.sources = sources;
        this.destDir = destParent.createDirectory(buildExportDirName(destParent));
    }

    /** 本次导出写入的目录,用于界面提示 */
    public UsbFile getDestDir() {
        return destDir;
    }

    public List<String> getFailedFiles() {
        return failedFiles;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * 执行复制。必须在后台线程调用。
     *
     * @return 成功复制的文件数;{@link #RESULT_CANCELLED} 表示被取消
     */
    public int run(UsbHelper.CopyProgressListener progressListener) {
        int copied = 0;
        for (File source : sources) {
            if (usbHelper.isCopyCancelled()) {
                cancelled = true;
                return RESULT_CANCELLED;
            }
            if (source == null || !source.exists()) {
                failedFiles.add(source == null ? "" : source.getName());
                continue;
            }
            if (source.isDirectory()) {
                copied += copyDirectory(source, destDir, progressListener);
            } else {
                copied += copyOneFile(source, destDir, progressListener);
            }
            if (cancelled) {
                return RESULT_CANCELLED;
            }
        }
        return copied;
    }

    private int copyDirectory(File localDir, UsbFile parent, UsbHelper.CopyProgressListener progressListener) {
        if (usbHelper.isCopyCancelled()) {
            cancelled = true;
            return 0;
        }
        UsbFile usbDir;
        try {
            // 导出目录是本次新建的,正常没有同名项;万一有(比如取消后重试),换个不重名的名字,不删已有的
            usbDir = parent.createDirectory(uniqueName(parent, localDir.getName()));
        } catch (Exception e) {
            // 目录建不出来,里面的文件一个都写不了,全部记为失败
            failedFiles.add(localDir.getAbsolutePath());
            return 0;
        }
        File[] children = localDir.listFiles();
        if (children == null) {
            failedFiles.add(localDir.getAbsolutePath());
            return 0;
        }
        int copied = 0;
        for (File child : children) {
            if (usbHelper.isCopyCancelled()) {
                cancelled = true;
                return copied;
            }
            if (child.isDirectory()) {
                copied += copyDirectory(child, usbDir, progressListener);
            } else {
                copied += copyOneFile(child, usbDir, progressListener);
            }
        }
        return copied;
    }

    /** @return 1 成功,0 失败 */
    private int copyOneFile(File localFile, UsbFile parent, UsbHelper.CopyProgressListener progressListener) {
        UsbFile usbFile = null;
        if (progressListener != null) {
            progressListener.copyStart(localFile.getName());
        }
        try {
            // libaums 在同名项已存在时直接抛异常,且按不区分大小写比较。
            // 这里先找一个不重名的名字,不再像原来那样把 U 盘上的同名文件删掉
            usbFile = parent.createFile(uniqueName(parent, localFile.getName()));
            boolean ok = usbHelper.copyFile(localFile, usbFile, progressListener);
            if (usbHelper.isCopyCancelled()) {
                cancelled = true;
                return 0;
            }
            if (!ok) {
                failedFiles.add(localFile.getAbsolutePath());
                return 0;
            }
            return 1;
        } catch (Exception e) {
            failedFiles.add(localFile.getAbsolutePath());
            // 建出来但没写完的文件删掉,避免 U 盘上留下内容是半截的同名文件
            if (usbFile != null) {
                try {
                    usbFile.delete();
                } catch (Exception ignored) {
                }
            }
            return 0;
        }
    }

    /**
     * 在父目录下找一个不重名的导出目录名。
     * 同一秒内重复导出时追加 _2、_3,避免 createDirectory 因重名抛异常。
     */
    private static String buildExportDirName(UsbFile parent) {
        String base = "PCR_export_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        return uniqueName(parent, base);
    }

    /**
     * 在父目录下找一个不重名的名字。libaums 按不区分大小写判断重名,
     * 已有 result 时再建 Result 会抛异常,所以这里的比较也不区分大小写。
     * 重名时在扩展名前追加 _2、_3。读目录失败时返回原名,交给创建时的异常处理。
     */
    private static String uniqueName(UsbFile parent, String name) {
        List<String> existing = new ArrayList<>();
        try {
            for (UsbFile file : parent.listFiles()) {
                existing.add(file.getName().toLowerCase(Locale.US));
            }
        } catch (Exception e) {
            return name;
        }
        if (!existing.contains(name.toLowerCase(Locale.US))) {
            return name;
        }
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        String ext = dot > 0 ? name.substring(dot) : "";
        int suffix = 2;
        String candidate;
        do {
            candidate = base + "_" + suffix + ext;
            suffix++;
        } while (existing.contains(candidate.toLowerCase(Locale.US)));
        return candidate;
    }
}
