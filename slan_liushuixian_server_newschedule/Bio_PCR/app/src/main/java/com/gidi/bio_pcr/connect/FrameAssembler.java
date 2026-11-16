package com.gidi.bio_pcr.connect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 串口帧组装：按字节流切出完整帧。
 *
 * 帧格式：5A A5 | LenH LenL | Type | 载荷(Len 字节) | CRC(2) | FF FC FF。
 * 长度字段只含载荷，CRC 也只覆盖载荷，与各消息类的 encodeBytes 一致。
 * 串口一次读到的数据可能是半包或粘包，这里按长度字段切分，半包留到下一次继续拼。
 *
 * 不依赖 Android，可直接做单元测试。
 */
public class FrameAssembler {

    /** 一帧最少字节数：帧头2 + 长度2 + 类型1 + CRC2 + 帧尾3 */
    static final int FRAME_OVERHEAD = 10;

    /** 载荷长度上限，防止长度字段损坏时一直等待 */
    static final int MAX_PAYLOAD = 4096;

    private static final byte[] TAIL = new byte[]{(byte) 0xFF, (byte) 0xFC, (byte) 0xFF};

    private final byte[] buf;
    private int len = 0;

    public FrameAssembler() {
        this(8192);
    }

    public FrameAssembler(int capacity) {
        buf = new byte[capacity];
    }

    /** 缓冲区里尚未成帧的字节数，用于排查数据堆积 */
    public int pending() {
        return len;
    }

    public void reset() {
        len = 0;
    }

    /**
     * 喂入新收到的字节，返回本次可以切出的所有完整帧。
     * 坏帧只跳过一个字节重新同步，不影响后面的帧。
     */
    public List<byte[]> feed(byte[] in) {
        List<byte[]> frames = new ArrayList<>();
        if (in == null || in.length == 0) {
            return frames;
        }
        if (len + in.length > buf.length) {
            // 缓冲区溢出：说明一直没同步上，丢弃旧数据重新找帧头
            len = 0;
        }
        int copy = Math.min(in.length, buf.length - len);
        System.arraycopy(in, 0, buf, len, copy);
        len += copy;

        int pos = 0;
        while (len - pos >= FRAME_OVERHEAD) {
            if ((buf[pos] & 0xFF) != 0x5A || (buf[pos + 1] & 0xFF) != 0xA5) {
                pos++;
                continue;
            }
            int payloadLen = ((buf[pos + 2] & 0xFF) << 8) | (buf[pos + 3] & 0xFF);
            if (payloadLen > MAX_PAYLOAD) {
                pos++;
                continue;
            }
            int frameLen = payloadLen + FRAME_OVERHEAD;
            if (len - pos < frameLen) {
                break; // 半包，等下一批数据
            }
            int end = pos + frameLen;
            if (!tailOk(buf, end) || !crcOk(buf, pos, payloadLen)) {
                pos++; // 坏帧：跳 1 字节重新找帧头
                continue;
            }
            frames.add(Arrays.copyOfRange(buf, pos, end));
            pos = end;
        }
        // 剩下的数据里没有帧头：只保留末尾 1 个字节（可能是下一帧 5A 的前半），其余杂数据丢掉，
        // 否则没有帧头的噪声会一直堆积，每次扫描都越来越慢。已找到帧头的半包必须保留。
        boolean hasHead = false;
        for (int i = pos; i < len - 1; i++) {
            if ((buf[i] & 0xFF) == 0x5A && (buf[i + 1] & 0xFF) == 0xA5) {
                hasHead = true;
                break;
            }
        }
        if (!hasHead && pos < len - 1 && (buf[len - 1] & 0xFF) != 0x5A) {
            pos = len;
        } else if (!hasHead && pos < len - 1) {
            pos = len - 1;
        }
        if (pos > 0) {
            System.arraycopy(buf, pos, buf, 0, len - pos);
            len -= pos;
        }
        return frames;
    }

    private static boolean tailOk(byte[] data, int end) {
        return data[end - 3] == TAIL[0] && data[end - 2] == TAIL[1] && data[end - 1] == TAIL[2];
    }

    /** CRC 只覆盖载荷，不含类型字节，与各消息类的 getCRCBytes 一致 */
    static boolean crcOk(byte[] data, int start, int payloadLen) {
        int sum = 0;
        for (int i = 0; i < payloadLen; i++) {
            sum += data[start + 5 + i] & 0xFF;
        }
        int crcPos = start + 5 + payloadLen;
        int crc = ((data[crcPos] & 0xFF) << 8) | (data[crcPos + 1] & 0xFF);
        return (sum & 0xFFFF) == crc;
    }
}
