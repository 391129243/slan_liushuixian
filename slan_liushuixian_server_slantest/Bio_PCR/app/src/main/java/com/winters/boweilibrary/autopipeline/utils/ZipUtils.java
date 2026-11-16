package com.winters.boweilibrary.autopipeline.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * 压缩和解压缩工具类
 * 支持单个文件、多个文件、文件夹的压缩和解压操作
 */
public class ZipUtils {

    private static final int BUFFER_SIZE = 8192;

    /**
     * 压缩单个文件
     *
     * @param srcFilePath  源文件路径
     * @param destZipPath  目标压缩文件路径
     * @return 是否成功
     */
    public static boolean zipFile(String srcFilePath, String destZipPath) {
        return zipFile(new File(srcFilePath), new File(destZipPath));
    }

    /**
     * 压缩单个文件
     *
     * @param srcFile  源文件
     * @param destZip  目标压缩文件
     * @return 是否成功
     */
    public static boolean zipFile(File srcFile, File destZip) {
        if (srcFile == null || !srcFile.exists() || srcFile.isDirectory()) {
            return false;
        }

        ZipOutputStream zos = null;
        FileInputStream fis = null;
        BufferedInputStream bis = null;

        try {
            zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(destZip)));
            fis = new FileInputStream(srcFile);
            bis = new BufferedInputStream(fis, BUFFER_SIZE);

            ZipEntry entry = new ZipEntry(srcFile.getName());
            zos.putNextEntry(entry);

            byte[] buffer = new byte[BUFFER_SIZE];
            int len;
            while ((len = bis.read(buffer)) != -1) {
                zos.write(buffer, 0, len);
            }

            zos.closeEntry();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            closeQuietly(bis);
            closeQuietly(fis);
            closeQuietly(zos);
        }
    }

    /**
     * 压缩多个文件
     *
     * @param srcFiles    源文件数组
     * @param destZipPath 目标压缩文件路径
     * @return 是否成功
     */
    public static boolean zipFiles(File[] srcFiles, String destZipPath) {
        return zipFiles(srcFiles, new File(destZipPath));
    }

    /**
     * 压缩多个文件
     *
     * @param srcFiles 源文件数组
     * @param destZip  目标压缩文件
     * @return 是否成功
     */
    public static boolean zipFiles(File[] srcFiles, File destZip) {
        if (srcFiles == null || srcFiles.length == 0) {
            return false;
        }

        ZipOutputStream zos = null;

        try {
            zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(destZip)));

            for (File srcFile : srcFiles) {
                if (srcFile != null && srcFile.exists()) {
                    addFileToZip(zos, srcFile, "");
                }
            }

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            closeQuietly(zos);
        }
    }

    /**
     * 压缩文件夹
     *
     * @param srcFolderPath 源文件夹路径
     * @param destZipPath   目标压缩文件路径
     * @return 是否成功
     */
    public static boolean zipFolder(String srcFolderPath, String destZipPath) {
        return zipFolder(new File(srcFolderPath), new File(destZipPath));
    }

    /**
     * 压缩文件夹
     *
     * @param srcFolder 源文件夹
     * @param destZip   目标压缩文件
     * @return 是否成功
     */
    public static boolean zipFolder(File srcFolder, File destZip) {
        if (srcFolder == null || !srcFolder.exists() || !srcFolder.isDirectory()) {
            return false;
        }

        ZipOutputStream zos = null;

        try {
            zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(destZip)));
            addFolderToZip(zos, srcFolder, "");
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            closeQuietly(zos);
        }
    }

    /**
     * 解压缩文件到指定目录
     *
     * @param zipFilePath 压缩文件路径
     * @param destDirPath 目标目录路径
     * @return 是否成功
     */
    public static boolean unzip(String zipFilePath, String destDirPath) {
        return unzip(new File(zipFilePath), new File(destDirPath));
    }

    /**
     * 解压缩文件到指定目录
     *
     * @param zipFile 压缩文件
     * @param destDir 目标目录
     * @return 是否成功
     */
    public static boolean unzip(File zipFile, File destDir) {
        if (zipFile == null || !zipFile.exists()) {
            return false;
        }

        if (!destDir.exists()) {
            destDir.mkdirs();
        }

        ZipInputStream zis = null;

        try {
            zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)));
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();
                File entryFile = new File(destDir, entryName);

                // 安全检查：防止 Zip Slip 漏洞
                String canonicalDestDir = destDir.getCanonicalPath();
                String canonicalEntryFile = entryFile.getCanonicalPath();
                if (!canonicalEntryFile.startsWith(canonicalDestDir + File.separator)) {
                    throw new IOException("Entry is outside of the target dir: " + entryName);
                }

                if (entry.isDirectory()) {
                    entryFile.mkdirs();
                } else {
                    // 确保父目录存在
                    File parentDir = entryFile.getParentFile();
                    if (parentDir != null && !parentDir.exists()) {
                        parentDir.mkdirs();
                    }

                    FileOutputStream fos = null;
                    BufferedOutputStream bos = null;

                    try {
                        fos = new FileOutputStream(entryFile);
                        bos = new BufferedOutputStream(fos, BUFFER_SIZE);

                        byte[] buffer = new byte[BUFFER_SIZE];
                        int len;
                        while ((len = zis.read(buffer)) != -1) {
                            bos.write(buffer, 0, len);
                        }
                    } finally {
                        closeQuietly(bos);
                        closeQuietly(fos);
                    }
                }

                zis.closeEntry();
            }

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            closeQuietly(zis);
        }
    }

    /**
     * 使用 ZipFile 方式解压（适合大文件，支持随机访问）
     *
     * @param zipFilePath 压缩文件路径
     * @param destDirPath 目标目录路径
     * @return 是否成功
     */
    public static boolean unzipWithZipFile(String zipFilePath, String destDirPath) {
        return unzipWithZipFile(new File(zipFilePath), new File(destDirPath));
    }

    /**
     * 使用 ZipFile 方式解压（适合大文件，支持随机访问）
     *
     * @param zipFile 压缩文件
     * @param destDir 目标目录
     * @return 是否成功
     */
    public static boolean unzipWithZipFile(File zipFile, File destDir) {
        if (zipFile == null || !zipFile.exists()) {
            return false;
        }

        if (!destDir.exists()) {
            destDir.mkdirs();
        }

        ZipFile zip = null;

        try {
            zip = new ZipFile(zipFile);
            Enumeration<? extends ZipEntry> entries = zip.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String entryName = entry.getName();
                File entryFile = new File(destDir, entryName);

                // 安全检查：防止 Zip Slip 漏洞
                String canonicalDestDir = destDir.getCanonicalPath();
                String canonicalEntryFile = entryFile.getCanonicalPath();
                if (!canonicalEntryFile.startsWith(canonicalDestDir + File.separator)) {
                    throw new IOException("Entry is outside of the target dir: " + entryName);
                }

                if (entry.isDirectory()) {
                    entryFile.mkdirs();
                } else {
                    // 确保父目录存在
                    File parentDir = entryFile.getParentFile();
                    if (parentDir != null && !parentDir.exists()) {
                        parentDir.mkdirs();
                    }

                    InputStream is = null;
                    BufferedInputStream bis = null;
                    FileOutputStream fos = null;
                    BufferedOutputStream bos = null;

                    try {
                        is = zip.getInputStream(entry);
                        bis = new BufferedInputStream(is, BUFFER_SIZE);
                        fos = new FileOutputStream(entryFile);
                        bos = new BufferedOutputStream(fos, BUFFER_SIZE);

                        byte[] buffer = new byte[BUFFER_SIZE];
                        int len;
                        while ((len = bis.read(buffer)) != -1) {
                            bos.write(buffer, 0, len);
                        }
                    } finally {
                        closeQuietly(bos);
                        closeQuietly(fos);
                        closeQuietly(bis);
                        closeQuietly(is);
                    }
                }
            }

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (zip != null) {
                try {
                    zip.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 将文件夹添加到压缩包
     */
    private static void addFolderToZip(ZipOutputStream zos, File folder, String parentPath) throws IOException {
        File[] files = folder.listFiles();
        if (files == null || files.length == 0) {
            // 空文件夹也要添加
            String folderPath = parentPath + folder.getName() + "/";
            ZipEntry entry = new ZipEntry(folderPath);
            zos.putNextEntry(entry);
            zos.closeEntry();
            return;
        }

        for (File file : files) {
            addFileToZip(zos, file, parentPath + folder.getName() + "/");
        }
    }

    /**
     * 将文件添加到压缩包
     */
    private static void addFileToZip(ZipOutputStream zos, File file, String parentPath) throws IOException {
        if (file.isDirectory()) {
            addFolderToZip(zos, file, parentPath);
        } else {
            FileInputStream fis = null;
            BufferedInputStream bis = null;

            try {
                fis = new FileInputStream(file);
                bis = new BufferedInputStream(fis, BUFFER_SIZE);

                ZipEntry entry = new ZipEntry(parentPath + file.getName());
                zos.putNextEntry(entry);

                byte[] buffer = new byte[BUFFER_SIZE];
                int len;
                while ((len = bis.read(buffer)) != -1) {
                    zos.write(buffer, 0, len);
                }

                zos.closeEntry();
            } finally {
                closeQuietly(bis);
                closeQuietly(fis);
            }
        }
    }

    /**
     * 安静地关闭流
     */
    private static void closeQuietly(InputStream is) {
        if (is != null) {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 安静地关闭流
     */
    private static void closeQuietly(OutputStream os) {
        if (os != null) {
            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 获取压缩文件内的文件列表
     *
     * @param zipFilePath 压缩文件路径
     * @return 文件名列表，失败返回null
     */
    public static String[] getZipFileList(String zipFilePath) {
        return getZipFileList(new File(zipFilePath));
    }

    /**
     * 获取压缩文件内的文件列表
     *
     * @param zipFile 压缩文件
     * @return 文件名列表，失败返回null
     */
    public static String[] getZipFileList(File zipFile) {
        if (zipFile == null || !zipFile.exists()) {
            return null;
        }

        ZipFile zip = null;

        try {
            zip = new ZipFile(zipFile);
            Enumeration<? extends ZipEntry> entries = zip.entries();
            java.util.List<String> fileNames = new java.util.ArrayList<>();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                fileNames.add(entry.getName());
            }

            return fileNames.toArray(new String[0]);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (zip != null) {
                try {
                    zip.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 检查是否是有效的压缩文件
     *
     * @param zipFilePath 压缩文件路径
     * @return 是否有效
     */
    public static boolean isValidZipFile(String zipFilePath) {
        return isValidZipFile(new File(zipFilePath));
    }

    /**
     * 检查是否是有效的压缩文件
     *
     * @param zipFile 压缩文件
     * @return 是否有效
     */
    public static boolean isValidZipFile(File zipFile) {
        if (zipFile == null || !zipFile.exists()) {
            return false;
        }

        ZipFile zip = null;

        try {
            zip = new ZipFile(zipFile);
            return true;
        } catch (IOException e) {
            return false;
        } finally {
            if (zip != null) {
                try {
                    zip.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
