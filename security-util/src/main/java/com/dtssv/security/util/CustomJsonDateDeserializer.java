package com.dtssv.security.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.google.common.collect.Lists;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * @version 1.0
 * @program .CustomJsonDateDeserializer
 * @author: dtssv
 * @date: 2020/10/9 16:31
 * @Description:
 * @since 1.0
 */
public class CustomJsonDateDeserializer extends JsonDeserializer<Date> {

    /**
     * formats
     */
    private static List<String> formats = Lists.newArrayList();

    static{
        formats.add("yyyy");
        formats.add("yyyy-MM");
        formats.add("yyyy-MM-dd");
        formats.add("yyyy-MM-dd HH:mm");
        formats.add("yyyy-MM-dd HH:mm:ss");
        formats.add("yyyy/MM");
        formats.add("yyyy/MM/dd");
        formats.add("yyyy/MM/dd HH:mm");
        formats.add("yyyy/MM/dd HH:mm:ss");
    }

    /**
     * YEAR_REG
     */
    public static final String YEAR_REG = "^\\d{4}$";

    @Override
    public Date deserialize(JsonParser jp, DeserializationContext dc) throws IOException, JsonProcessingException {
        String dateStr = jp.getText();
        try {
            if (dateStr.matches(YEAR_REG)) {//2017
                return parseDate(dateStr, formats.get(0));
            } else if (dateStr.matches("^\\d{4}-\\d{1,2}$")) {//2017-09
                return parseDate(dateStr, formats.get(1));
            } else if (dateStr.matches("^\\d{4}-\\d{1,2}-\\d{1,2}$")) {//2017-09-10
                return parseDate(dateStr, formats.get(2));
            } else if (dateStr.matches("^\\d{4}-\\d{1,2}-\\d{1,2} {1}\\d{1,2}:\\d{1,2}$")) {//2017-09-10 21:15
                return parseDate(dateStr, formats.get(3));
            } else if (dateStr.matches("^\\d{4}-\\d{1,2}-\\d{1,2} {1}\\d{1,2}:\\d{1,2}:\\d{1,2}$")) {//2017-09-10 21:15:30
                return parseDate(dateStr, formats.get(4));
            } else if (dateStr.matches("^\\d{4}/\\d{1,2}$")) {//2017/09
                return parseDate(dateStr, formats.get(5));
            } else if (dateStr.matches("^\\d{4}/\\d{1,2}/\\d{1,2}$")) {//2017/09/10
                return parseDate(dateStr, formats.get(6));
            } else if (dateStr.matches("^\\d{4}/\\d{1,2}/\\d{1,2} {1}\\d{1,2}:\\d{1,2}$")) {//2017/09/10 21:15
                return parseDate(dateStr, formats.get(7));
            } else if (dateStr.matches("^\\d{4}/\\d{1,2}/\\d{1,2} {1}\\d{1,2}:\\d{1,2}:\\d{1,2}$")) {//2017/09/10 21:15:30
                return parseDate(dateStr, formats.get(8));
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 功能描述：格式化日期
     *
     * @param dateStr
     *            String 字符型日期
     * @param format
     *            String 格式
     * @return Date 日期
     */
    public  Date parseDate(String dateStr, String format) {
        Date date=null;
        SimpleDateFormat dateFormat = new SimpleDateFormat(format);
        dateFormat.setLenient(false);//指定日期/时间解析为不严格
        try {
            date = dateFormat.parse(dateStr);
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return date;
    }
}
