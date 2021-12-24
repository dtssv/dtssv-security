package com.dtssv.security.mybatis.config;



import com.dtssv.security.mybatis.handler.BaseSecurityHandler;
import com.google.common.collect.Lists;
import lombok.Data;

import java.util.List;

/**
 * SecurityInterceptorConfig
 * @author dtssv
 * @date 2020/11/23 15:07
 * @return
 **/
@Data
public class SecurityInterceptorConfig {
    /**
     * securityHandler
     */
    private BaseSecurityHandler securityHandler;
    /**
     * 使用目标值覆盖原值 包括where条件和select返回值
     */
    private boolean overwriteSourceByTarget = false;
    /**
     * 是否写入源字段
     */
    private boolean writeSource = false;

    /**
     * enable
     */
    private boolean enable = false;

    /**
     * 加密字段配置
     */
    List<SecurityColumnConfig> securityColumnConfig = Lists.newArrayList();

    /**
     * mappedStatementIdPrefix
     */
    private List<String> mappedStatementIdPrefix = Lists.newArrayList();


}
