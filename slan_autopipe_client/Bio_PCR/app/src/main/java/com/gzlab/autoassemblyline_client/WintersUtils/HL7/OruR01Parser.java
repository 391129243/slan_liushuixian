package com.gzlab.autoassemblyline_client.WintersUtils.HL7;
import java.io.IOException;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.v251.message.ORU_R01;
import ca.uhn.hl7v2.parser.PipeParser;
import ca.uhn.hl7v2.util.Terser;

/**
 * ORU_R01 消息解析工具 (v2.3.1)
 */
public class OruR01Parser {

    private final PipeParser parser = new PipeParser();

    /**
     * 解析 HL7 消息字符串
     */
    public ORU_R01 parse(String hl7Message) throws HL7Exception {
        return (ORU_R01) parser.parse(hl7Message);
    }

    /**
     * 解析并打印消息内容
     */
    public void parseAndPrint(String hl7Message) throws HL7Exception {
        ORU_R01 message = parse(hl7Message);
        Terser terser = new Terser(message);

        // 打印消息头
        System.out.println("=== MSH 头信息 ===");
        System.out.println("消息类型: " + terser.get("/MSH-9-1"));
        System.out.println("消息时间: " + terser.get("/MSH-7"));
        System.out.println("消息控制ID: " + terser.get("/MSH-10"));

        // 打印患者信息
        System.out.println("\n=== 患者信息 ===");
        System.out.println("患者ID: " + terser.get("/PATIENT_RESULT/PATIENT/PID-3-1"));
        System.out.println("姓名: " + terser.get("/PATIENT_RESULT/PATIENT/PID-5-1")
                + " " + terser.get("/PATIENT_RESULT/PATIENT/PID-5-2"));

        // 打印检验结果
        System.out.println("\n=== 检验结果 ===");
        int obxCount = terser.get("/PATIENT_RESULT/ORDER_OBSERVATION/OBSERVATION/OBX").length();
        for (int i = 0; i < obxCount; i++) {
            String path = String.format("/PATIENT_RESULT/ORDER_OBSERVATION/OBSERVATION(%d)/OBX", i);
            System.out.printf("项目: %s | 结果: %s %s | 参考范围: %s | 状态: %s%n",
                    terser.get(path + "-3-1"),
                    terser.get(path + "-5-1"),
                    terser.get(path + "-6-1"),
                    terser.get(path + "-7"),
                    terser.get(path + "-11"));
        }
    }

    /**
     * 生成 ACK 响应消息
     */
    public String generateAck(ORU_R01 message) throws HL7Exception, IOException {
        return message.generateACK().encode();
    }
}