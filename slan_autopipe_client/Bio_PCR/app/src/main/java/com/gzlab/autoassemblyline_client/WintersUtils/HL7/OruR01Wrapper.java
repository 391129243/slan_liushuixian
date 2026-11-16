package com.gzlab.autoassemblyline_client.WintersUtils.HL7;

import android.os.Environment;
import android.util.Log;

import com.gzlab.autoassemblyline_client.bean.CtResult;
import com.gzlab.autoassemblyline_client.bean.CtRuleConclusion;
import com.gzlab.autoassemblyline_client.bean.JudgeConclusion;
import com.gzlab.autoassemblyline_client.bean.PeakResult;
import com.gzlab.autoassemblyline_client.bean.PeakRuleConclusion;
import com.gzlab.autoassemblyline_client.util.FileUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.app.Connection;
import ca.uhn.hl7v2.app.Initiator;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v251.message.ORU_R01;
import ca.uhn.hl7v2.model.v251.segment.MSH;
import ca.uhn.hl7v2.model.v251.segment.PID;
import ca.uhn.hl7v2.model.v251.segment.OBR;
import ca.uhn.hl7v2.model.v251.segment.OBX;
import ca.uhn.hl7v2.model.v251.group.ORU_R01_ORDER_OBSERVATION;
import ca.uhn.hl7v2.model.v251.group.ORU_R01_PATIENT_RESULT;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.parser.Parser;

/**
 * ORU_R01 消息封装器
 */
public class OruR01Wrapper {



    private  ORU_R01 oruR01;

    public ORU_R01 getOruR01() {
        return oruR01;
    }

    public void setOruR01(ORU_R01 oruR01) {
        this.oruR01 = oruR01;
    }

    private static DecimalFormat nf = new DecimalFormat( "0.00");

    private static final int PORT_NUMBER = 63395;// change this to whatever your port number is

    // In HAPI, almost all things revolve around a context object
    public static HapiContext context;

    public OruR01Wrapper() throws HL7Exception, IOException {
        this.oruR01 = new ORU_R01();
        this.oruR01.initQuickstart("ORU", "R01", "T");
        initializeMessageHeader();
    }

    public OruR01Wrapper(ORU_R01 message) {
        this.oruR01 = message;
    }
    // 初始化 MSH 段
    public void initializeMessageHeader() throws HL7Exception {
        MSH msh = oruR01.getMSH();
        msh.getFieldSeparator().setValue("|");
        msh.getEncodingCharacters().setValue("^~\\&");
        msh.getDateTimeOfMessage().getTime().setValue(getCurrentTimestamp());
        msh.getMessageType().getMsg1_MessageCode().setValue("ORU");
        msh.getMessageType().getMsg2_TriggerEvent().setValue("R01");
    }

    // 设置MSH字段
    /**
     * 设置消息头信息
     * @param sendingApp 发送应用
     * @param sendingFacility 发送机构
     * @param receivingApp 接收应用
     * @param receivingFacility 接收机构
     */
    public void setMSHFields(String sendingApp, String sendingFacility,
                             String receivingApp, String receivingFacility) throws HL7Exception {
        MSH msh = oruR01.getMSH();
        msh.getSendingApplication().getNamespaceID().setValue(sendingApp);
        msh.getSendingFacility().getNamespaceID().setValue(sendingFacility);
        msh.getReceivingApplication().getNamespaceID().setValue(receivingApp);
        msh.getReceivingFacility().getNamespaceID().setValue(receivingFacility);
        msh.getMessageControlID().setValue(generateMessageId());
    }

    // 添加患者信息
    /**
     * 添加患者信息
     * @param patientId 患者ID
     * @param lastName 姓
     * @param firstName 名
     * @param birthDate 出生日期 (格式: yyyyMMdd)
     * @param gender 性别 (M/F)
     */
    public void addPatientInfo(String patientId, String lastName, String firstName,
                               String birthDate, String gender) throws HL7Exception {
        ORU_R01_PATIENT_RESULT patientResult = oruR01.getPATIENT_RESULT();
        PID pid = patientResult.getPATIENT().getPID();
        pid.getPid1_SetIDPID().setValue("1");
        pid.getPatientID().getIDNumber().setValue("1");
        pid.getPatientIdentifierList(0).getIDNumber().setValue(patientId);
        pid.getPatientName(0).getFamilyName().getSurname().setValue(lastName);
        pid.getPatientName(0).getGivenName().setValue(firstName);
        pid.getDateTimeOfBirth().getTime().setValue(birthDate);
        pid.getAdministrativeSex().setValue(gender);
    }

    // 添加检验医嘱
    /**
     * 添加检验医嘱
     * @param placerOrderNumber 医嘱号
     * @param serviceId 检验项目代码
     * @param specimenId 标本号
     */
    public void addOrderObservation(String placerOrderNumber,String serviceId,
                                    String specimenId) throws HL7Exception {
        ORU_R01_ORDER_OBSERVATION orderObs = oruR01.getPATIENT_RESULT().getORDER_OBSERVATION(0);
        OBR obr = orderObs.getOBR();
        obr.getObr1_SetIDOBR().setValue("1");
        obr.getPlacerOrderNumber().getEntityIdentifier().setValue(placerOrderNumber);
        //obr.getFillerOrderNumber().getEntityIdentifier().setValue(fillerOrderNumber);
        obr.getUniversalServiceIdentifier().getIdentifier().setValue(serviceId);
        obr.getSpecimenSource().getSps1_SpecimenSourceNameOrCode().getCwe1_Identifier().setValue(specimenId);
        obr.getObservationDateTime().getTime().setValue(getCurrentTimestamp());
    }

    // 添加检验结果
    /**
     * 添加检验结果
     * @param testCode 检验项目代码
     * @param value 结果值
     * @param unit 单位
     * @param range 参考范围
     * @param status 结果状态 (F: 最终结果)
     */
    public void addResult(String testCode, String value,
                                   String unit, String range, String status,int i) throws HL7Exception {
        ORU_R01_ORDER_OBSERVATION orderObs = oruR01.getPATIENT_RESULT().getORDER_OBSERVATION();

        OBX obx = orderObs.getOBSERVATION(i).getOBX();

        obx.getObx1_SetIDOBX().setValue(i + "");
        obx.getValueType().setValue("NM"); // 数值型结果
        obx.getObservationIdentifier().getCe1_Identifier().setValue(testCode);
        obx.getObservationValue(0).getData().parse(value);
        obx.getUnits().getCe1_Identifier().setValue(unit);
        obx.getReferencesRange().setValue(range);
        obx.getObservationResultStatus().setValue(status);
    }

    // 获取完整的HL7消息字符串
    public String build() throws HL7Exception {
        return oruR01.encode();
    }

    // 工具方法
    private String getCurrentTimestamp() {
        return new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
    }

    private String generateMessageId() {
        return UUID.randomUUID().toString();
    }

    public static List<String> upload(String filePath,String ip,int port) {
        List<String> results = new ArrayList<>();

        try {
           // String file = Environment.getExternalStorageDirectory().getAbsolutePath() + "/PCR/" + "judgeConclusion1.result";

            FileInputStream inputStream = new FileInputStream(filePath);

            String strJson = FileUtils.fileToStr(inputStream);

            Gson gson = new GsonBuilder().registerTypeAdapter(Double.class, new JsonSerializer<Double>() {

                @Override
                public JsonElement serialize(Double src, Type typeOfSrc, JsonSerializationContext context) {
                    if (src == src.longValue())
                        return new JsonPrimitive(src.longValue());
                    return new JsonPrimitive(src);
                }
            }).create();

            JudgeConclusion judgeConclusion  = gson.fromJson(strJson,new TypeToken<JudgeConclusion>() {}.getType());

            inputStream.close();

            int count = 0;

            if(judgeConclusion.getCtRuleConclusions() != null && judgeConclusion.getCtRuleConclusions().size() != 0){
                count = judgeConclusion.getCtRuleConclusions().size();
            }else if(judgeConclusion.getPeakRuleConclusions() != null && judgeConclusion.getPeakRuleConclusions().size() != 0){
                count = judgeConclusion.getPeakRuleConclusions().size();
            }

            Log.e("test_one_by_one","sendResult_HL7_MLLP_single:"  + "count:" + count);

            for(int i = 0; i < count ; i++){
                String result = sendResult_HL7_MLLP_single(ip, port, judgeConclusion,i);
                results.add(result);
                Log.e("test_one_by_one","sendResult_HL7_MLLP_single:"  + "--" + i + "--" + result);
            }

        } catch (Exception e) {
            Log.e("test_one_by_one","send oru1 failed\n");
        }

        return results;
    }

    public static List<String> upload_01(final String filePath,String ip,int port) {
        List<String> results = new ArrayList<>();
        try {
            FileInputStream inputStream = new FileInputStream(filePath);
            String strJson = FileUtils.fileToStr(inputStream);
            Gson gson = new GsonBuilder().registerTypeAdapter(Double.class, new JsonSerializer<Double>() {

                @Override
                public JsonElement serialize(Double src, Type typeOfSrc, JsonSerializationContext context) {
                    if (src == src.longValue())
                        return new JsonPrimitive(src.longValue());
                    return new JsonPrimitive(src);
                }
            }).create();

            JudgeConclusion judgeConclusion  = gson.fromJson(strJson,new TypeToken<JudgeConclusion>() {}.getType());
            ArrayList<String> wellNames = new ArrayList<String>();
            if(null != judgeConclusion.getCtRuleConclusions() && judgeConclusion.getCtRuleConclusions().size()>0){
                wellNames.clear();
                for(CtRuleConclusion ctRuleConclusion : judgeConclusion.getCtRuleConclusions()){
                    if(!wellNames.contains(ctRuleConclusion.getTupeNo())){
                        wellNames.add(ctRuleConclusion.getTupeNo());
                    }

                }
            }else if(null != judgeConclusion.getPeakRuleConclusions() && judgeConclusion.getPeakRuleConclusions().size()>0){
                wellNames.clear();
                for(PeakRuleConclusion peakRuleConclusion : judgeConclusion.getPeakRuleConclusions()){
                    if(!wellNames.contains(peakRuleConclusion.getTupeNo())){
                        wellNames.add(peakRuleConclusion.getTupeNo());
                    }

                }
            }
            inputStream.close();
            if(null != wellNames && wellNames.size()>0){
                for(String wellName : wellNames){
                    String result = sendResult_HL7_MLLP_single_01(ip, port, judgeConclusion,wellName);
                    Log.e("test_one_by_one","sendResult_HL7_MLLP_single:"  + "--" + wellName + "--" + result);
                    results.add(result);
                }
            }

        } catch (Exception e) {
            Log.e("test_one_by_one","send oru1 failed\n");
        }

        return results;
    }

    public static String uploadSingle(String filePath,String ip,int port,String wellName){
        try {
            FileInputStream inputStream = new FileInputStream(filePath);
            String strJson = FileUtils.fileToStr(inputStream);
            Gson gson = new GsonBuilder().registerTypeAdapter(Double.class, new JsonSerializer<Double>() {

                @Override
                public JsonElement serialize(Double src, Type typeOfSrc, JsonSerializationContext context) {
                    if (src == src.longValue())
                        return new JsonPrimitive(src.longValue());
                    return new JsonPrimitive(src);
                }
            }).create();

            JudgeConclusion judgeConclusion  = gson.fromJson(strJson,new TypeToken<JudgeConclusion>() {}.getType());
            inputStream.close();

            String result = sendResult_HL7_MLLP_single_01(ip, port, judgeConclusion,wellName);
            Log.e("test_one_by_one","sendResult_HL7_MLLP_single:"  + "--" + wellName + "--" + result);
            return result;

        } catch (Exception e) {
            Log.e("test_one_by_one","send oru1 failed\n");
            return "error";
        }

    }

    public static void test() {

        try {
            String file = Environment.getExternalStorageDirectory().getAbsolutePath() + "/PCR/" + "judgeConclusion1.result";

            FileInputStream inputStream = new FileInputStream(file);

            String strJson = FileUtils.fileToStr(inputStream);

            Gson gson = new GsonBuilder().registerTypeAdapter(Double.class, new JsonSerializer<Double>() {

                @Override
                public JsonElement serialize(Double src, Type typeOfSrc, JsonSerializationContext context) {
                    if (src == src.longValue())
                        return new JsonPrimitive(src.longValue());
                    return new JsonPrimitive(src);
                }
            }).create();

            JudgeConclusion judgeConclusion  = gson.fromJson(strJson,new TypeToken<JudgeConclusion>() {}.getType());

            inputStream.close();

            sendResult_HL7_MLLP("127.0.0.1", PORT_NUMBER, judgeConclusion);


        } catch (Exception e) {
            Log.e("test","send oru1 failed\n");
        }

    }


    public static void test_one_by_one() {

        try {
            String file = Environment.getExternalStorageDirectory().getAbsolutePath() + "/PCR/" + "judgeConclusion.result";
            Log.e("test_one_by_one","file"  + file);
            FileInputStream inputStream = new FileInputStream(file);

            String strJson = FileUtils.fileToStr(inputStream);

            Gson gson = new GsonBuilder().registerTypeAdapter(Double.class, new JsonSerializer<Double>() {

                @Override
                public JsonElement serialize(Double src, Type typeOfSrc, JsonSerializationContext context) {
                    if (src == src.longValue())
                        return new JsonPrimitive(src.longValue());
                    return new JsonPrimitive(src);
                }
            }).create();

            JudgeConclusion judgeConclusion  = gson.fromJson(strJson,new TypeToken<JudgeConclusion>() {}.getType());

            inputStream.close();

            int count = 0;

            if(judgeConclusion.getCtRuleConclusions() != null && judgeConclusion.getCtRuleConclusions().size() != 0){
                count = judgeConclusion.getCtRuleConclusions().size();
            }else if(judgeConclusion.getPeakRuleConclusions() != null && judgeConclusion.getPeakRuleConclusions().size() != 0){
                count = judgeConclusion.getPeakRuleConclusions().size();
            }

            Log.e("test_one_by_one","sendResult_HL7_MLLP_single:"  + "count:" + count);



            for(int i = 0; i < count ; i++){
                String result = sendResult_HL7_MLLP_single("192.168.1.103", PORT_NUMBER, judgeConclusion,i);
                Log.e("test_one_by_one","sendResult_HL7_MLLP_single:"  + "--" + i + "--" + result);
            }

        } catch (Exception e) {
            Log.e("test_one_by_one","send oru1 failed\n");
        }

    }



    public static void main(String[] args) {
        try {
            // 创建消息构建器
            OruR01Wrapper builder = new OruR01Wrapper();
            builder.initializeMessageHeader();
            builder.setMSHFields("ZZK-08A", "ZZK-08A", "LIS", "LIS");
            builder.addPatientInfo("P123456", "张", "三", "19800101", "M");
            builder.addOrderObservation("ORD2023090001", "GLUCOSE", "SP2023090001");
            builder.addResult("GLU", "5.6", "mmol/L", "3.9-6.1", "F",0);
            builder.addResult("HbA1c", "6.2", "%", "4.0-6.0", "F",1);

            // 生成 HL7 消息字符串
            String hl7Message = builder.build();
            System.out.println("生成的 HL7 消息:\n" + hl7Message);

            context = new DefaultHapiContext();

            // create a new MLLP client over the specified port
            Connection connection = context.newClient("127.0.0.1", PORT_NUMBER, false);

            // The initiator which will be used to transmit our message
            Initiator initiator = connection.getInitiator();

            // send the previously created HL7 message over the connection established
            Parser parser = context.getPipeParser();
            System.out.println("Sending message:" + "\n" + parser.encode(builder.getOruR01()));
            Message response = initiator.sendAndReceive(builder.getOruR01());

            // display the message response received from the remote party
            String responseString = parser.encode(response);
            System.out.println("Received response:\n" + responseString);

            if(responseString.contains("ack")){
                System.out.println("send oru1 succeed\n");
            }
//            String file = "D:\\judgeConclusion.result";
//            FileInputStream inputStream = new FileInputStream(file);
//            String strJson = FileUtils.fileToStr(inputStream);
//
//            Gson gson = new GsonBuilder().registerTypeAdapter(Double.class, new JsonSerializer<Double>() {
//
//                @Override
//                public JsonElement serialize(Double src, Type typeOfSrc, JsonSerializationContext context) {
//                    if (src == src.longValue())
//                        return new JsonPrimitive(src.longValue());
//                    return new JsonPrimitive(src);
//                }
//            }).create();
//
//            JudgeConclusion judgeConclusion  = gson.fromJson(strJson,new TypeToken<JudgeConclusion>() {}.getType());
//
//            inputStream.close();
//
//            sendResult_HL7_MLLP("127.0.0.1", PORT_NUMBER, judgeConclusion);


        } catch (Exception e) {
            System.out.println("send oru1 failed\n");
        }
    }

    public static String getHl7Message() {
        try {
            // 1. 创建消息构建器
            OruR01Wrapper builder = new OruR01Wrapper();
            builder.initializeMessageHeader();
            builder.setMSHFields("LIS", "HOSPITAL_LAB", "HIS", "HOSPITAL_INFO_SYSTEM");
            builder.addPatientInfo("P123456", "张", "三", "19800101", "M");
            builder.addOrderObservation("ORD2023090001", "GLUCOSE", "SP2023090001");
            builder.addResult("GLU", "5.6", "mmol/L", "3.9-6.1", "F",1);
            builder.addResult("HbA1c", "6.2%", "%", "4.0-6.0", "F",2);

            // 2. 生成 HL7 消息字符串
            String hl7Message = builder.build();
            System.out.println("生成的 HL7 消息:\n" + hl7Message);

            // 3. 解析验证消息
            OruR01Parser parser = new OruR01Parser();
            parser.parseAndPrint(hl7Message);

            return hl7Message;

        } catch (HL7Exception e) {
            e.printStackTrace();
            return "";
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }



    public static String sendResult_HL7_MLLP(String IP, int port, JudgeConclusion judgeConclusion)  {

        // create a new MLLP client over the specified port
        Connection connection;

        Initiator initiator;

        if(judgeConclusion == null){

            return "error : judgeConclusion null";

        }
        try {
            connection = context.newClient(IP, port, false);

            initiator = connection.getInitiator();

            int count;

            if(judgeConclusion.getCtRuleConclusions() != null && judgeConclusion.getCtRuleConclusions().size() != 0){
                count = judgeConclusion.getCtRuleConclusions().size();
            }else if(judgeConclusion.getPeakRuleConclusions() != null && judgeConclusion.getPeakRuleConclusions().size() != 0){
                count = judgeConclusion.getPeakRuleConclusions().size();
            }else{
                return "error : judgeConclusion null";
            }

            for(int i = 0; i < count; i++){
                // 1. 创建消息构建器
                OruR01Wrapper builder = new OruR01Wrapper();
                builder.initializeMessageHeader();
                builder.setMSHFields("ZZK-08A", "ZZK-08A", "LIS", "LIS");
                int index = 0;
                if(judgeConclusion.getCtRuleConclusions() != null && judgeConclusion.getCtRuleConclusions().size() != 0){
                    builder.addPatientInfo(judgeConclusion.getCtRuleConclusions().get(i).getSampleID(),"", "", "", "");
                    builder.addOrderObservation(judgeConclusion.getCtRuleConclusions().get(i).getTime(),"待定" , judgeConclusion.getCtRuleConclusions().get(i).getSampleID());
                    Map<String, List<CtResult>> mapCT= judgeConclusion.getCtRuleConclusions().get(i).getConlusionMap();
                    Iterator<Map.Entry<String, List<CtResult>>> iteratorCT = mapCT.entrySet().iterator();
                    while (iteratorCT.hasNext()) { // 这里可以根据需要调整轮询次数或条件
                        Map.Entry<String, List<CtResult>> entry  = iteratorCT.next();
                        Log.e("sendResult_HL7_MLLP", "iteratorCT Key: " + entry.getKey() + ", Value: " + entry.getValue());
                        String channelCT = entry.getKey();
                        List<CtResult> list =entry.getValue();
                        if(list != null && list.size() > 0){
                            for(int k = 0; k < list.size(); k++){
                                CtResult ctResult = list.get(k);
                                String range = nf.format(ctResult.getCtValueDown()) +
                                        "--" + nf.format(ctResult.getCtValueUp()) +
                                        "--" + nf.format(ctResult.getCtValueUp_Suspicious());
                                builder.addResult(ctResult.getCategory(), nf.format(ctResult.getActualCt()), "",range, ctResult.getConclusion(),index++);
                            }
                        }
                    }


                    if(judgeConclusion.getPeakRuleConclusions() != null && judgeConclusion.getPeakRuleConclusions().size() != 0){
                        Map<String, List<PeakResult>> mapPEAK= judgeConclusion.getPeakRuleConclusions().get(i).getConlusionMap();
                        Iterator<Map.Entry<String, List<PeakResult>>> iteratorPEAK = mapPEAK.entrySet().iterator();
                        while (iteratorPEAK.hasNext()) { // 这里可以根据需要调整轮询次数或条件
                            Map.Entry<String, List<PeakResult>> entry  = iteratorPEAK.next();
                            Log.e("sendResult_HL7_MLLP", "iteratorPEAK Key: " + entry.getKey() + ", Value: " + entry.getValue());

                            String channelPeak = entry.getKey();

                            List<PeakResult> list =entry.getValue();
                            if(list != null && list.size() > 0){
                                for(int k = 0; k < list.size(); k++){
                                    PeakResult peakResult = list.get(k);
                                    String range = "TM:" + nf.format(peakResult.getActualTM() -peakResult.getPeakValueDown()) +
                                            "--" +  nf.format(peakResult.getActualTM() + peakResult.getPeakValueUp())  + "  " +
                                            "RM: >="  + nf.format(peakResult.getPeakValueRM());
                                    builder.addResult(peakResult.getCategory(), "TM:" + nf.format(peakResult.getActualTM()) + " " +"RM:" + nf.format(peakResult.getActualRM()),
                                            "",range, peakResult.getConclusion(),index++);
                                }
                            }
                        }
                    }
                }else if(judgeConclusion.getPeakRuleConclusions() != null && judgeConclusion.getPeakRuleConclusions().size() != 0){

                    for(int j = 0; j < judgeConclusion.getPeakRuleConclusions().get(i).getConlusionMap().size(); j++){

                        Map<String, List<PeakResult>> mapPEAK= judgeConclusion.getPeakRuleConclusions().get(i).getConlusionMap();
                        Iterator<Map.Entry<String, List<PeakResult>>> iteratorPEAK = mapPEAK.entrySet().iterator();
                        while (iteratorPEAK.hasNext()) { // 这里可以根据需要调整轮询次数或条件
                            Map.Entry<String, List<PeakResult>> entry  = iteratorPEAK.next();
                            Log.e("sendResult_HL7_MLLP", "iteratorPEAK Key: " + entry.getKey() + ", Value: " + entry.getValue());

                            String channelPeak = entry.getKey();

                            List<PeakResult> list =entry.getValue();
                            if(list != null && list.size() > 0){
                                for(int k = 0; k < list.size(); k++){
                                    PeakResult peakResult = list.get(k);
                                    String range = "TM:" + nf.format(peakResult.getActualTM() -peakResult.getPeakValueDown()) +
                                            "--" +  nf.format(peakResult.getActualTM() + peakResult.getPeakValueUp())  + "  " +
                                            "RM: >="  + nf.format(peakResult.getPeakValueRM());
                                    builder.addResult(peakResult.getCategory(), "TM:" + nf.format(peakResult.getActualTM()) + " " +"RM:" + nf.format(peakResult.getActualRM()),
                                            "",range, peakResult.getConclusion(),index++);
                                }
                            }
                        }
                    }
                }else{
                    ///////////
                    return "error : judgeConclusion null";
                }

                // send the previously created HL7 message over the connection established
                Parser parser = context.getPipeParser();
                String message = parser.encode(builder.getOruR01());
                Log.e("sendResult_HL7_MLLP", "Sending message:" + "\n" + message);
                Message response = initiator.sendAndReceive(builder.getOruR01());

                // display the message response received from the remote party
                String responseString = parser.encode(response);
                Log.e("sendResult_HL7_MLLP", "Received response:\n" + responseString);

                if(responseString.contains("ack")){
                    Log.e("sendResult_HL7_MLLP", "send oru1 succeed\n");
                }
            }

            return "succeed";

        } catch (Exception e) {
            Log.e("sendResult_HL7_MLLP error ", e.getMessage());
            return "error:" + e.getMessage();
        }
    }


    //HL7长传单个结果
    public static String sendResult_HL7_MLLP_single_01(String IP, int port, JudgeConclusion judgeConclusion,String wellName)  {

        // create a new MLLP client over the specified port
//        Connection connection;
//
//        Initiator initiator;

        if(judgeConclusion == null){
            return "error : judgeConclusion null";
        }

        try {

            Socket socket = new Socket(IP, port);

            OutputStream out = socket.getOutputStream();

            if(context == null){
                context = new DefaultHapiContext();
            }

//            connection = context.newClient(IP, port, false);
//
//            initiator = connection.getInitiator();

            // 1. 创建消息构建器
            OruR01Wrapper builder = new OruR01Wrapper();
            builder.initializeMessageHeader();
            builder.setMSHFields("ZZK-08A", "ZZK-08A", "LIS", "LIS");
            int index = 0;
            //Ct结论
            if(judgeConclusion.getCtRuleConclusions() != null && judgeConclusion.getCtRuleConclusions().size() != 0){
                Log.e("sendResult_HL7_MLLP","Ct结论");
                for(CtRuleConclusion ctRuleConclusion : judgeConclusion.getCtRuleConclusions()){
                    if(null != ctRuleConclusion && ctRuleConclusion.getTupeNo().equals(wellName)){
                        //确定是哪个孔位的结论
                        //患者信息
                        builder.addPatientInfo(ctRuleConclusion.getSampleID(),"", "", "", "");
                        builder.addOrderObservation(ctRuleConclusion.getTime(),"待定" , ctRuleConclusion.getSampleID());
                        Map<String, List<CtResult>> mapCT= ctRuleConclusion.getConlusionMap();
                        Iterator<Map.Entry<String, List<CtResult>>> iteratorCT = mapCT.entrySet().iterator();
                        while (iteratorCT.hasNext()) { // 这里可以根据需要调整轮询次数或条件
                            Map.Entry<String, List<CtResult>> entry  = iteratorCT.next();
                            Log.e("sendResult_HL7_MLLP", "iteratorCT Key: " + entry.getKey() + ", Value: " + entry.getValue());
                            String channelCT = entry.getKey();
                            List<CtResult> list =entry.getValue();
                            if(list != null && list.size() > 0){
                                for(int k = 0; k < list.size(); k++){
                                    CtResult ctResult = list.get(k);
                                    String range = nf.format(ctResult.getCtValueDown()) +
                                            "--" + nf.format(ctResult.getCtValueUp()) +
                                            "--" + nf.format(ctResult.getCtValueUp_Suspicious());
                                    builder.addResult(ctResult.getCategory(), nf.format(ctResult.getActualCt()), "",range, ctResult.getConclusion(),index++);
                                }
                            }
                        }
                    }
                }
            }

           if(judgeConclusion.getPeakRuleConclusions() != null && judgeConclusion.getPeakRuleConclusions().size() != 0){
               Log.e("sendResult_HL7_MLLP","Peak结论");
               for(PeakRuleConclusion peakRuleConclusion: judgeConclusion.getPeakRuleConclusions()){
                   if(null != peakRuleConclusion && peakRuleConclusion.getTupeNo().equals(wellName)){
                       Map<String, List<PeakResult>> mapPEAK= peakRuleConclusion.getConlusionMap();
                       Iterator<Map.Entry<String, List<PeakResult>>> iteratorPEAK = mapPEAK.entrySet().iterator();
                       while (iteratorPEAK.hasNext()) { // 这里可以根据需要调整轮询次数或条件
                           Map.Entry<String, List<PeakResult>> entry  = iteratorPEAK.next();
                           Log.e("sendResult_HL7_MLLP", "iteratorPEAK Key: " + entry.getKey() + ", Value: " + entry.getValue());

                           String channelPeak = entry.getKey();

                           List<PeakResult> list =entry.getValue();
                           if(list != null && list.size() > 0){
                               for(int k = 0; k < list.size(); k++){
                                   PeakResult peakResult = list.get(k);
                                   String range = "TM:" + nf.format(peakResult.getActualTM() -peakResult.getPeakValueDown()) +
                                           "--" +  nf.format(peakResult.getActualTM() + peakResult.getPeakValueUp())  + "  " +
                                           "RM: >="  + nf.format(peakResult.getPeakValueRM());
                                   builder.addResult(peakResult.getCategory(), "TM:" + nf.format(peakResult.getActualTM()) + " " +"RM:" + nf.format(peakResult.getActualRM()),
                                           "",range, peakResult.getConclusion(),index++);
                               }
                           }
                       }
                   }
               }
           }

            // send the previously created HL7 message over the connection established
            Parser parser = context.getPipeParser();
            String message = parser.encode(builder.getOruR01());
            Log.e("sendResult_HL7_MLLP", "Sending message:" + "\n" + message);
//            Message response = initiator.sendAndReceive(builder.getOruR01());

            // display the message response received from the remote party
//            String responseString = parser.encode(response);
//            Log.e("sendResult_HL7_MLLP", "Received response:\n" + responseString);
//
//            if(responseString.contains("ack")){
//                Log.e("sendResult_HL7_MLLP", "send oru1 succeed\n");
//            }

//            connection.close();

            byte[] mllpData = wrapMLLP(message);
            out.write(mllpData);
            out.flush();

            // 接收响应（可选）
            InputStream in = socket.getInputStream();
            byte[] buffer = new byte[1024];
            int bytesRead = in.read(buffer);
            String response = unwrapMLLP(buffer,bytesRead);
            Log.d("HL7", "Response: " + response);
            if(response.contains("ack")){
                Log.e("sendResult_HL7_MLLP", "send oru1 succeed\n");
            }

            return "succeed" +" " + wellName;

        } catch (Exception e) {
            Log.e("sendResult_HL7_MLLP error ", e.getMessage());
            return "error:" + "" + wellName + "" +  e.getMessage();
        }
    }






    public static String sendResult_HL7_MLLP_single(String IP, int port, JudgeConclusion judgeConclusion,int i)  {

        // create a new MLLP client over the specified port
//        Connection connection;
//
//        Initiator initiator;

        if(judgeConclusion == null){
            return "error : judgeConclusion null";
        }
        String tupeNo = "";
        try {

            Socket socket = new Socket(IP, port);

            OutputStream out = socket.getOutputStream();

            if(context == null){
                context = new DefaultHapiContext();
            }

//            connection = context.newClient(IP, port, false);
//
//            initiator = connection.getInitiator();

            // 1. 创建消息构建器
            OruR01Wrapper builder = new OruR01Wrapper();
            builder.initializeMessageHeader();
            builder.setMSHFields("ZZK-08A", "ZZK-08A", "LIS", "LIS");
            int index = 0;
            if(judgeConclusion.getCtRuleConclusions() != null && judgeConclusion.getCtRuleConclusions().size() != 0){
                builder.addPatientInfo(judgeConclusion.getCtRuleConclusions().get(i).getSampleID(),"", "", "", "");
                builder.addOrderObservation(judgeConclusion.getCtRuleConclusions().get(i).getTime(),"待定" , judgeConclusion.getCtRuleConclusions().get(i).getSampleID());
                Map<String, List<CtResult>> mapCT= judgeConclusion.getCtRuleConclusions().get(i).getConlusionMap();
                Iterator<Map.Entry<String, List<CtResult>>> iteratorCT = mapCT.entrySet().iterator();
                while (iteratorCT.hasNext()) { // 这里可以根据需要调整轮询次数或条件
                    Map.Entry<String, List<CtResult>> entry  = iteratorCT.next();
                    Log.e("sendResult_HL7_MLLP", "iteratorCT Key: " + entry.getKey() + ", Value: " + entry.getValue());
                    String channelCT = entry.getKey();
                    List<CtResult> list =entry.getValue();
                    if(list != null && list.size() > 0){
                        for(int k = 0; k < list.size(); k++){
                            CtResult ctResult = list.get(k);
                            String range = nf.format(ctResult.getCtValueDown()) +
                                    "--" + nf.format(ctResult.getCtValueUp()) +
                                    "--" + nf.format(ctResult.getCtValueUp_Suspicious());
                            builder.addResult(ctResult.getCategory(), nf.format(ctResult.getActualCt()), "",range, ctResult.getConclusion(),index++);
                        }
                    }
                }


                if(judgeConclusion.getPeakRuleConclusions() != null && judgeConclusion.getPeakRuleConclusions().size() != 0){
                    Map<String, List<PeakResult>> mapPEAK= judgeConclusion.getPeakRuleConclusions().get(i).getConlusionMap();
                    Iterator<Map.Entry<String, List<PeakResult>>> iteratorPEAK = mapPEAK.entrySet().iterator();
                    while (iteratorPEAK.hasNext()) { // 这里可以根据需要调整轮询次数或条件
                        Map.Entry<String, List<PeakResult>> entry  = iteratorPEAK.next();
                        Log.e("sendResult_HL7_MLLP", "iteratorPEAK Key: " + entry.getKey() + ", Value: " + entry.getValue());

                        String channelPeak = entry.getKey();

                        List<PeakResult> list =entry.getValue();
                        if(list != null && list.size() > 0){
                            for(int k = 0; k < list.size(); k++){
                                PeakResult peakResult = list.get(k);
                                String range = "TM:" + nf.format(peakResult.getActualTM() -peakResult.getPeakValueDown()) +
                                        "--" +  nf.format(peakResult.getActualTM() + peakResult.getPeakValueUp())  + "  " +
                                        "RM: >="  + nf.format(peakResult.getPeakValueRM());
                                builder.addResult(peakResult.getCategory(), "TM:" + nf.format(peakResult.getActualTM()) + " " +"RM:" + nf.format(peakResult.getActualRM()),
                                        "",range, peakResult.getConclusion(),index++);
                            }
                        }
                    }
                }
            }else if(judgeConclusion.getPeakRuleConclusions() != null && judgeConclusion.getPeakRuleConclusions().size() != 0){

                for(int j = 0; j < judgeConclusion.getPeakRuleConclusions().get(i).getConlusionMap().size(); j++){

                    Map<String, List<PeakResult>> mapPEAK= judgeConclusion.getPeakRuleConclusions().get(i).getConlusionMap();
                    Iterator<Map.Entry<String, List<PeakResult>>> iteratorPEAK = mapPEAK.entrySet().iterator();
                    while (iteratorPEAK.hasNext()) { // 这里可以根据需要调整轮询次数或条件
                        Map.Entry<String, List<PeakResult>> entry  = iteratorPEAK.next();
                        Log.e("sendResult_HL7_MLLP", "iteratorPEAK Key: " + entry.getKey() + ", Value: " + entry.getValue());

                        String channelPeak = entry.getKey();

                        List<PeakResult> list =entry.getValue();
                        if(list != null && list.size() > 0){
                            for(int k = 0; k < list.size(); k++){
                                PeakResult peakResult = list.get(k);
                                String range = "TM:" + nf.format(peakResult.getActualTM() -peakResult.getPeakValueDown()) +
                                        "--" +  nf.format(peakResult.getActualTM() + peakResult.getPeakValueUp())  + "  " +
                                        "RM: >="  + nf.format(peakResult.getPeakValueRM());
                                builder.addResult(peakResult.getCategory(), "TM:" + nf.format(peakResult.getActualTM()) + " " +"RM:" + nf.format(peakResult.getActualRM()),
                                        "",range, peakResult.getConclusion(),index++);
                            }
                        }
                    }
                }
            }else{
                ///////////
                return "error : judgeConclusion null";
            }

            // send the previously created HL7 message over the connection established
            Parser parser = context.getPipeParser();
            String message = parser.encode(builder.getOruR01());
            Log.e("sendResult_HL7_MLLP", "Sending message:" + "\n" + message);
//            Message response = initiator.sendAndReceive(builder.getOruR01());

            // display the message response received from the remote party
//            String responseString = parser.encode(response);
//            Log.e("sendResult_HL7_MLLP", "Received response:\n" + responseString);
//
//            if(responseString.contains("ack")){
//                Log.e("sendResult_HL7_MLLP", "send oru1 succeed\n");
//            }

//            connection.close();

            byte[] mllpData = wrapMLLP(message);
            out.write(mllpData);
            out.flush();

            // 接收响应（可选）
            InputStream in = socket.getInputStream();
            byte[] buffer = new byte[1024];
            int bytesRead = in.read(buffer);
            String response = unwrapMLLP(buffer,bytesRead);
            Log.d("HL7", "Response: " + response);
            if(response.contains("ack")){
                Log.e("sendResult_HL7_MLLP", "send oru1 succeed\n");
            }
            if(judgeConclusion.getCtRuleConclusions() != null && judgeConclusion.getCtRuleConclusions().size() != 0){
                tupeNo = judgeConclusion.getCtRuleConclusions().get(i).getTupeNo();
            }

            return "succeed" +" " + tupeNo;

        } catch (Exception e) {
            Log.e("sendResult_HL7_MLLP error ", e.getMessage());
            return "error:" + "" + tupeNo + "" +  e.getMessage();
        }
    }

    // MLLP 封装
    public static byte[] wrapMLLP(String hl7Message) {
        byte[] messageBytes = hl7Message.getBytes(StandardCharsets.UTF_8);
        byte[] wrapped = new byte[messageBytes.length + 3];
        wrapped[0] = 0x0B; // SB
        System.arraycopy(messageBytes, 0, wrapped, 1, messageBytes.length);
        wrapped[wrapped.length - 2] = 0x1C; // EB
        wrapped[wrapped.length - 1] = 0x0D;  // CR
        return wrapped;
    }

    // MLLP 解封装
    public static String unwrapMLLP(byte[] data,int length) {
        if (data.length < 3) return null;
        return new String(data, 1, length - 3, StandardCharsets.UTF_8);
    }

    public static void sendHL7Message(String serverIp, int port, String hl7Message) {
        new Thread(() -> {
            try (Socket socket = new Socket(serverIp, port);
                 OutputStream out = socket.getOutputStream()) {

                byte[] mllpData = wrapMLLP(hl7Message);
                out.write(mllpData);
                out.flush();

                // 接收响应（可选）
                InputStream in = socket.getInputStream();
                byte[] buffer = new byte[1024];
                int bytesRead = in.read(buffer);
                String response = unwrapMLLP(buffer,bytesRead);
                Log.d("HL7", "Response: " + response);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

}