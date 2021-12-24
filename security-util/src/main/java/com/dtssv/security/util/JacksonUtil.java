package com.dtssv.security.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * @author dtssv
 * @version 1.0
 * @program .JacksonUtil
 * @date 2020/11/12 10:21
 * @Description
 * @since 1.0
 */
@Slf4j
public class JacksonUtil {
    /**
     * OBJECT_MAPPER
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    /**
     * DATE_PATTERN
     */
    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    static {
        //对象的所有字段全部写入
        OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.ALWAYS);
        //取消默认转换timestamps
        OBJECT_MAPPER.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,Boolean.FALSE);
        //忽略空bean转json错误
        OBJECT_MAPPER.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS,Boolean.FALSE);
        //时间格式化表达式
        OBJECT_MAPPER.setDateFormat(new SimpleDateFormat(DATE_PATTERN));
        //忽略序列化在bean中不存在的错误
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,Boolean.FALSE);
        //localdate序列化反序列化
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DATE_PATTERN)));
        javaTimeModule.addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        javaTimeModule.addSerializer(LocalTime.class, new LocalTimeSerializer(DateTimeFormatter.ofPattern("HH:mm:ss")));
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(DATE_PATTERN)));
        javaTimeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        javaTimeModule.addDeserializer(LocalTime.class, new LocalTimeDeserializer(DateTimeFormatter.ofPattern("HH:mm:ss")));
        javaTimeModule.addDeserializer(Date.class,new CustomJsonDateDeserializer());
        OBJECT_MAPPER.registerModule(javaTimeModule);
    }
    /**
     *
     * @author dtssv
     * @date 2020/10/9 16:39
     * @param object
     * @return java.lang.String
     **/
    public static String toJsonString(Object object){
        if(object == null){
            return null;
        }
        try {
            return object instanceof String ? (String) object : OBJECT_MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("jackson 转json失败");
            return null;
        }
    }
    /**
     *
     * @author dtssv
     * @date 2020/10/9 17:07
     * @param object
     * @return java.lang.String
     **/
    public static String toJsonStringPretty(Object object){
        if(object == null){
            return null;
        }
        try {
            return object instanceof String ? (String) object : OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("jackson 转json失败");
            return null;
        }
    }
    /**
     * @author dtssv
     * @description 反序列化需要无参构造函数,不能反序列化集合
     * @date 2020/7/14 20:25
     **/
    public static <T> T toJavaObject(String json, Class<T> clazz){
        return parseObject(json, clazz);
    }
    /**
     * @author dtssv
     * @description 反序列化需要无参构造函数,不能反序列化集合
     * @date 2020/7/14 20:25
     **/
    public static <T> T parseObject(String json,Class<T> clazz){
        if(Strings.isNullOrEmpty(json) || clazz == null){
            return null;
        }
        try {
            return clazz.equals(String.class) ? (T) json : OBJECT_MAPPER.readValue(json,clazz);
        } catch (JsonProcessingException e) {
            log.error("jackson 转javaObject失败",e);
            return null;
        }
    }
    /**
     * @author dtssv
     * @description 反序列化需要无参构造函数
     * @date 2020/7/14 20:25
     **/
    public static <T> T toJavaObject(String json, TypeReference<T> clazz){
        return parseObject(json,clazz);
    }
    /**
     * @author dtssv
     * @description 反序列化需要无参构造函数
     * @date 2020/7/14 20:25
     **/
    public static <T> T parseObject(String json, TypeReference<T> clazz){
        if(Strings.isNullOrEmpty(json) || clazz == null){
            return null;
        }
        try {
            return clazz.getType().equals(String.class) ? (T) json : OBJECT_MAPPER.readValue(json,clazz);
        } catch (JsonProcessingException e) {
            log.error("jackson 转javaObject失败");
            return null;
        }
    }
    /**
     * @author dtssv
     * @description 反序列化需要无参构造函数
     * @date 2020/7/14 20:25
     **/
    public static <T> T toJavaObject(String json, Class<?> collectionClazz, Class<?>... elementClazz){
        return parseObject(json, collectionClazz, elementClazz);
    }
    /**
     * @author dtssv
     * @description 反序列化需要无参构造函数
     * @date 2020/7/14 20:25
     **/
    public static <T> T parseObject(String json, Class<?> collectionClazz, Class<?>... elementClazz){
        JavaType javaType = OBJECT_MAPPER.getTypeFactory().constructParametricType(collectionClazz,elementClazz);
        try {
            return OBJECT_MAPPER.readValue(json,javaType);
        } catch (JsonProcessingException e) {
            log.error("jackson 转javaObject失败");
            return null;
        }
    }
}
