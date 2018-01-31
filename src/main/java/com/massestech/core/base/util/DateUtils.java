package com.massestech.core.base.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * 日期时间操作工具类
 */
public class DateUtils {

	/**
	 * 取得当前日期时间，格式：yyyy-MM-dd HH:mm:ss
	 */
	public static String getCurrentDateTimeStr() {
		return converDateToString(Calendar.getInstance().getTime(), "yyyy-MM-dd HH:mm:ss");
	}
	
	/**
	 * 取得当前日期时间，格式自定义，例如：yyyy-MM-dd
	 */
	public static String getCurrentDateTimeStr(String style) {
		return converDateToString(Calendar.getInstance().getTime(), style);
	}

	/**
	 * 取得当前日期，格式：yyyy-MM-dd
	 */
	public static String getCurrentDateStr() {
		return converDateToString(Calendar.getInstance().getTime(), "yyyy-MM-dd");
	}

	/**
	 * 取得当前时间，格式：HH:mm:ss
	 */
	public static String getCurrentTimeStr() {
		return converDateToString(Calendar.getInstance().getTime(), "HH:mm:ss");
	}

	/**
	 * 取得当前年份
	 */
	public static int getYear() {
		return Calendar.getInstance().get(Calendar.YEAR);
	}

	/**
	 * 取得当前月份
	 */
	public static int getMonth() {
		return Calendar.getInstance().get(Calendar.MONTH) + 1;
	}

	/**
	 * 取得当前天
	 */
	public static int getDay() {
		return Calendar.getInstance().get(Calendar.DATE);
	}
	
	/**
	 * 取得本月第一天

	 */
	public static String getFirstDayOfMonth(){
		SimpleDateFormat dateFormat=new SimpleDateFormat("yyyy-MM-dd");
		Calendar calendar=Calendar.getInstance();
		calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMinimum(Calendar.DAY_OF_MONTH));
		return dateFormat.format(calendar.getTime());
	}
	
	/**
	 * 取得本月第一天

	 */
	public static String getLastDayOfMonth(){
		SimpleDateFormat dateFormat=new SimpleDateFormat("yyyy-MM-dd");
		Calendar calendar=Calendar.getInstance();
		calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
		return dateFormat.format(calendar.getTime());
	}
	
	/**
	 * 取得今年第一月

	 */
	public static String getFirstMonthOfYear(){
		return Calendar.getInstance().get(Calendar.YEAR)+"-01";
	}
	
	/**
	 * 取得今年最后一月

	 */
	public static String getLastMonthOfYear(){
		return Calendar.getInstance().get(Calendar.YEAR)+"-12";
	}
	
	
	
	/**
	 * 取得当前周
	 */
	public static int getWeek() {
		int week = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1;
		if (week == 0){
			week = 7;
		}
		return week;
	}

	/**
	 * 根据特定时间获取几周前后的日期时间
	 */
	public static Date getDiffWeek(Date date, int week) {
		Calendar c = Calendar.getInstance();
		c.setTime(date);
		c.add(Calendar.WEEK_OF_YEAR, week);
		return c.getTime();
	}

	/**
	 * 取得当前小时
	 */
	public static int getHour() {
		return Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
	}

	/**
	 * 取得当前分
	 */
	public static int getMinute() {
		return Calendar.getInstance().get(Calendar.MINUTE);
	}

	/**
	 * 取得当前秒
	 */
	public static int getSecond() {
		return Calendar.getInstance().get(Calendar.SECOND);
	}

	/**
	 * 日期字符串转换成Date,style:格式自定义

	 */
	public static Date convertStringToDate(String str, String style) {
		SimpleDateFormat formatter = new SimpleDateFormat(style);
		Date date = null;
		try {
			date = formatter.parse(str);
		} catch (ParseException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		return date;
	}

	/**
	 * 日期字符串转换成Date，格式：yyyy-MM-dd HH:mm:ss
	 */
	public static Date convertStringToDate(String str) {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date date = null;
		try {
			date = formatter.parse(str);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return date;
	}

	/**
	 * 日期转换成字符串,style:格式自定义

	 */
	public static String converDateToString(Date date, String style) {
		SimpleDateFormat formatter = new SimpleDateFormat(style);
		return formatter.format(date);
	}

	/**
	 * 日期转换成字符串，格式：yyyy-MM-dd HH:mm:ss
	 */
	public static String converDateToString(Date date) {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return formatter.format(date);
	}

	/**
	 * 日期重新格式化
	 */
	public static Date converDateToDate(Date date, String style) {
		SimpleDateFormat formatter = new SimpleDateFormat(style);
		try {
			return formatter.parse(formatter.format(date));
		} catch (ParseException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * 取得当前日期
	 */
	public static Date getCurrentDate() {
		return Calendar.getInstance().getTime();
	}

	/**
	 * 返回指定时间与当前时间差-单位为天，将时间取整后在计算
	 */
	public static int getDiffAllDayByCurrentTime(Date date) {
		Date datetime1 = converDateToDate(getCurrentDate(), "yyyy-MM-dd");
		Date datetime2 = converDateToDate(date, "yyyy-MM-dd");
		//return Math.abs((int) ((datetime1.getTime() - datetime2.getTime()) / 1000 / 60 / 60 / 24));
		return (int)((datetime1.getTime() - datetime2.getTime()) / 1000 / 60 / 60 / 24);
	}

	/**
	 * 返回开始时间与结束时间的时间差-单位为天
	 */
	public static int getDiffAllDays(Date startDate, Date endDate) {
		Date datetime1 = converDateToDate(startDate, "yyyy-MM-dd");
		Date datetime2 = converDateToDate(endDate, "yyyy-MM-dd");
		return Math.abs((int) ((datetime2.getTime() - datetime1.getTime()) / 1000 / 60 / 60 / 24));
	}

	/**
	 * 返回开始时间与结束时间的时间差-单位为小时
	 */
	public static int getDiffAllHours(Date startDate, Date endDate) {
		return Math.abs((int) ((endDate.getTime() - startDate.getTime()) / 1000 / 60 / 60));
	}

	/**
	 * 返回指定时间与当前时间差-单位为天
	 */
	public static int getDiffDayByCurrentTime(Date datetime) {
		return Math.abs((int) ((getCurrentDate().getTime() - datetime.getTime()) / 1000 / 60 / 60 / 24));
	}

	/**
	 * 返回指定时间与当前时间差-单位为秒
	 */
	public static int getDiffSecByCurrentTime(Date datetime) {
		return Math.abs((int) ((getCurrentDate().getTime() - datetime.getTime()) / 1000));
	}

	/**
	 * 返回指定时间与当前时间差-单位为分钟
	 */
	public static int getDiffMinByCurrentTime(Date datetime) {
		return Math.abs((int) ((getCurrentDate().getTime() - datetime.getTime()) / 1000 / 60));
	}

	/**
	 * 返回指定时间与当前时间差-单位为小时
	 */
	public static int getDiffHourByCurrentTime(Date datetime) {
		return Math.abs((int) ((getCurrentDate().getTime() - datetime.getTime()) / 1000 / 60 / 60));
	}
	
	/**
	 * 返回指定时间毫秒数

	 */
	public static long getTime(String datetime, String style) {
		long time = 0;
		try {
			time = convertStringToDate(datetime, style).getTime();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return time;
	}

	/**
	 * 根据millis获得当前的时间字符串格式， 格式：yyyy-MM-dd HH:mm:ss
	 */
	public static String getDateTimeStrByMillis(long millis) {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date t = new Date(millis);
		return formatter.format(t);
	}

	/**
	 * 根据millis获得当前的时间字符串格式,格式自定义

	 */
	public static String getDateTimeStrByMillis(long millis, String style) {
		SimpleDateFormat formatter = new SimpleDateFormat(style);
		Date t = new Date(millis);
		return formatter.format(t);
	}

	/**
	 * 根据当前时间获得几天前的日期时间
	 */
	public static Date getBeforeDateTime(int days) {
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DAY_OF_YEAR, 0 - days);
		return c.getTime();
	}

	/**
	 * 根据当前时间获得几小时前的日期时间
	 */
	public static Date getBeforeHourTime(int hours) {
		Calendar c = Calendar.getInstance();
		c.add(Calendar.HOUR_OF_DAY, 0 - hours);
		return c.getTime();
	}

	//根据当前时间得到几分钟前的日期时间
	public static Date getBeforeMinTime(int mins){
		Date date = new Date(new Date().getTime() - mins*60*1000);
		return date;
	}

	//根据指定时间得到几分钟前的日期时间
	public static Date getBeforeMinTime(Date date, int mins){
		return new Date(date.getTime() - mins*60*1000);
	}

	/**
	 * 根据特定时间获得其几天前的日期时间
	 */
	public static Date getBeforeDateTime(Date date, int days) {
		Calendar c = Calendar.getInstance();
		c.setTime(date);
		c.add(Calendar.DAY_OF_YEAR, 0 - days);
		return c.getTime();
	}

	/**
	 * 根据特定时间获得其几个月前或后的日期时间
	 */
	public static Date getDiffMonDateTime(Date date, int mons) {
		Calendar c = Calendar.getInstance();
		c.setTime(date);
		c.add(Calendar.MONTH, mons);
		return c.getTime();
	}

	/**
	 * 获得几天前的日期时间字符串
	 */
	public static String getBeforeDateTimeStr(int days) {
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DAY_OF_YEAR, 0 - days);
		return converDateToString(c.getTime());
	}

	/**
	 * 获得几天前的日期字符串
	 */
	public static String getBeforeDateStr(int days) {
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DAY_OF_YEAR, 0 - days);
		return converDateToString(c.getTime(), "yyyy-MM-dd");
	}

	/**
	 * 根据当前时间获得几天后的日期时间
	 */
	public static Date getAfterDateTime(int days) {
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DAY_OF_YEAR, days);
		return c.getTime();
	}

	/**
	 * 根据当前时间获得几小时后的日期时间
	 */
	public static Date getAfterHourTime(int hours) {
		Calendar c = Calendar.getInstance();
		c.add(Calendar.HOUR_OF_DAY, hours);
		return c.getTime();
	}

	//根据当前时间得到几分钟后的日期时间
	public static Date getAfterMinTime(int mins){
		Date date = new Date(new Date().getTime() + mins*60*1000);
		return date;
	}

	//根据指定时间得到几分钟后的日期时间
	public static Date getAfterMinTime(Date date, int mins){
		return new Date(date.getTime() + mins*60*1000);
	}

	/**
	 * 根据特定时间获取几天后的日期时间
	 */
	public static Date getAfterDateTime(Date date, int days) {
		Calendar c = Calendar.getInstance();
		c.setTime(date);
		c.add(Calendar.DAY_OF_YEAR, days);
		return c.getTime();
	}

	/**
	 * 获得几天后的日期时间字符串
	 */
	public static String getAfterDateTimeStr(int days) {
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DAY_OF_YEAR, days);
		return converDateToString(c.getTime());
	}

	/**
	 * 获得几天后的日期时间字符串
	 */
	public static String getAfterDateStr(int days) {
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DAY_OF_YEAR, days);
		return converDateToString(c.getTime(), "yyyy-MM-dd");
	}

	/**
	 * 获取指定日期的毫秒
	 */
	public static long getMillisOfDate(Date date) {
		Calendar c = Calendar.getInstance();
		c.setTime(date);
		return c.getTimeInMillis();
	}

	/**
	 * 两个时间差，返回秒
	 */
	public static int getDiffSecBetweenBothDate(Date beginTime, Date endTime) {
		return Math.abs((int) ((endTime.getTime() - beginTime.getTime()) / 1000));
	}

	//将指定时间转换成简单的描述 : 刚刚/1分钟前/1小时前/一天前
	public static String getDateTimeSimpleDesc(Date date) {
		int diffMinByCurrentTime = DateUtils.getDiffMinByCurrentTime(date);
		int diffHourByCurrentTime = DateUtils.getDiffHourByCurrentTime(date);
		if(diffMinByCurrentTime <= 0){
			return "刚刚";
		}else if(diffMinByCurrentTime < 60){
			return diffMinByCurrentTime + "分钟前";
		}else if(diffHourByCurrentTime < 24){
			return diffHourByCurrentTime + "小时前";
		}else {
			return DateUtils.getDiffAllDayByCurrentTime(date) + "天前";
		}
	}

}