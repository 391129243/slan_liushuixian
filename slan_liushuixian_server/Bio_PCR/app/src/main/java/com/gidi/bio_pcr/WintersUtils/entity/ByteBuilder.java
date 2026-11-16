package com.gidi.bio_pcr.WintersUtils.entity;

public interface ByteBuilder {

    /**
     * 数据包编码成byte数组
     *
     * @return
     */
    public abstract int getMsdID();


    /**
     * 反馈Packet的ID
     *
     * @return
     */
    public abstract byte[] encodeBytes();

    /**
     * 从byte数组解码数据包
     *
     * @param rawData 原始byte数组
     * @return 解码包
     */
    public abstract ByteBuilder decodeBytes(byte[] rawData);

}
