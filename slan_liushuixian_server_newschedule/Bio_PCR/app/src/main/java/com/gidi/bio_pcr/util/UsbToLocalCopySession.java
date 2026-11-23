package com.gidi.bio_pcr.util;

import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import me.jahnen.libaums.core.fs.UsbFile;

/**
 * 一次"U 盘文件复制到本地"的过程。
 *
 * 1. 本地已有同名文件时覆盖。先写到同目录的临时文件,写完再替换原文件;
 *    复制失败或取消时只删临时文件,本地原文件保持不变。
 *    同名目录直接合并,目录里的同名文件逐个覆盖。
 * 2. 递归时直接用 UsbFile.listFiles(),不调用 UsbHelper.getUsbFolderFileList(),
 *    后者会改掉 UsbHelper 的当前目录,导致之后的导出、新建写到别的目录。
 * 3. 结果只在结束时汇总一次。
 * 4. 每个文件之间、以及写入循环里都检查 UsbHelper 的取消标记。
 */
public class UsbToLocalCopySession {

    private static final String TAG = "UsbToLocalCopySession";
    /** 临时文件后缀。以点开头的隐藏名,写完即改名,正常情况下不会留在本地 */
    private static final String TMP_SUFFIX = ".usbtmp";

    private final UsbHelper usbHelper;
    private final List<UsbFile> sources;
    /** 本地目标目录,一般为当前浏览的本地目录 */
    private final File destDir;

    private final List<String> failedFiles = new ArrayList<>();
    private int copiedCount = 0;
    private boolean cancelled = false;

    public UsbToLocalCopySession(UsbHelper usbHelper, List<UsbFile> sources, File destDir) {
        this.usbHelper = usbHelper;
        this.sources = sources;
        this.destDir = destDir;
    }

    public File getDestDir() {
        return destDir;
    }

    public List<String> getFailedFiles() {
        return failedFiles;
    }

    public int getCopiedCount() {
        return copiedCount;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    /** 执行复制。必须在后台线程调用。 */
    public void run(UsbHelper.CopyProgressListener progressListener) {
        for (UsbFile source : sources) {
            if (checkCancelled()) {
                return;
            }
            if (source == null) {
                continue;
            }
            if (source.isDirectory()) {
                copyDirectory(source, new File(destDir, source.getName()), progressListener);
            } else {
                copyOneFile(source, destDir, progressListener);
            }
            if (cancelled) {
                return;
            }
        }
    }

    private boolean checkCancelled() {
        if (usbHelper.isCopyCancelled()) {
            cancelled = true;
        }
        return cancelled;
    }

    /** 本地已有同名目录时直接合并进去 */
    private void copyDirectory(UsbFile usbDir, File localDir, UsbHelper.CopyProgressListener progressListener) {
        if (checkCancelled()) {
            return;
        }
        if (localDir.exists() && !localDir.isDirectory()) {
            //本地同名的是文件而不是目录:不删这个文件,记为失败
            failedFiles.add(usbDir.getName());
            return;
        }
        if (!localDir.isDirectory() && !localDir.mkdirs()) {
            failedFiles.add(usbDir.getName());
            return;
        }
        UsbFile[] children;
        try {
            children = usbDir.listFiles();
        } catch (Exception e) {
            //目录读不出来,里面的文件一个都复制不了
            failedFiles.add(usbDir.getName());
            return;
        }
        for (UsbFile child : children) {
            if (checkCancelled()) {
                return;
            }
            if (child.isDirectory()) {
                copyDirectory(child, new File(localDir, child.getName()), progressListener);
            } else {
                copyOneFile(child, localDir, progressListener);
            }
        }
    }

    /**
     * 复制一个文件,本地已有同名文件时覆盖。
     * 先写临时文件,成功后再替换,失败或取消时原文件不受影响。
     */
    private void copyOneFile(UsbFile usbFile, File localDir, UsbHelper.CopyProgressListener progressListener) {
        String name = usbFile.getName();
        File target = new File(localDir, name);
        if (target.isDirectory()) {
            //本地同名的是目录而不是文件:不删这个目录,记为失败
            failedFiles.add(name);
            return;
        }
        if (progressListener != null) {
            progressListener.copyStart(name);
        }
        String tmpName = "." + name + TMP_SUFFIX;
        File tmp = new File(localDir, tmpName);
        if (tmp.exists()) {
            //上次异常退出留下的临时文件
            tmp.delete();
        }
        //saveUSbFileToLocal 失败或取消时会删掉 tmp,不会碰 target
        boolean ok = usbHelper.saveUSbFileToLocal(usbFile, localDir.getAbsolutePath(), tmpName, progressListener);
        if (ok) {
            if (replace(tmp, target)) {
                copiedCount++;
            } else {
                tmp.delete();
                failedFiles.add(name);
            }
        } else if (!usbHelper.isCopyCancelled()) {
            failedFiles.add(name);
        }
        checkCancelled();
    }

    /**
     * 用 tmp 替换 target。
     * Linux 上 renameTo 会直接覆盖已有文件;个别存储不支持时,先把原文件改名备份,
     * 替换成功再删备份,替换失败把备份改回去,保证原文件不丢。
     */
    private static boolean replace(File tmp, File target) {
        if (tmp.renameTo(target)) {
            return true;
        }
        if (!target.exists()) {
            return false;
        }
        File backup = new File(target.getParentFile(), "." + target.getName() + ".usbbak");
        if (backup.exists()) {
            backup.delete();
        }
        if (!target.renameTo(backup)) {
            Log.e(TAG, "cannot move old file aside: " + target);
            return false;
        }
        if (tmp.renameTo(target)) {
            backup.delete();
            return true;
        }
        Log.e(TAG, "cannot replace file, restoring: " + target);
        backup.renameTo(target);
        return false;
    }

    /**
     * 待复制内容在 U 盘上的总字节数,用于复制前和本地剩余空间比较。
     * 有目录读不出来时返回 -1。
     * 覆盖时原文件要等新文件写完才删除,所以按全部写入计算,不扣除被覆盖文件的大小。
     */
    public static long totalSizeOf(List<UsbFile> files) {
        long total = 0;
        for (UsbFile file : files) {
            long size = sizeOf(file);
            if (size < 0) {
                return -1;
            }
            total += size;
        }
        return total;
    }

    private static long sizeOf(UsbFile file) {
        if (file == null) {
            return 0;
        }
        if (!file.isDirectory()) {
            return file.getLength();
        }
        try {
            long total = 0;
            for (UsbFile child : file.listFiles()) {
                long size = sizeOf(child);
                if (size < 0) {
                    return -1;
                }
                total += size;
            }
            return total;
        } catch (Exception e) {
            return -1;
        }
    }
}
