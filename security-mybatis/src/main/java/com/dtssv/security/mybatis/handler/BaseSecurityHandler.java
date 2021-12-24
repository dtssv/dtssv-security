package com.dtssv.security.mybatis.handler;

import java.util.Properties;

/**
 * 加密处理器BaseSecurityHandler
 * @date
 * @author dtssv
 */
public interface BaseSecurityHandler {

    /**
     * 加密
     * @param parameter
     * @return
     */
    String encrypt(Object parameter);

    /**
     * 解密
     * @param parameter
     * @return
     */
    String decrypt(Object parameter);

    /**
     * 计算拆线呢索引
     * @param parameter
     * @return
     */
    String index(Object parameter);
    /**
     * 设置属性
     * @author dtssv
     * @date 2020/12/8 11:36
     * @param properties
     * @return void
     **/
    default void setProperties(Properties properties){}
}
