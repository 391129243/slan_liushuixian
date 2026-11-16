package com.gzlab.autoassemblyline_client.util;


import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtil {
	public static String clear(String input){
		if(isEmpty(input)){
			return input;
		}
		input = input.trim();
		input = input.replaceAll(" ","");
		return input;
	}
	/**
	 * 使用java正则表达式去掉多余的.与0
	 * 
	 * @param s
	 * @return
	 */
	public static String subZeroAndDot(String s) {
		if (s.indexOf(".") > 0) {
			s = s.replaceAll("0+?$", "");// 去掉多余的0
			s = s.replaceAll("[.]$", "");// 如最后一位是.则去掉
		}
		return s;
	}


	public static String[] splitStringAddMark(String input, String mark) {
		String[] spilteResult = null;
		spilteResult = input.split(mark);
		for(int i=0;i<spilteResult.length;i++){
			spilteResult[i] += mark;
		}
		return spilteResult;
	}

	public static String[] splitString(String input, String mark) {
		String[] spilteResult = null;
		spilteResult = input.split(mark);
		return spilteResult;
	}

	public static String[] splitString(String input, int length) {
		int size = input.length() / length;
		if (input.length() % length != 0) {
			size += 1;
		}
		String[] spilteResult = new String[size];
		for (int index = 0; index < size; index++) {
			String childStr = substring(input, index * length, (index + 1)
					* length);
			spilteResult[index] = childStr;
		}
		return spilteResult;
	}

	public static float DoubleToFloat(Double value,float defaultValue){
		if(Double.isInfinite(value)|| Double.isNaN(value)){
			return defaultValue;
		}
		try{
			Double roundedValue = roundToSignificantDigits(value, 4);
			return Float.parseFloat(roundedValue.toString());
		}catch (NumberFormatException e){
			return defaultValue;
		}catch (Exception e1) {
			// TODO: handle exception
			try {
				return Double.valueOf(value.toString()).floatValue();
			} catch (NumberFormatException e2){
				return defaultValue;
			}catch (Exception e3) {
				// TODO: handle exception
				return defaultValue;
			}
		}

	}

	public static Double roundToSignificantDigits(Double value, int significantDigits) {
		if (value == null || significantDigits <= 0) {
			return value;
		}
		BigDecimal bd = new BigDecimal(value);
		bd = bd.round(new MathContext(significantDigits, RoundingMode.HALF_EVEN));
		return bd.doubleValue();
	}



	/**
	 * 将字符串等长分割
	 * 
	 * @param input
	 * @param length
	 * @return
	 */
	public static List<String> splitStrList(String input, int length) {
		int size = input.length() / length;
		if (input.length() % length != 0) {
			size += 1;
		}
		return getStrList(input, length, size);
	}

	/**
	 * 把原始字符串分割成指定长度的字符串列表
	 * 
	 * @param inputString
	 *            原始字符串
	 * @param length
	 *            指定长度
	 * @param size
	 *            指定列表大小
	 * @return
	 */
	public static List<String> getStrList(String inputString, int length,
                                          int size) {
		List<String> list = new ArrayList<String>();
		for (int index = 0; index < size; index++) {
			String childStr = substring(inputString, index * length,
					(index + 1) * length);
			list.add(childStr);
		}
		return list;
	}

	/**
	 * 分割字符串，如果开始位置大于字符串长度，返回空
	 * 
	 * @param str
	 *            原始字符串
	 * @param f
	 *            开始位置
	 * @param t
	 *            结束位置
	 * @return
	 */
	public static String substring(String str, int f, int t) {
		if (f > str.length())
			return null;
		if (t > str.length()) {
			return str.substring(f, str.length());
		} else {
			return str.substring(f, t);
		}
	}


	public static boolean isEmpty(String input){
		if (input == null || input.isEmpty()|| input.equals("")){
			return true;
		}
		return false;
	}


	//忽略个位数
	public static int IgnoreUnities(int input){
		int ignore_result = 0;
		if(input > 0){
			if(input > 10 ){
				ignore_result = (int)input/10*10;
			}else{
				ignore_result = input;
			}
		}

		return ignore_result;
	}

	public static String md5(String currentString){
		MessageDigest md5 = null;
		try {
			md5 = MessageDigest.getInstance("MD5");
		} catch (Exception e) {
			// TODO: handle exception
			System.out.println(e.toString());
			e.printStackTrace();
			return "";
		}

		char[] charArray = currentString.toCharArray();
		byte[] byteArray = new byte[charArray.length];
		for(int i = 0; i< charArray.length; i++){
			byteArray[i] = (byte) charArray[i];
		}
		byte[] md5Bytes = md5.digest(byteArray);
		StringBuffer hexValue = new StringBuffer();
		for (int j = 0; j < md5Bytes.length; j++) {
			int val = ((int) md5Bytes[j]) & 0xff;
			if (val < 16)
				hexValue.append("0");
			hexValue.append(Integer.toHexString(val));
		}
		return hexValue.toString();

	}


	public static int convertToInt(Object value, int defaultValue){
		if(value == null || "".equals(value.toString())){
			return defaultValue;
		}
		try {
			return Integer.parseInt(value.toString());

		} catch (NumberFormatException e1){
			return defaultValue;
		}catch (Exception e) {
			// TODO: handle exception
			try {
				return Double.valueOf(value.toString()).intValue();
			} catch (NumberFormatException e2){
				return defaultValue;
			}catch (Exception e3) {
				// TODO: handle exception
				return defaultValue;
			}
		}
	}

	public static long convertToLong(Object value, long defaultValue){
		if(value == null || "".equals(value.toString())){
			return defaultValue;
		}
		try {
			return Long.parseLong(value.toString());

		} catch (NumberFormatException e1){
			return defaultValue;
		}catch (Exception e) {
			// TODO: handle exception
			try {
				return Double.valueOf(value.toString()).longValue();
			} catch (NumberFormatException e2){
				return defaultValue;
			}catch (Exception e3) {
				// TODO: handle exception
				return defaultValue;
			}
		}
	}
	public static float convertToFloat(Object value, float defaultValue){
		if(value == null || "".equals(value.toString())){
			return defaultValue;
		}
		try {
			return Float.parseFloat(value.toString());
		}  catch (NumberFormatException e){
			return defaultValue;
		}catch (Exception e1) {
			// TODO: handle exception
			try {
				return Double.valueOf(value.toString()).floatValue();
			} catch (NumberFormatException e2){
				return defaultValue;
			}catch (Exception e3) {
				// TODO: handle exception
				return defaultValue;
			}
		}
	}



	public static boolean isAllZero(String input){
		boolean result = false;
		if(input != null && !input.equals("")){
			String regEx="[0]";
			Pattern p = Pattern.compile(regEx);
			Matcher m = p.matcher(input);
			String output = m.replaceAll("");
			if(output.equals("")){
				result = true;
			}
		}
		return result;
	}



	/***
	 * 提取5个数字的，收割字符不是符号的信息
	 * 例如电池状态和仪器信息
	 */
	public static String getAllNumFromMsg(String input){
		String result = null;
		String value = null;
		if(null != input && !input.equals("")){
			input = input.substring(13);
			String regEx="[^-0-9]";
			Pattern p = Pattern.compile(regEx);
			Matcher m = p.matcher(input);
			result = m.replaceAll("");
			if(result.length()==6){
				value = result.substring(0,result.length()-1);
			}
		}
		return value;
	}

	public static boolean isDicimals(String input) {// 判断小数，与判断整型的区别在与d后面的小数点（红色）
		return input.matches("\\d+\\.\\d+$");
	}

	public static String getInteger(String input){
		String result = "";
		result = input.replaceFirst("^0*", "");
		return result;
	}

	public static String getNumOfMsg(String input){
		String result = null;
		if(null != input && !input.equals("")){
			String regEx="[^-0-9]";
			Pattern p = Pattern.compile(regEx);
			Matcher m = p.matcher(input);
			result = m.replaceAll("").trim();
		}
		return result;
	}

	public static String replaceLast(String string, String toReplace, String replacement) {
		int pos = string.lastIndexOf(toReplace);
		if (pos > -1) {
			return string.substring(0, pos)
					+ replacement
					+ string.substring(pos + toReplace.length(), string.length());
		} else {
			return string;
		}
	}
}
