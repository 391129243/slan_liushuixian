package com.gidi.bio_pcr.connect;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * 对照验证：新的 FrameAssembler 与原来的字符串切帧（Connection.onReceiveMessage 的旧实现）。
 * 旧实现按原样搬过来，用来证明它在哪些情况下会丢帧。
 */
public class FrameAssemblerTest {

    private static byte[] frame(byte type, byte[] payload) {
        // 长度字段和 CRC 都只覆盖载荷，不含类型字节，与 RNARunningMessage.encodeBytes 一致
        int len = payload.length;
        byte[] f = new byte[len + 10];
        f[0] = 0x5A;
        f[1] = (byte) 0xA5;
        f[2] = (byte) ((len >> 8) & 0xFF);
        f[3] = (byte) (len & 0xFF);
        f[4] = type;
        System.arraycopy(payload, 0, f, 5, payload.length);
        int sum = 0;
        for (byte b : payload) sum += b & 0xFF;
        f[5 + payload.length] = (byte) ((sum >> 8) & 0xFF);
        f[6 + payload.length] = (byte) (sum & 0xFF);
        f[f.length - 3] = (byte) 0xFF;
        f[f.length - 2] = (byte) 0xFC;
        f[f.length - 1] = (byte) 0xFF;
        return f;
    }

    private static byte[] concat(byte[]... parts) {
        int n = 0;
        for (byte[] p : parts) n += p.length;
        byte[] out = new byte[n];
        int pos = 0;
        for (byte[] p : parts) {
            System.arraycopy(p, 0, out, pos, p.length);
            pos += p.length;
        }
        return out;
    }

    /** 原来的切帧逻辑，原样搬过来 */
    static class LegacyAssembler {
        private final StringBuilder sb = new StringBuilder();

        List<byte[]> feed(byte[] bytes) {
            sb.append(hex(bytes));
            List<String> cmdList = new ArrayList<>();
            while (true) {
                String processData = sb.toString().toUpperCase();
                if (processData.contains("5A A5") && processData.contains("FF FC FF")) {
                    int indexHead = processData.indexOf("5A A5");
                    int indexTail = processData.indexOf("FF FC FF");
                    if (indexTail > indexHead) {
                        cmdList.add(processData.substring(indexHead, indexTail - indexHead + 8));
                        sb.delete(0, indexTail + 8);
                    } else {
                        sb.delete(0, indexTail + 8);
                    }
                } else if (processData.contains("5A A5")) {
                    sb.delete(0, processData.indexOf("5A A5"));
                    break;
                } else {
                    break; // 原实现的 "drop" 分支：什么都没删
                }
            }
            List<byte[]> frames = new ArrayList<>();
            for (String msg : cmdList) frames.add(parse(msg));
            return frames;
        }

        private static String hex(byte[] list) {
            String result = "";
            for (byte num : list) {
                String s = Integer.toHexString(0xFF & num);
                if (s.length() == 1) s = "0" + s;
                result = result + s.toUpperCase() + " ";
            }
            return result;
        }

        private static byte[] parse(String hex) {
            String[] list = hex.split("[ ]+");
            byte[] out = new byte[list.length];
            for (int i = 0; i < list.length; i++) out[i] = (byte) Integer.parseInt(list[i], 16);
            return out;
        }
    }

    @Test
    public void 连续两帧都能切出() {
        byte[] f1 = frame((byte) 0xA1, "{\"a\":1}".getBytes());
        byte[] f2 = frame((byte) 0xA2, "{\"b\":22}".getBytes());

        List<byte[]> got = new FrameAssembler().feed(concat(f1, f2));

        assertEquals(2, got.size());
        assertArrayEquals(f1, got.get(0));
        assertArrayEquals(f2, got.get(1));
    }

    @Test
    public void 半包分两次到达也能拼起来() {
        byte[] f = frame((byte) 0xA1, "{\"status\":\"run\"}".getBytes());
        FrameAssembler a = new FrameAssembler();

        assertTrue(a.feed(Arrays.copyOfRange(f, 0, 7)).isEmpty());
        List<byte[]> got = a.feed(Arrays.copyOfRange(f, 7, f.length));

        assertEquals(1, got.size());
        assertArrayEquals(f, got.get(0));
        assertEquals(0, a.pending());
    }

    @Test
    public void 帧头前面有杂数据时旧实现会截掉帧尾() {
        byte[] junk = new byte[]{0x11, 0x22, 0x33};
        byte[] f = frame((byte) 0xA1, "{\"a\":1}".getBytes());

        byte[] legacy = new LegacyAssembler().feed(concat(junk, f)).get(0);
        byte[] fixed = new FrameAssembler().feed(concat(junk, f)).get(0);

        assertTrue("旧实现截掉了帧尾", legacy.length < f.length);
        assertArrayEquals(f, fixed);
    }

    @Test
    public void 坏帧不影响后面的好帧() {
        byte[] bad = frame((byte) 0xA1, "{\"x\":1}".getBytes());
        bad[bad.length - 4] ^= 0x01; // 破坏 CRC
        byte[] good = frame((byte) 0xA2, "{\"y\":2}".getBytes());

        List<byte[]> got = new FrameAssembler().feed(concat(bad, good));

        assertEquals(1, got.size());
        assertArrayEquals(good, got.get(0));
    }

    @Test
    public void 没有帧头的杂数据不会堆积() {
        FrameAssembler a = new FrameAssembler();
        a.feed(new byte[]{0x01, 0x02, 0x03, 0x04});
        a.feed(new byte[]{0x05, 0x06});

        assertEquals("旧实现这里会一直留着", 0, a.pending());
    }

    @Test
    public void 粘包拆包混合连续喂入() {
        byte[][] frames = new byte[20][];
        for (int i = 0; i < frames.length; i++) {
            frames[i] = frame((byte) 0xA1, ("{\"n\":" + i + "}").getBytes());
        }
        byte[] all = concat(frames);
        FrameAssembler a = new FrameAssembler();
        List<byte[]> got = new ArrayList<>();
        for (int pos = 0; pos < all.length; ) {
            int step = 1 + (pos % 7); // 每次喂 1~7 字节，模拟串口拆包
            int end = Math.min(pos + step, all.length);
            got.addAll(a.feed(Arrays.copyOfRange(all, pos, end)));
            pos = end;
        }
        assertEquals(frames.length, got.size());
        for (int i = 0; i < frames.length; i++) assertArrayEquals(frames[i], got.get(i));
    }
}
