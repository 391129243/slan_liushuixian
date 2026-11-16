"""对照验证：把 FrameAssembler.java 的逻辑逐行搬成 Python，与旧的字符串切帧实现对比。

跑法：python -I tools/verify_frame_assembler.py
本机没有 JDK，所以用这份脚本代替 JUnit 做验证。两边的帧协议实现必须保持一致。
"""
import sys

FRAME_OVERHEAD = 10
MAX_PAYLOAD = 4096


def frame(type_, payload: bytes) -> bytes:
    # 长度字段和 CRC 都只覆盖载荷，不含类型字节，与 RNARunningMessage.encodeBytes 一致
    length = len(payload)
    f = bytearray(length + 10)
    f[0], f[1] = 0x5A, 0xA5
    f[2], f[3] = (length >> 8) & 0xFF, length & 0xFF
    f[4] = type_
    f[5:5 + length] = payload
    s = sum(payload) & 0xFFFF
    f[5 + length] = (s >> 8) & 0xFF
    f[6 + length] = s & 0xFF
    f[-3], f[-2], f[-1] = 0xFF, 0xFC, 0xFF
    return bytes(f)


class FrameAssembler:
    """与 app/src/main/java/.../connect/FrameAssembler.java 逐行对应。"""

    def __init__(self, capacity=8192):
        self.buf = bytearray(capacity)
        self.len = 0

    def pending(self):
        return self.len

    def feed(self, data: bytes):
        frames = []
        if not data:
            return frames
        if self.len + len(data) > len(self.buf):
            self.len = 0
        copy = min(len(data), len(self.buf) - self.len)
        self.buf[self.len:self.len + copy] = data[:copy]
        self.len += copy

        pos = 0
        while self.len - pos >= FRAME_OVERHEAD:
            if self.buf[pos] != 0x5A or self.buf[pos + 1] != 0xA5:
                pos += 1
                continue
            payload_len = (self.buf[pos + 2] << 8) | self.buf[pos + 3]
            if payload_len > MAX_PAYLOAD:
                pos += 1
                continue
            frame_len = payload_len + FRAME_OVERHEAD
            if self.len - pos < frame_len:
                break
            end = pos + frame_len
            if not (self.buf[end - 3] == 0xFF and self.buf[end - 2] == 0xFC
                    and self.buf[end - 1] == 0xFF) or not self._crc_ok(pos, payload_len):
                pos += 1
                continue
            frames.append(bytes(self.buf[pos:end]))
            pos = end
        # 剩下的数据里没有帧头才清理；已找到帧头的半包必须保留
        has_head = any(self.buf[i] == 0x5A and self.buf[i + 1] == 0xA5
                       for i in range(pos, self.len - 1))
        if not has_head and pos < self.len - 1 and self.buf[self.len - 1] != 0x5A:
            pos = self.len
        elif not has_head and pos < self.len - 1:
            pos = self.len - 1
        if pos > 0:
            self.buf[:self.len - pos] = self.buf[pos:self.len]
            self.len -= pos
        return frames

    def _crc_ok(self, start, payload_len):
        # CRC 只覆盖载荷，不含类型字节，与各消息类的 getCRCBytes 一致
        s = sum(self.buf[start + 5:start + 5 + payload_len]) & 0xFFFF
        crc_pos = start + 5 + payload_len
        crc = (self.buf[crc_pos] << 8) | self.buf[crc_pos + 1]
        return s == crc


class LegacyAssembler:
    """Connection.onReceiveMessage 的旧实现，按原样搬过来。"""

    def __init__(self):
        self.sb = ""

    @staticmethod
    def _hex(data: bytes) -> str:
        result = ""
        for b in data:                       # 原 HexListToString16 的 O(n^2) 拼接
            result = result + f"{b:02X} "
        return result

    def feed(self, data: bytes):
        self.sb += self._hex(data)
        cmd_list = []
        while True:
            process = self.sb.upper()
            if "5A A5" in process and "FF FC FF" in process:
                head = process.index("5A A5")
                tail = process.index("FF FC FF")
                if tail > head:
                    # 第二个参数写成了长度，而 substring 要的是结束位置
                    cmd_list.append(process[head:tail - head + 8])
                    self.sb = self.sb[tail + 8:]
                else:
                    self.sb = self.sb[tail + 8:]
            elif "5A A5" in process:
                self.sb = self.sb[process.index("5A A5"):]
                break
            else:
                break                        # 原 "drop" 分支：什么都没删
        frames = []
        for msg in cmd_list:
            parts = msg.split()
            frames.append(bytes(int(p, 16) for p in parts))
        return frames


def check(name, cond):
    print(("PASS " if cond else "FAIL ") + name)
    return cond


def main():
    ok = True
    f1 = frame(0xA1, b'{"a":1}')
    f2 = frame(0xA2, b'{"b":22}')

    got = FrameAssembler().feed(f1 + f2)
    ok &= check("连续两帧都能切出", got == [f1, f2])

    a = FrameAssembler()
    ok &= check("半包先不输出", a.feed(f1[:7]) == [])
    ok &= check("半包补齐后拼出完整帧", a.feed(f1[7:]) == [f1] and a.pending() == 0)

    junk = bytes([0x11, 0x22, 0x33])
    legacy = LegacyAssembler().feed(junk + f1)
    fixed = FrameAssembler().feed(junk + f1)
    ok &= check("旧实现：帧头前有杂数据时截掉帧尾", len(legacy) == 1 and len(legacy[0]) < len(f1))
    ok &= check("新实现：帧头前有杂数据时帧完整", fixed == [f1])

    bad = bytearray(frame(0xA1, b'{"x":1}'))
    bad[-4] ^= 0x01
    good = frame(0xA2, b'{"y":2}')
    ok &= check("坏帧不影响后面的好帧", FrameAssembler().feed(bytes(bad) + good) == [good])

    a = FrameAssembler()
    a.feed(bytes([1, 2, 3, 4]))
    a.feed(bytes([5, 6]))
    legacy_a = LegacyAssembler()
    legacy_a.feed(bytes([1, 2, 3, 4]))
    legacy_a.feed(bytes([5, 6]))
    ok &= check("新实现：没有帧头的杂数据不堆积", a.pending() == 0)
    ok &= check("旧实现：没有帧头的杂数据一直留着", len(legacy_a.sb) > 0)

    frames = [frame(0xA1, f'{{"n":{i}}}'.encode()) for i in range(20)]
    allb = b"".join(frames)
    a = FrameAssembler()
    got = []
    pos = 0
    while pos < len(allb):                   # 每次喂 1~7 字节，模拟串口拆包
        step = 1 + pos % 7
        got += a.feed(allb[pos:pos + step])
        pos += step
    ok &= check("粘包拆包混合连续喂入 20 帧", got == frames)

    # 帧头被拆到两次读取的边界上：第一次只读到 5A，第二次才读到 A5
    f = frame(0xA1, b'{"z":3}')
    a = FrameAssembler()
    a.feed(bytes([0x11]) + f[:1])
    ok &= check("帧头跨两次读取也能拼出", a.feed(f[1:]) == [f] and a.pending() == 0)

    # 长度字段损坏成超大值时不能一直等，要跳过重新同步
    bogus = bytearray(frame(0xA1, b'{"q":1}'))
    bogus[2], bogus[3] = 0x7F, 0xFF
    ok &= check("超大长度字段后的好帧不丢", FrameAssembler().feed(bytes(bogus) + f) == [f])

    print("ALL PASSED" if ok else "FAILED")
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
