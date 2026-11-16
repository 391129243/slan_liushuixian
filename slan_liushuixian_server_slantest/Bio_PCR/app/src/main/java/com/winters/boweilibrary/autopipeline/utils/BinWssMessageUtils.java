package com.winters.boweilibrary.autopipeline.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import android.util.Log;

import com.gidi.bio_pcr.WintersUtils.utils.GsonUtil;
import com.winters.boweilibrary.autopipeline.model.item.WSSConnectionItem;
import com.winters.boweilibrary.autopipeline.model.message.BinBaseMessage;
import com.winters.boweilibrary.autopipeline.model.message.PCRMessage.PCRLogMessage;
import com.winters.boweilibrary.autopipeline.model.message.PCRMessage.SLAN_PCRFinishMessage;
import com.winters.boweilibrary.autopipeline.model.message.PCRMessage.SLAN_PCRReqMessage;
import com.winters.boweilibrary.autopipeline.model.message.RNAMessage.RNALogMessage;
import com.winters.boweilibrary.autopipeline.model.message.UpgradeMessage;

import org.json.JSONObject;

public class BinWssMessageUtils {

    /**
     * 获取文件的二进制内容
     * @param path 文件路径
     * @return 文件内容的字节数组，失败返回null
     */
    public static byte[] getFileBytes(String path) {
        File file = new File(path);
        if (!file.exists() || !file.isFile()) return null;
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] data = new byte[(int) file.length()];
            int readLen = fis.read(data);
            if (readLen != file.length()) return null;
            return data;
        } catch (IOException e) {
            Log.e("FileUpgradeUtils", "getFileBytes IOException", e);
            return null;
        }
    }




    /**
     * 计算字节数组的MD5值
     * @param data 输入字节数组
     * @return MD5字符串（小写），失败返回null
     */
    public static String getBytesMD5(byte[] data) {
        if (data == null) return null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            Log.e("FileUpgradeUtils", "getBytesMD5 NoSuchAlgorithmException", e);
            return null;
        }
    }

    public static boolean packetUpgradeMessage(
            WSSConnectionItem wssConnectionItem,
            String upgradeFilePath) {
        try {
            if(wssConnectionItem == null){
                Log.e("FileUpgradeUtils", "Device wssconnection is null");
                return false;
            }

            File upgradeFile = new File(upgradeFilePath);

            if(!upgradeFile.exists()){
                Log.e("FileUpgradeUtils", "Upgrade file not exist" + upgradeFilePath);
                return false;
            }

            String deviceType = wssConnectionItem.getDeviceType();
            if ("PCR".equals(deviceType)) {
                if (!upgradeFilePath.contains("pcr_app")) {
                    Log.e("FileUpgradeUtils", "DeviceType PCR file path not match");
                    return false;
                }
            } else if ("RNA".equals(deviceType)) {
                if (!upgradeFilePath.contains("rna_app")) {
                    Log.e("FileUpgradeUtils", "DeviceType RNA file path not match");
                    return false;
                }
            } else {
                Log.e("FileUpgradeUtils", "Unsupported DeviceType: " + deviceType);
                return false;
            }

            byte[] fileBytes = getFileBytes(upgradeFilePath);

            if(fileBytes == null){
                Log.e("FileUpgradeUtils", "Read upgrade file failed" + upgradeFilePath );
                return false;

            }

            String fileMD5 = getBytesMD5(fileBytes);

            UpgradeMessage upgradeMessage = new UpgradeMessage();
            upgradeMessage.setType("Upgrade");
            upgradeMessage.setDeviceID(wssConnectionItem.getDeviceID());
            upgradeMessage.setDeviceType(wssConnectionItem.getDeviceType());
            upgradeMessage.setPosition(wssConnectionItem.getPosition());
            upgradeMessage.setSeqId(1);
            upgradeMessage.setVersion(upgradeFile.getName());
            upgradeMessage.setCheckSum(fileMD5);
            upgradeMessage.setFileSize(fileBytes.length);
            upgradeMessage.setFileName(upgradeFile.getName());

            byte[] jsonBytes = GsonUtil.toJson(upgradeMessage,false).getBytes();

            byte[] header = intToBigEndian(0x5AA5FCFF);
            byte[] headerlen = intToBigEndian(jsonBytes.length);

            // 拼接 header + headerlen + jsonBytes + fileBytes
            int totalLen = header.length + headerlen.length + jsonBytes.length + fileBytes.length;
            ByteBuffer byteBuffer = ByteBuffer.allocate(totalLen);
            byteBuffer.put(header);
            byteBuffer.put(headerlen);
            byteBuffer.put(jsonBytes);
            byteBuffer.put(fileBytes);
            wssConnectionItem.getWebSocketConnection().send(byteBuffer);
            Log.i("FileUpgradeUtils", "wssConnectionItem send upgrade msg succeed" );
            return true;
        } catch (Exception e) {
            Log.e("FileUpgradeUtils", e.getMessage());
            return false;
        }
    }

    public static ByteBuffer packetBinMessage(BinBaseMessage message,
                                            String filePath) {
        File file = new File(filePath);

        Log.e("packetBinMessage", "file path:" + filePath);

        if(!file.exists()){
            Log.e("packetBinMessage", "file not exist" + filePath);
            return null;
        }

        byte[] fileBytes = getFileBytes(filePath);

        Log.e("packetBinMessage", "fileBytes  " + fileBytes.length );

        if(fileBytes == null){
            Log.e("packetBinMessage", "Read bin file failed" + filePath );
            return null;

        }

        String fileMD5 = getBytesMD5(fileBytes);

        message.setCheckSum(fileMD5);
        message.setFileSize(fileBytes.length);
        message.setFileName(file.getName());

        byte[] jsonBytes = GsonUtil.toJson(message,false).getBytes();

        byte[] header = intToBigEndian(0x5AA5FCFF);
        byte[] headerlen = intToBigEndian(jsonBytes.length);

        // 拼接 header + headerlen + jsonBytes + fileBytes
        int totalLen = header.length + headerlen.length + jsonBytes.length + fileBytes.length;

        Log.e("packetBinMessage", "totalLen" + totalLen );

        ByteBuffer byteBuffer = ByteBuffer.allocate(totalLen);
        byteBuffer.put(header);
        byteBuffer.put(headerlen);
        byteBuffer.put(jsonBytes);
        byteBuffer.put(fileBytes);

        Log.e("packetBinMessage", "byteBuffer " + byteBuffer.capacity() );

        return byteBuffer;

    }

    public static boolean sendBinMessage(
            WSSConnectionItem wssConnectionItem,
            BinBaseMessage message,
            String prjFilePath) {
        try {
            if(wssConnectionItem == null||!wssConnectionItem.getDeviceType().equals("SLANPCR")){
                Log.e("sendBinMessage", "Device wssconnection is null");
                return false;
            }

            ByteBuffer byteBuffer = packetBinMessage(message, prjFilePath);
            wssConnectionItem.getWebSocketConnection().send(byteBuffer);
            Log.i("FileUpgradeUtils", "wssConnectionItem send upgrade msg succeed" );
            return true;
        } catch (Exception e) {
            Log.e("sendBinMessage", e.getMessage());
            return false;
        }
    }

    public static BinBaseMessage parseBinMessage(ByteBuffer byteBuffer){
        try {
            // 1. 读取前4字节，按大端(网络字节序)模式组成header

            if(byteBuffer.remaining()<=4){
                return null;
            }

            byte[] headerBytes = new byte[4];
            byteBuffer.get(headerBytes);
            int header = ((headerBytes[0] & 0xFF) << 24) | ((headerBytes[1] & 0xFF) << 16)
                       | ((headerBytes[2] & 0xFF) << 8) | (headerBytes[3] & 0xFF);
            if (header != 0x5AA5FCFF) {
                Log.e("parseBinMessage", "Header not match: " + Integer.toHexString(header));
                return null;
            }

            // 2. 读取json长度（4字节），按大端(网络字节序)模式组成jsonLen
            byte[] jsonLenBytes = new byte[4];
            byteBuffer.get(jsonLenBytes);
            int jsonLen = ((jsonLenBytes[0] & 0xFF) << 24) | ((jsonLenBytes[1] & 0xFF) << 16)
                        | ((jsonLenBytes[2] & 0xFF) << 8) | (jsonLenBytes[3] & 0xFF);
            if (jsonLen <= 0 || jsonLen > byteBuffer.remaining()) {
                Log.e("parseBinMessage", "Invalid jsonLen: " + jsonLen);
                return null;
            }

            // 3. 读取json字节
            byte[] jsonBytes = new byte[jsonLen];
            byteBuffer.get(jsonBytes);

            String jsonStr = new String(jsonBytes, StandardCharsets.UTF_8);
            JSONObject jsonObj = new JSONObject(jsonStr);

            // 4. 获取文件长度
            int fileLen = jsonObj.optInt("FileSize", -1);
            if (fileLen < 0 || fileLen > byteBuffer.remaining()) {
                Log.e("parseBinMessage", "Invalid fileLen: " + fileLen);
                return null;
            }

            // 5. 读取文件流数据
            byte[] fileBytes = new byte[fileLen];
            byteBuffer.get(fileBytes);

            // 6. 获取CheckSum字段
            String checkSum = jsonObj.optString("CheckSum", null);

            // 7. 计算文件流MD5并比较
            String fileMd5 = getBytesMD5(fileBytes);
            boolean md5Match = checkSum != null && checkSum.equalsIgnoreCase(fileMd5);

            Log.i("parseBinMessage", "parseBinMessage succeed, json: "  + ", fileBytes.length=" + fileBytes.length
                    + ", CheckSum=" + checkSum + ", fileMd5=" + fileMd5 + ", md5Match=" + md5Match);

            // 8. Type
            String Type = jsonObj.optString("Type", null);
            if(md5Match && Type.equals("SLANPCRReq")){

                SLAN_PCRReqMessage sLAN_PCRReqMessage = GsonUtil.fromJson(jsonStr, SLAN_PCRReqMessage.class);
                if (sLAN_PCRReqMessage == null) {
                    Log.e("parseBinMessage", "Failed to parse SLAN_PCRReqMessage json");
                    return null;
                }

                sLAN_PCRReqMessage.setFileData(fileBytes);

                return sLAN_PCRReqMessage;
            }else if(md5Match && Type.equals("SLANPCRFinish")){

                SLAN_PCRFinishMessage sLAN_PCRFinishMessage = GsonUtil.fromJson(jsonStr, SLAN_PCRFinishMessage.class);
                if (sLAN_PCRFinishMessage == null) {
                    Log.e("parseBinMessage", "Failed to parse SLAN_PCRFinishMessage json");
                    return null;
                }

                sLAN_PCRFinishMessage.setFileData(fileBytes);

                return sLAN_PCRFinishMessage;
            }else if(md5Match && Type.equals("PCRLog")){

                PCRLogMessage pCRLogMessage = GsonUtil.fromJson(jsonStr, PCRLogMessage.class);
                if (pCRLogMessage == null) {
                    Log.e("parseBinMessage", "Failed to parse PCRLogMessage json");
                    return null;
                }

                pCRLogMessage.setFileData(fileBytes);

                return pCRLogMessage;
            }else if(md5Match && Type.equals("RNALog")){

                RNALogMessage rNALogMessage = GsonUtil.fromJson(jsonStr, RNALogMessage.class);
                if (rNALogMessage == null) {
                    Log.e("parseBinMessage", "Failed to parse RNALogMessage json");
                    return null;
                }

                rNALogMessage.setFileData(fileBytes);

                return rNALogMessage;
            }else if(md5Match && Type.equals("Upgrade")){

                UpgradeMessage upgradeMessage = GsonUtil.fromJson(jsonStr, UpgradeMessage.class);
                if (upgradeMessage == null) {
                    Log.e("parseBinMessage", "Failed to parse UpgradeMessage json");
                    return null;
                }

                upgradeMessage.setFileData(fileBytes);

                return upgradeMessage;
            }
            return null;

        } catch (Exception e) {
            Log.e("parseBinMessage", "parseBinMessage error", e);
            return null;
        }
    }

    /**
     * int转小端字节数组
     */
    public static byte[] intToLittleEndian(int value) {
        return new byte[] {
            (byte)(value & 0xFF),
            (byte)((value >> 8) & 0xFF),
            (byte)((value >> 16) & 0xFF),
            (byte)((value >> 24) & 0xFF)
        };
    }

    /**
     * long转小端字节数组
     */
    public static byte[] longToLittleEndian(long value) {
        return new byte[] {
            (byte)(value & 0xFF),
            (byte)((value >> 8) & 0xFF),
            (byte)((value >> 16) & 0xFF),
            (byte)((value >> 24) & 0xFF),
            (byte)((value >> 32) & 0xFF),
            (byte)((value >> 40) & 0xFF),
            (byte)((value >> 48) & 0xFF),
            (byte)((value >> 56) & 0xFF)
        };
    }

    /**
     * int转大端（网络字节序）字节数组
     */
    public static byte[] intToBigEndian(int value) {
        return new byte[] {
            (byte)((value >> 24) & 0xFF),
            (byte)((value >> 16) & 0xFF),
            (byte)((value >> 8) & 0xFF),
            (byte)(value & 0xFF)
        };
    }

    /**
     * long转大端（网络字节序）字节数组
     */
    public static byte[] longToBigEndian(long value) {
        return new byte[] {
            (byte)((value >> 56) & 0xFF),
            (byte)((value >> 48) & 0xFF),
            (byte)((value >> 40) & 0xFF),
            (byte)((value >> 32) & 0xFF),
            (byte)((value >> 24) & 0xFF),
            (byte)((value >> 16) & 0xFF),
            (byte)((value >> 8) & 0xFF),
            (byte)(value & 0xFF)
        };
    }

    /**
     * 将二进制数据写入指定文件路径
     * @param path 文件路径
     * @param data 要写入的字节数组
     * @return 写入成功返回true，否则false
     */
    public static boolean writeBytesToFile(String path, byte[] data) {
        if (path == null || data == null) return false;
        File file = new File(path);
        // 判断目录是否存在，不存在则创建
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            if (!parent.mkdirs()) {
                Log.e("BinWssMessageUtils", "Failed to create directory: " + parent.getAbsolutePath());
                return false;
            }
        }
        java.io.FileOutputStream fos = null;
        try {
            fos = new java.io.FileOutputStream(file);
            fos.write(data);
            fos.flush();
            return true;
        } catch (IOException e) {
            Log.e("BinWssMessageUtils", "writeBytesToFile IOException", e);
            return false;
        } finally {
            if (fos != null) {
                try { fos.close(); } catch (IOException ignored) {}
            }
        }
    }

}
