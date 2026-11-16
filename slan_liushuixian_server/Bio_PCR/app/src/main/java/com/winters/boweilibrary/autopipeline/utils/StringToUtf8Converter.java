package com.winters.boweilibrary.autopipeline.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.*;
import java.io.UnsupportedEncodingException;
public class StringToUtf8Converter {

    // 方法1：使用String.getBytes()直接转换
    public static byte[] convertToUtf8_method1(String input) throws UnsupportedEncodingException {
        return input.getBytes("UTF-8");
    }

    // 方法2：通过InputStreamReader/OutputStreamWriter桥接转换
    public static byte[] convertToUtf8_method2(String input) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        OutputStreamWriter writer = new OutputStreamWriter(baos, "UTF-8");
        writer.write(input);
        writer.flush();
        return baos.toByteArray();
    }

    // 方法3：使用NIO Charset转换
    public static byte[] convertToUtf8(String input) {
        Charset utf8 = StandardCharsets.UTF_8;
        return utf8.encode(input).array();
    }
}
