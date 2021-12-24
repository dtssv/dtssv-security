package com.dtssv.security.mybatis.interceptor;

import com.dtssv.security.mybatis.config.SecurityColumnConfig;
import com.dtssv.security.mybatis.config.SecurityInterceptorConfig;
import com.dtssv.security.mybatis.handler.BaseSecurityHandler;
import com.dtssv.security.mybatis.sqlprocessor.*;
import com.dtssv.security.util.CommonConstance;
import com.dtssv.security.util.JacksonUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Statement;
import java.util.*;

/**
 * @author dtssv
 * @version 1.0
 * @program SecurityInterceptor
 * @date 2020/11/23 15:01
 * @Description
 * @since 1.0
 */
@Data
@Slf4j
@Intercepts({
        @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class,Integer.class})
        , @Signature(type = StatementHandler.class, method = "query", args = {Statement.class,ResultHandler.class})
})
public class SecurityInterceptor implements Interceptor {

    /**
     * 表和字段的映射关系
     */
    private Map<String, Map<String, SecurityColumnConfig>> tableColumnMap = Maps.newHashMap();

    /**
     * 用于辅助进行反射的字段缓存
     */
    private Field additionalParametersField;
    /**
     * 加密拦截器配置
     */
    private SecurityInterceptorConfig securityInterceptorConfig;


    /**
     * columnPropertyMapThreadLocal
     */
    private ThreadLocal<Map> columnPropertyMapThreadLocal = new ThreadLocal<Map>();
    /**
     *
     */
    public SecurityInterceptor() {
        try {
            //反射获取 BoundSql 中的 additionalParameters 属性
            additionalParametersField = BoundSql.class.getDeclaredField("additionalParameters");
            additionalParametersField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e.getMessage());
        }
    }
    /**
     * 
     * @author dtssv
     * @date 2020/12/9 17:14
     * @param target 
     * @return java.lang.Object
     **/
    private Object getSuperTarget(Object target){
        if(Proxy.isProxyClass(target.getClass())){
            try {
                Field field = target.getClass().getSuperclass().getDeclaredField("h");
                field.setAccessible(true);
                //获取指定对象中此字段的值
                Object invocationProxy = field.get(target); //获取Proxy对象中的此字段的值
                Field invocation = invocationProxy.getClass().getDeclaredField("target");
                invocation.setAccessible(true);
                return getSuperTarget(invocation.get(invocationProxy));
            } catch (Exception e) {
               return target;
            }
        }else if(ClassUtils.isCglibProxy(target)){
            try {
                Field h = target.getClass().getDeclaredField("CGLIB$CALLBACK_0");
                h.setAccessible(true);
                Object invocationProxy = h.get(target);
                Field advised = invocationProxy.getClass().getDeclaredField("advised");
                advised.setAccessible(true);
                return getSuperTarget(advised.get(invocationProxy));
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return target;
    }
    /**
     * 
     * @author dtssv
     * @date 2020/11/24 17:08
     * @param invocation 
     * @return java.lang.Object
     **/
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        try {
            // 如果没有需要处理的字段 那么直接执行
            if (tableColumnMap == null || tableColumnMap.isEmpty()) {
                return invocation.proceed();
            }
            StatementHandler statementHandler;
            Object target = invocation.getTarget();
            target = getSuperTarget(target);
            if(target != null && target instanceof StatementHandler){
                statementHandler = (StatementHandler) target;
            }else{
                return invocation.proceed();
            }
            MetaObject metaObject = SystemMetaObject.forObject(statementHandler);
            /*
             先拦截到RoutingStatementHandler，里面有个StatementHandler类型的delegate变量，
             其实现类是BaseStatementHandler，然后就到BaseStatementHandler的成员变量mappedStatement
             */
            // 获取ms对象
            Object mappedStatement = metaObject.getValue("delegate.mappedStatement");
            MappedStatement ms;
            if(mappedStatement != null && mappedStatement instanceof MappedStatement) {
                ms = (MappedStatement) mappedStatement;
            }else{
                return invocation.proceed();
            }
            List<String> mappedStatementIdPrefix = securityInterceptorConfig.getMappedStatementIdPrefix();
            String id = ms.getId();
            // 判断是否是需要处理的mapper
            boolean needProcess = false;
            for (String statementIdPrefix : mappedStatementIdPrefix) {
                if (id.startsWith(statementIdPrefix)) {
                    needProcess = true;
                    break;
                }
            }
            // 如果不是需要处理的mapper
            if (!needProcess) {
                return invocation.proceed();
            }
            Object config = metaObject.getValue("delegate.configuration");
            Configuration configuration;
            if(config != null && config instanceof Configuration) {
                configuration = (Configuration) config;
            }else {
                return invocation.proceed();
            }
            // 如果是预处理方法 进行sql处理
            if (CommonConstance.PREPARE.equals(invocation.getMethod().getName())) {
                BoundSql boundSql = statementHandler.getBoundSql();
                BaseSqlProcessor baseSqlProcessor = null;
                switch (ms.getSqlCommandType()) {
                    case INSERT:
                        baseSqlProcessor = new InsertBaseSqlProcessor(boundSql.getSql(), ms);
                        break;
                    case UPDATE:
                        baseSqlProcessor = new UpdateBaseSqlProcessor(boundSql.getSql(), ms);
                        break;
                    case SELECT:
                        baseSqlProcessor = new SelectBaseSqlProcessor(boundSql.getSql(), ms);
                        break;
                    case DELETE:
                        baseSqlProcessor = new DeleteBaseSqlProcessor(boundSql.getSql(), ms);
                    default:
                        break;
                }
                baseSqlProcessor.setBaseSecurityHandler(securityInterceptorConfig.getSecurityHandler());
                baseSqlProcessor.setOverwriteSourceByTarget(securityInterceptorConfig.isOverwriteSourceByTarget());
                baseSqlProcessor.setWriteSource(securityInterceptorConfig.isWriteSource());
                if (baseSqlProcessor == null) {
                    return invocation.proceed();
                }
                // 获取到本次sql涉及的表
                List<String> tablesNames = baseSqlProcessor.getTablesNames();
                // 获取到需要处理的表
                Set<String> needProcessTables = tableColumnMap.keySet();

                Map<String, SecurityColumnConfig> sourceTargetMap = Maps.newHashMap();
                needProcess = false;
                for (String tablesName : tablesNames) {
                    if (needProcessTables.contains(tablesName)) {
                        needProcess = true;
                        sourceTargetMap.putAll(tableColumnMap.get(tablesName));
                    }
                }
                if (!needProcess) {
                    return invocation.proceed();
                }

                // 创建一个新的boundsql来进行处理 防止处理后影响其他的逻辑
                Map<String, Object> additionalParameters = (Map<String, Object>) additionalParametersField.get(boundSql);
                List<ParameterMapping> newParameterMappings = Lists.newArrayList();
                newParameterMappings.addAll(boundSql.getParameterMappings());
                BoundSql newBoundSql = new BoundSql(ms.getConfiguration(), boundSql.getSql(), newParameterMappings, boundSql.getParameterObject());
                for (String key : additionalParameters.keySet()) {
                    newBoundSql.setAdditionalParameter(key, additionalParameters.get(key));
                }
                baseSqlProcessor.process(newBoundSql, sourceTargetMap);
                if (baseSqlProcessor instanceof SelectBaseSqlProcessor) {
                    columnPropertyMapThreadLocal.set(((SelectBaseSqlProcessor) baseSqlProcessor).getColumnPropertyMap());
                }
                ParameterHandler parameterHandler = configuration.newParameterHandler(ms, boundSql.getParameterObject(), newBoundSql);
                metaObject.setValue("delegate.boundSql", newBoundSql);
                metaObject.setValue("delegate.parameterHandler", parameterHandler);

                return invocation.proceed();

            } else {
                // 如果是查询操作 进行数据的解密映射
                Object result = invocation.proceed();
                Map<String, String> columnPropertyMap = columnPropertyMapThreadLocal.get();
                if(columnPropertyMap == null){
                    columnPropertyMap = Maps.newHashMap();
                    columnPropertyMapThreadLocal.set(columnPropertyMap);
                }
                if (columnPropertyMap != null && columnPropertyMap.size() > 0 && result instanceof List) {
                    List resultArr = (List) result;
                    Collection<String> properties = columnPropertyMap.values();
                    for (Object o : resultArr) {
                        MetaObject mo = configuration.newMetaObject(o);
                        for (String pro : properties) {
                            String property = mo.findProperty(pro, configuration.isMapUnderscoreToCamelCase());
                            Object value = mo.getValue(property);
                            if (value != null) {
                                String decrypt = securityInterceptorConfig.getSecurityHandler().decrypt(value.toString());
                                mo.setValue(property, decrypt);
                            }
                        }
                    }
                }
                return result;
            }
        }catch (Exception e){
            return invocation.proceed();
        }
    }
    @Override
    public Object plugin(Object target) {
        if(securityInterceptorConfig.isEnable()) {
            return Plugin.wrap(target, this);
        }else {
            return target;
        }
    }

    @Override
    public void setProperties(Properties properties) {
        SecurityInterceptorConfig securityInterceptorConfig = this.securityInterceptorConfig;
        if(securityInterceptorConfig == null){
            securityInterceptorConfig = new SecurityInterceptorConfig();
        }
        try {
            String enable = properties.getProperty("enable");
            if(!Strings.isNullOrEmpty(enable)){
                securityInterceptorConfig.setEnable(Boolean.parseBoolean(enable));
            }
        } catch (Exception e) {
            log.error("security[enable] error,use default value");
        }
        try {
            String overwriteSourceByTarget = properties.getProperty("overwriteSourceByTarget");
            if(!Strings.isNullOrEmpty(overwriteSourceByTarget)){
                securityInterceptorConfig.setOverwriteSourceByTarget(Boolean.parseBoolean(overwriteSourceByTarget));
            }
        } catch (Exception e) {
            log.error("security[overwriteSourceByTarget] error,use default value");
        }
        try {
            String writeSource = properties.getProperty("writeSource");
            if(!Strings.isNullOrEmpty(writeSource)){
                securityInterceptorConfig.setWriteSource(Boolean.parseBoolean(writeSource));
            }
        } catch (Exception e) {
            log.error("security[writeSource] error,use default value");
        }
        try {
            String handleClass = properties.getProperty("handleClass");
            Class<?> clazz = Class.forName(handleClass);
            Object handleInstance = clazz.newInstance();
            if(handleInstance instanceof BaseSecurityHandler){
                BaseSecurityHandler baseSecurityHandler = (BaseSecurityHandler) handleInstance;
                baseSecurityHandler.setProperties(properties);
                securityInterceptorConfig.setSecurityHandler(baseSecurityHandler);
            }else{
                throw new RuntimeException("security handleClass empty");
            }
        } catch (Exception e) {
            throw new RuntimeException("security handleClass empty");
        }
        String mappedStatementIdPrefix = properties.getProperty("mappedStatementIdPrefix");
        if(!Strings.isNullOrEmpty(mappedStatementIdPrefix)){
            List<String> mappedStatementIds = JacksonUtil.toJavaObject(mappedStatementIdPrefix, new TypeReference<List<String>>() {});
            securityInterceptorConfig.getMappedStatementIdPrefix().removeAll(mappedStatementIds);
            securityInterceptorConfig.getMappedStatementIdPrefix().addAll(mappedStatementIds);
        }
        String securityColumnConfig = properties.getProperty("securityColumnConfig");
        if(!Strings.isNullOrEmpty(securityColumnConfig)){
            List<SecurityColumnConfig> securityColumnConfigList = JacksonUtil.parseObject(securityColumnConfig, new TypeReference<List<SecurityColumnConfig>>() {});
            if(!CollectionUtils.isEmpty(securityColumnConfigList)){
                for (SecurityColumnConfig columnConfig : securityColumnConfigList) {
                    securityInterceptorConfig.getSecurityColumnConfig().add(columnConfig);
                }
            }
        }
        this.setSecurityInterceptorConfig(securityInterceptorConfig);
    }
    
    /**
     *
     * @author dtssv
     * @date 2020/11/23 16:52
     * @param securityInterceptorConfig
     * @return void
     **/
    public void setSecurityInterceptorConfig(SecurityInterceptorConfig securityInterceptorConfig) {
        this.securityInterceptorConfig = securityInterceptorConfig;
        List<SecurityColumnConfig> securityColumnConfigs = securityInterceptorConfig.getSecurityColumnConfig();
        for (SecurityColumnConfig securityColumnConfig : securityColumnConfigs) {
            String table =  securityColumnConfig.getTable();
            Map<String, SecurityColumnConfig> tempMap =  tableColumnMap.get(table);
            if(tempMap == null){
                tempMap = Maps.newHashMap();
                tableColumnMap.put(table,tempMap);
            }
            tempMap.put(securityColumnConfig.getSourceColumn(),securityColumnConfig);
        }
    }
}
