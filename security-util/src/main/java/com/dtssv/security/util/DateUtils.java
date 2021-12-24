package com.dtssv.security.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Objects;

import static java.util.Calendar.DATE;

/**
 * @author dtssv
 * @version 1.0
 * @program DateUtils
 * @date 2020/11/24 16:55
 * @Description
 * @since 1.0
 */
public class DateUtils {

    /**
     * 年-月-日 时:分:秒 显示格式
     */
    // 备注:如果使用大写HH标识使用24小时显示格式,如果使用小写hh就表示使用12小时制格式。
    public static String DATE_TO_STRING_DETAIAL_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * 年-月-日 显示格式
     */
    public static String DATE_TO_STRING_SHORT_PATTERN = "yyyy-MM-dd";

    /**
     * eightClock HH:mm:ss
     */
    public static String EIGHT_CLOCK_SUFFIX = " 08:00:00";

    /**
     * yyyyMMddHHmmssSSS格式
     */
    public static String YYYYMMDDHHMMSSSSS_FORMAT = "yyyyMMddHHmmssSSS";

    /**
     * yyyyMM格式
     */
    public static final String DATE_PATTENT_MONTH = "yyyyMM";

    /**
     * 时间相减得到天数
     * 不写common端工具类里，减少PRC调用
     *
     * @param beginDate 开始日期
     * @param endDate   结束日期
     */
    public static long getDaySub(Date beginDate, Date endDate) {
        if (beginDate == null || endDate == null) {
            return 0;
        }
        return (endDate.getTime() - beginDate.getTime()) / (24 * 60 * 60 * 1000);
    }

    /**
     * 时间相减得到月数
     * 不写common端工具类里，减少PRC调用
     *
     * @param beginDate 开始日期
     * @param endDate   结束日期
     */
    public static int getMonthSub(Date beginDate, Date endDate) {
        if (beginDate == null || endDate == null) {
            return 0;
        }
        return (int) (endDate.getTime() - beginDate.getTime()) / (1000 * 60 * 60 * 24 * 30);
    }

    /**
     * 字符串转换为对应日期(可能会报错异常)
     *
     * @param source  source
     * @param pattern pattern
     * @return Date
     */
    public static Date stringToDate(String source, String pattern) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        Date date = null;
        try {
            date = simpleDateFormat.parse(source);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

    /**
     * Date类型转为指定格式的String类型
     *
     * @param source  source
     * @param pattern pattern
     * @return 指定格式的String类型
     */
    public static String dateToString(Date source, String pattern) {
        if (source == null) {
            return null;
        }
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        return simpleDateFormat.format(source);
    }

    /**
     * 功能描述：返回月
     *
     * @param date Date 日期
     * @return 返回月份
     */
    public static int getMonth(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.MONTH) + 1;
    }

    /**
     * 功能描述：返回月
     *
     * @param date Date 日期
     * @return 返回月份
     */
    public static int getYear(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.YEAR);
    }

    /**
     * 功能描述：返回日期
     *
     * @param date Date 日期
     * @return 返回日份
     */
    public static int getDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.DAY_OF_MONTH);
    }

    /**
     * 获取指定日期的第二天
     *
     * @param currentDate 指定日期
     * @return 指定日期的第二天
     */
    public static int getTomorrowDay(Date currentDate) {
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(currentDate);
        //把日期往后增加一天.整数往后推,负数往前移动
        calendar.add(DATE, 1);
        return calendar.get(Calendar.DAY_OF_MONTH);
    }

    /**
     * 获取当前时间8点的Date
     *
     * @param currentDate 当前时间
     * @return 当前时间8点的Date
     */
    public static Date getTodayEightClock(Date currentDate) {
        String prefixDate = dateToString(currentDate, DATE_TO_STRING_SHORT_PATTERN);
        return stringToDate(prefixDate + EIGHT_CLOCK_SUFFIX, DATE_TO_STRING_DETAIAL_PATTERN);
    }

    /**
     * 获取当前日期昨天8点的Date
     *
     * @param currentDate 当前日期
     * @return 当前日期昨天8点的Date
     */
    public static Date getYesterdayEightClock(Date currentDate) {
        Date todayEightClock = getTodayEightClock(currentDate);
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(todayEightClock);
        calendar.add(Calendar.DATE, -1);
        return calendar.getTime();
    }

    /**
     * 功能描述：返回小时
     *
     * @param date 日期
     * @return 返回小时
     */
    public static int getHour(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.HOUR_OF_DAY);
    }

    /**
     * 功能描述：返回分
     *
     * @param date 日期
     * @return 返回分钟
     */
    public static int getMinute(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.MINUTE);
    }

    /**
     * 返回秒钟
     *
     * @param date Date 日期
     * @return 返回秒钟
     */
    public static int getSecond(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.SECOND);
    }

    /**
     * 功能描述：返回毫
     *
     * @param date 日期
     * @return 返回毫
     */
    public static long getMillis(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.getTimeInMillis();
    }


    /**
     * 返回某一天最后时刻的日期
     *
     * @param date
     * @return
     */
    public static Date toLastDateTime(Date date) {
        if (date == null) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    /**
     * 解析时间戳
     *
     * @param timeStamp
     * @return
     */
    public static Date parseTimeStamp(Long timeStamp) {
        if (Objects.isNull(timeStamp)) {
            return null;
        }
        Date date = null;
        try {
            date = new Date(timeStamp);
        } catch (Exception e) {

        }
        return date;
    }

    /**
     * local2date
     * @author dtssv
     * @date 2020/8/18 17:03
     * @param localDate
     * @return java.util.Date
     **/
    public static Date localDate2Date(LocalDate localDate){
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    /**
     *
     * @author dtssv
     * @date 2020/10/12 9:10
     * @param date
     * @return java.time.LocalDate
     **/
    public static LocalDate date2LocalDate(Date date){
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /**
     * local2date
     * @author dtssv
     * @date 2020/8/18 17:03
     * @param localDateTime
     * @return java.util.Date
     **/
    public static Date localDateTime2Date(LocalDateTime localDateTime){
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    /**
     *
     * @author dtssv
     * @date 2020/10/12 9:10
     * @param date
     * @return java.time.LocalDateTime
     **/
    public static LocalDateTime date2LocalDateTime(Date date){
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}
