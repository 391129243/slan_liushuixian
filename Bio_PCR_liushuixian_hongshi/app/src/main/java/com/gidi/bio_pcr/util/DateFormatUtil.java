package com.gidi.bio_pcr.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;


/**日期格式化工具**/
public class DateFormatUtil {
	private static SimpleDateFormat fullsdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private static SimpleDateFormat year_month_day_sdf = new SimpleDateFormat("yyyy-MM-dd");

	public static String getCurrDate(){
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.CHINA);
		String currDate = sdf.format(new Date());
		return currDate;
	}
	
	/**使用格林威治时间**/
	public static String getSysFullTime(){
		//TimeZone timeZone = TimeZone.getTimeZone("GMT+00:00");
		//sdf.setTimeZone(timeZone);
		return sdf.format(System.currentTimeMillis());
	}
	
		
	public static String getFileName(){
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.CHINA);
		return sdf.format(System.currentTimeMillis());
	}
	
	/**时间显示中的日期**/
	public static String getNowDate(){
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
		return sdf.format(System.currentTimeMillis());
	}
	public static String getNowDate_2(){
		SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd", Locale.CHINA);
		return sdf.format(System.currentTimeMillis());
	}
	/***显示时间中的分秒**/
	public static String getNowTime(){
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.CHINA);
		return sdf.format(System.currentTimeMillis());
	}
	
	public static String getNowTimeHM(){
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.CHINA);
		return sdf.format(System.currentTimeMillis());
	}

	public static String getDateHMS(String time){
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.CHINA);
		String subDateStrinng = null;
		try {
			Date timeDate = fullsdf.parse(time) ;
			subDateStrinng = sdf.format(timeDate);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return subDateStrinng;
		
	}



	public static String getDate(String date){
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
		String subDateStrinng = null;
		try {
			Date Date = fullsdf.parse(date) ;
			subDateStrinng = sdf.format(Date);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return subDateStrinng;		
	}
	
	public static String formatTime(long time){
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));
		String hms = sdf.format(new Date(time));
		return hms;
	}

	public static String formatHMS(long time){
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));
		String hms = sdf.format(time);
		return hms;
	}
	
	public static String formatFullDate(long time){
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
		return sdf.format(new Date(time));
	}
	
	public static String formatDate(long time){
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
		String subDateStrinng = sdf.format(new Date(time));
		return subDateStrinng;		
	}
	
	public static int getYear(String date){
		year_month_day_sdf.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));
		int year = 0;
		try {
			Date mDate = year_month_day_sdf.parse(date) ;
			Calendar calendar= Calendar.getInstance();

			calendar.setTime(mDate);
			year = calendar.get(Calendar.YEAR);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return year;		
	}
	
	public static int getMonth(String date){
		year_month_day_sdf.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));
		int month = 0;
		try {
			Date mDate = year_month_day_sdf.parse(date) ;
			Calendar calendar= Calendar.getInstance();

			calendar.setTime(mDate);
			month = calendar.get(Calendar.MONTH);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return month;		
	}
	
	public static int getDay(String date){
		year_month_day_sdf.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));
		int day = 0;
		try {
			Date mDate = year_month_day_sdf.parse(date) ;
			Calendar calendar= Calendar.getInstance();

			calendar.setTime(mDate);
			day = calendar.get(Calendar.DAY_OF_MONTH);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return day;		
	}
	
	/***获取当前时区***/
	public static String getTimeZone(){
		TimeZone mTimeZone = TimeZone.getDefault();
		String timezone = mTimeZone.getDisplayName(false, TimeZone.LONG);
		return timezone;
	}
	
	/**设置时区**/	  
	public static void setTimeZone(String timeZone){
	  Calendar calendar = Calendar.getInstance();
	  TimeZone mTimeZone = TimeZone.getTimeZone(timeZone);
	  calendar.setTimeZone(mTimeZone);	  
	}
	
	/**将原来时区的时间更改成目标时区的时间**/
	 public static String timeConvert(String sourceTime, String sourceId,
                                      String targetId, String reFormat){
         //校验入参是否合法
         if (null == sourceId || "".equals(sourceId) || null == targetId
                 || "".equals(targetId) || null == sourceTime
                 || "".equals(sourceTime)){
             return null;
         }
         
         if(StringUtil.isEmpty(reFormat)){
        	 reFormat = "yyyy-MM-dd HH:mm:ss";
         }
         
         //校验 时间格式必须为：yyyy-MM-dd HH:mm:ss
         String reg = "^[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}$";
         if (!sourceTime.matches(reg)){
             return null;
         }
         
         try{
             //时间格式
             SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
             //根据入参原时区id，获取对应的timezone对象
             TimeZone sourceTimeZone = TimeZone.getTimeZone(sourceId);
             //设置SimpleDateFormat时区为原时区（否则是本地默认时区），目的:用来将字符串sourceTime转化成原时区对应的date对象
             df.setTimeZone(sourceTimeZone);
             //将字符串sourceTime转化成原时区对应的date对象
             Date sourceDate = df.parse(sourceTime);
             
             //开始转化时区：根据目标时区id设置目标TimeZone
             TimeZone targetTimeZone = TimeZone.getTimeZone(targetId);
             //设置SimpleDateFormat时区为目标时区（否则是本地默认时区），目的:用来将字符串sourceTime转化成目标时区对应的date对象
             df.setTimeZone(targetTimeZone);
             //得到目标时间字符串
             String targetTime = df.format(sourceDate);
             
             SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
             Date date = sdf.parse(targetTime);
             sdf = new SimpleDateFormat(reFormat);
             
             return sdf.format(date);
         }
         catch (ParseException e){
             e.printStackTrace();
         }
         return null;
     }

	 /**
	  * 判断两个日期的大小
	  */
	 public static boolean compare_date(String date1, String date2){
		  SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		  try {
			Date dt1 = df.parse(date1);
			Date dt2 = df.parse(date2);
			if(dt1.getTime() <= dt2.getTime()){
				return true;
			}else{
				return false;
			}
		} catch (Exception e) {
			// TODO: handle exception
			return false;
		}
		  
	 }

	public static boolean compareDate(String endDate,String nowDate){
		if(!StringUtil.isEmpty(endDate)&& !StringUtil.isEmpty(nowDate)){
			int EndDate = StringUtil.convertToInt(endDate,0);
			int NowDate = StringUtil.convertToInt(nowDate,0);
			if(EndDate > NowDate){
				return true;
			}
			return false;
		}
		return false;
	}

	/**
	 * ]秒转化成时分秒
	 */
	public static String secondToTime(long second){
		String time = "";
		if(second <0){
			return "00:00";
		}
		long h = second/3600;
		long m = (second%3600)/60;
		long s = second%60;
		if(h>0){
			time = (h<10?("0"+h):h)+":";
		}else{
			time = "00:";
		}
		time += (m<10?("0"+m):m)+":";
		time += (s<10?("0"+s):s);
		return time;
	}

	public static String secondToMS(int second){
		String time = "";
		if(second <0){
			return "00:00";
		}
		long m = (second%3600)/60;
		long s = second%60;
		time += (m<10?("0"+m):m)+":";
		time += (s<10?("0"+s):s);
		return time;
	}

	/**
	 * 更改时间格式
	 */
	public static String convertDateFormat(String date){
		SimpleDateFormat after_sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
		SimpleDateFormat before_sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		try {
			Date beforeDate = before_sdf.parse(date);
			String afterDate = after_sdf.format(beforeDate);
			return afterDate;
		}catch (Exception e){
			return date;
		}
	}

	/**
	 * 计算两个日期之间的天数
	 */
	//public void
	public static int getDaysBetweenDay(String end_day1,String start_day2){
		DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		try{
			Date end_date_1 = format.parse(end_day1);
			Date start_date_2 = format.parse(start_day2);
			Calendar fromCalendar = Calendar.getInstance();
			fromCalendar.setTime(start_date_2);
			fromCalendar.set(Calendar.HOUR_OF_DAY, 0);
			fromCalendar.set(Calendar.MINUTE, 0);
			fromCalendar.set(Calendar.SECOND, 0);
			fromCalendar.set(Calendar.MILLISECOND, 0);

			Calendar toCalendar = Calendar.getInstance();
			toCalendar.setTime(end_date_1);
			toCalendar.set(Calendar.HOUR_OF_DAY, 0);
			toCalendar.set(Calendar.MINUTE, 0);
			toCalendar.set(Calendar.SECOND, 0);
			toCalendar.set(Calendar.MILLISECOND, 0);

			return (int) ((toCalendar.getTime().getTime() - fromCalendar.getTime().getTime()) / (1000 * 60 * 60 * 24));
		}catch (Exception e){

		}
		return -1;
	}

	public static String secondToYearMonthDayTime(long second){
		String time = "";
		if(second <0){
			return "00-00-00 00:00:00";
		}else if(second == 0){
			return "00-00-00 00:00:00";
		}else {
			long years = second/(365*24*60*60);
			long months = (second%(365*24*60*60))/(30*24*60*60);
			long days =((second%(365*24*60*60))%(30*24*60*60))/(24*60*60);
			long hours  = (((second%(365*24*60*60))%(30*24*60*60))%(24*60*60))/(3600);
			long minutes = ((((second%(365*24*60*60))%(30*24*60*60))%(24*60*60))%(3600))/60;
			long seconds = ((((second%(365*24*60*60))%(30*24*60*60))%(24*60*60))%(3600))%60;
			time +=(years<10?("0"+years):years)+"-";
			time +=(months<10?("0"+months):months)+"-";
			time +=(days<10?("0"+days):days)+" ";
			time += (hours<10?("0"+hours):hours)+":";
			time += (minutes<10?("0"+minutes):minutes)+":";
			time += (seconds<10?("0"+seconds):seconds);

		}
		return time;
	}

}
