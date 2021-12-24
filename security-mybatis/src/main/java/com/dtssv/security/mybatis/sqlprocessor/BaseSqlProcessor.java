package com.dtssv.security.mybatis.sqlprocessor;

import com.dtssv.security.mybatis.config.SecurityColumnConfig;
import com.dtssv.security.mybatis.handler.BaseSecurityHandler;
import com.dtssv.security.util.CommonConstance;
import com.google.common.collect.Lists;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.ItemsList;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Field;
import java.util.*;

/**
 * SqlProcessor
 * @author dtssv
 * @date 2020/11/23 15:18 
 * @return 
 **/
@Data
@Slf4j
public abstract class BaseSqlProcessor<T extends Statement> {
    /**
     * 使用目标值覆盖原值 包括where条件和select返回值
     */
    private boolean overwriteSourceByTarget = true;
    /**
     * 是否写入源字段
     */
    private boolean writeSource = false;

    /**
     * boundSql 
     */
    protected BoundSql boundSql = null;
    /**
     * configuration 
     */
    protected Configuration configuration;
    /**
     * mappedStatement 
     */
    protected MappedStatement mappedStatement;
    /**
     * parameterObject 
     */
    private Object parameterObject;
    /**
     * orginalParameterMappings 
     */
    private List<ParameterMapping> orginalParameterMappings = Lists.newArrayList();
    /**
     * baseSecurityHandler 
     */
    protected BaseSecurityHandler baseSecurityHandler;
    /**
     * sourceTargetMap 
     */
    protected Map<String, SecurityColumnConfig> sourceTargetMap;
    /**
     * stmt 
     */
    private Statement stmt = null;
    /**
     * tablesNames 
     */
    private List<String> tablesNames = null;
    /**
     * 
     */
    public BaseSqlProcessor(){
        
    }

    /**
     * 
     */
    public BaseSqlProcessor(String sql, MappedStatement mappedStatement) {

        this.mappedStatement = mappedStatement;
        this.configuration = mappedStatement.getConfiguration();

        try {
            this.stmt = CCJSqlParserUtil.parse(sql);
            TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
            if(stmt instanceof Select){
                this.tablesNames = tablesNamesFinder.getTableList(stmt);
            }else if (stmt instanceof Update){
                this.tablesNames = tablesNamesFinder.getTableList(stmt);
            } else if (stmt instanceof Insert){
                this.tablesNames = tablesNamesFinder.getTableList(stmt);
            } else if (stmt instanceof Delete){
                this.tablesNames = tablesNamesFinder.getTableList(stmt);
            }
            List<String> result = new ArrayList();
            for (String tablesName : this.tablesNames) {
                String s = columnExtract(tablesName);
                result.add(s);
            }
            this.tablesNames = result;


        } catch (Throwable e) {

        }
    }
    /**
     * 
     */
    public List<String> getTablesNames() {
        return tablesNames;
    }

    /**
     * 处理信息
     * @param boundSql
     * @param sourceTargetMap
     */
    public void process(BoundSql boundSql, Map<String, SecurityColumnConfig> sourceTargetMap) {
        this.sourceTargetMap = sourceTargetMap;
        this.boundSql = boundSql;
        orginalParameterMappings.addAll(boundSql.getParameterMappings());
        parameterObject = boundSql.getParameterObject();
        log.debug("orignal:{}", stmt.toString());
        doProcess((T)stmt);
        log.debug("process:{}", stmt.toString());
        try {
            Field field = boundSql.getClass().getDeclaredField("sql");
            field.setAccessible(true);
            field.set(boundSql, stmt.toString());
        } catch (NoSuchFieldException  e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

    }
    /**
     * 
     */
    protected abstract void doProcess(T stmt);

    /**
     * 处理where表达式
     * @param expression
     */
    protected void processWhereExpression(Expression expression) {
        // 如果不是使用target 覆盖 source 那么跳过处理where
        if(!isOverwriteSourceByTarget()){
            return;
        }
        //如果表达式是"(" expression ")"类型
        if (expression instanceof Parenthesis) {
            Parenthesis parenthesis = (Parenthesis) expression;
            processWhereExpression(parenthesis.getExpression());
        } else if (expression instanceof BinaryExpression) { // 如果表达式时二分表达式 即分成了左右值
            BinaryExpression binaryExpression = (BinaryExpression) expression;
            processWhereExpression(binaryExpression.getLeftExpression(), binaryExpression.getRightExpression());
        } else if (expression instanceof Column) { // 如果 表达式是一个列
            Column column = (Column) expression;
            if (isNeedProcessColumn(Lists.newArrayList(),column)) {
                String columnName = columnExtract(column.getColumnName());
                column.setColumnName(column.getColumnName().replace(columnName, getIndexName(column)));
            }
        } else if(expression instanceof InExpression){ // 如果表达式是in
            Expression leftExpression = ((InExpression) expression).getLeftExpression();
            if(leftExpression instanceof Column){
                Column column = (Column) leftExpression;
                if (isNeedProcessColumn(Lists.newArrayList(),column)) {
                    processWhereExpression(column);
                    ItemsList rightItemsList = ((InExpression) expression).getRightItemsList();
                    if (rightItemsList instanceof ExpressionList) {
                        List<Expression> expressions = ((ExpressionList) rightItemsList).getExpressions();
                        for (Expression item : expressions) {
                            if (item instanceof JdbcParameter) {
                                JdbcParameter jdbcParameter = (JdbcParameter) item;
                                Integer index = jdbcParameter.getIndex();
                                ParameterMapping parameterMapping = orginalParameterMappings.get(index - 1);
                                String indexValue = baseSecurityHandler.index(getValueOfParameterMapping(parameterMapping));
                                replaceParams(UUID.randomUUID().toString(), indexValue, parameterMapping.getProperty());
                            }
                        }
                    }
                }
            }
        }else if(expression instanceof Function){
            ExpressionList expressionList = ((Function)expression).getParameters();
            List<Expression> expressions = expressionList.getExpressions();
            for (Expression functionExpression : expressions) {
                if(expression instanceof JdbcParameter){
                    JdbcParameter jdbcParameter = (JdbcParameter) functionExpression;
                    Integer index = jdbcParameter.getIndex();
                    ParameterMapping parameterMapping = orginalParameterMappings.get(index - 1);
                    String indexValue = baseSecurityHandler.index(getValueOfParameterMapping(parameterMapping));
                    replaceParams(UUID.randomUUID().toString(),indexValue,parameterMapping.getProperty());
                }
            }
        }

    }

    /**
     * 处理where表达式  左右类型
     * @param leftExpression
     * @param rightExpression
     */
    private void processWhereExpression(Expression leftExpression, Expression rightExpression) {
        if (leftExpression instanceof Column) {
            Column column = (Column) leftExpression;
            if (isOverwriteSourceByTarget() && isNeedProcessColumn(Lists.newArrayList(),column) ) {
                processWhereExpression(column);
                if (rightExpression instanceof JdbcParameter) {
                    JdbcParameter jdbcParameter = (JdbcParameter) rightExpression;
                    Integer index = jdbcParameter.getIndex();
                    ParameterMapping parameterMapping = orginalParameterMappings.get(index - 1);
                    String indexValue = baseSecurityHandler.index(getValueOfParameterMapping(parameterMapping));
                    replaceParams(UUID.randomUUID().toString(),indexValue,parameterMapping.getProperty());
                } else if (rightExpression instanceof StringValue) {
                    StringValue stringValue = (StringValue) rightExpression;
                    stringValue.setValue(baseSecurityHandler.index(stringValue));
                } else if(rightExpression instanceof InExpression){
                    processWhereExpression(rightExpression);
                } else if(rightExpression instanceof Function){
                    ExpressionList expressionList = ((Function)rightExpression).getParameters();
                    List<Expression> expressions = expressionList.getExpressions();
                    for (Expression expression : expressions) {
                        if(expression instanceof JdbcParameter){
                            JdbcParameter jdbcParameter = (JdbcParameter) expression;
                            Integer index = jdbcParameter.getIndex();
                            ParameterMapping parameterMapping = orginalParameterMappings.get(index - 1);
                            String indexValue = baseSecurityHandler.index(getValueOfParameterMapping(parameterMapping));
                            replaceParams(UUID.randomUUID().toString(),indexValue,parameterMapping.getProperty());
                        }
                    }
                }
            }

        }
        processWhereExpression(leftExpression);
        processWhereExpression(rightExpression);
    }

    /**
     * 是否是需要处理的字段
     * @param column
     * @return
     */
    protected boolean isNeedProcessColumn(List<Column> columns,Column column){
        if(!CollectionUtils.isEmpty(columns)){
            for (Column compareColumn : columns) {
                if(columnExtract(compareColumn.getColumnName()).equals(getTargetName(column))){
                    return false;
                }
                if(columnExtract(compareColumn.getColumnName()).equals(getIndexName(column))){
                    return false;
                }
            }
        }
        //^/w+$
        if(sourceTargetMap.keySet().contains(columnExtract(column.getColumnName()))){
            return true;
        }
        return false;
    }

    /**
     * 字段提取
     * @param columnName
     * @return
     */
    protected String columnExtract(String columnName){
        if(columnName.charAt(0) == CommonConstance.MYSQL_CHAR){
           return  columnName.substring(1,columnName.length()-1);
        }
        return columnName;

    }

    /**
     * 添加参数
     * @param property
     * @param value
     */
    protected void addParams(String property,Object value) {
        boundSql.getParameterMappings().add(new ParameterMapping.Builder(mappedStatement.getConfiguration(), property, String.class).build());
        boundSql.setAdditionalParameter(property,value);
    }

    /**
     * 替换参数
     * @param property 新的属性
     * @param value 新的值
     * @param oldOroperty 老的属性
     */
    protected void replaceParams(String property,Object value,String oldOroperty) {
        if(value!=null){
            int indexOfProperty = -1;
            for (int i = 0; i < boundSql.getParameterMappings().size(); i++) {
                ParameterMapping parameterMapping = boundSql.getParameterMappings().get(i);
                if(!parameterMapping.getProperty().equals(oldOroperty)){
                    continue;
                }
                indexOfProperty = i;
                break;
            }
            if(indexOfProperty != -1){
                boundSql.getParameterMappings().set(indexOfProperty,new ParameterMapping.Builder(mappedStatement.getConfiguration(), property, String.class).build());
            }
            boundSql.setAdditionalParameter(property,value);
        }
    }

    /**
     * 添加参数
     * @param property 属性
     * @param value 值
     * @param afterProperty 在该属性后添加
     */
    protected void addParams(String property,Object value,String afterProperty,Integer addIndex) {
        int indexOfProperty = -1;
        for (int i = 0; i < boundSql.getParameterMappings().size(); i++) {
            ParameterMapping parameterMapping = boundSql.getParameterMappings().get(i);
            if(!parameterMapping.getProperty().equals(afterProperty)){
                continue;
            }
            indexOfProperty = i;
            break;
        }
        if(indexOfProperty != -1){
            boundSql.getParameterMappings().add(indexOfProperty + addIndex + 1,
                    new ParameterMapping.Builder(mappedStatement.getConfiguration(), property, String.class).build());
        }
        boundSql.setAdditionalParameter(property,value);

    }

    /**
     *
     * @author dtssv
     * @date 2020/12/2 18:35
     * @return void
     **/
    protected void removeParams(String  property){
        int indexOfProperty = -1;
        for (int i = 0; i < boundSql.getParameterMappings().size(); i++) {
            ParameterMapping parameterMapping = boundSql.getParameterMappings().get(i);
            if(!parameterMapping.getProperty().equals(property)){
                continue;
            }
            indexOfProperty = i;
            break;
        }
        if(indexOfProperty != -1){
            boundSql.getParameterMappings().remove(indexOfProperty);
        }
    }
    /**
     * 通过序号获取ParameterMapping
     * @param index
     * @return
     */
    protected ParameterMapping getParameterMappingFromBoundSql(int index){
        if(orginalParameterMappings == null){
            return null;
        }
        if(index < 0 || index>=orginalParameterMappings.size()){
            return null;
        }
        return orginalParameterMappings.get(index);
    }

    /**
     * 获取parameterMapping对应的值
     * @param parameterMapping
     * @return
     */
    protected Object getValueOfParameterMapping(ParameterMapping parameterMapping){
        if (parameterMapping.getMode() != ParameterMode.OUT) {
            Object value;
            String propertyName = parameterMapping.getProperty();
            if (boundSql.hasAdditionalParameter(propertyName)) { // issue #448 ask first for additional params
                value = boundSql.getAdditionalParameter(propertyName);
            } else if (parameterObject == null) {
                value = null;
            } else if (configuration.getTypeHandlerRegistry().hasTypeHandler(parameterObject.getClass())) {
                value = parameterObject;
            } else {
                MetaObject metaObject = configuration.newMetaObject(parameterObject);
                value = metaObject.getValue(propertyName);
            }
            return value;
        }
        return  null;
    }

    /**
     * 修改parameterMapping对应的值
     * @param parameterMapping
     * @param value
     */
    protected void setValueOfParameterMapping(ParameterMapping parameterMapping, Object value){
        if (parameterMapping.getMode() != ParameterMode.OUT) {
            String propertyName = parameterMapping.getProperty();
            if (boundSql.hasAdditionalParameter(propertyName)) { // issue #448 ask first for additional params
                boundSql.setAdditionalParameter(propertyName,value);
            } else if (parameterObject == null) {

            } else if (configuration.getTypeHandlerRegistry().hasTypeHandler(parameterObject.getClass())) {

            } else {
                MetaObject metaObject = configuration.newMetaObject(parameterObject);
                 metaObject.setValue(propertyName,value);
            }
        }
    }

    /**
     * 处理字段
     * @author dtssv
     * @date 2020/12/7 14:45
     * @param columns
     * @return List<Map.Entry<Column,Integer>>
     **/
    protected List<Map.Entry<Column,Integer>> processColumn(List<Column> columns){
        List<Map.Entry<Column,Integer>> columnIndices = Lists.newArrayList();
        List<Map.Entry<Column,Integer>> processColumn = Lists.newArrayList();
        List<Integer> processColumnIndex = Lists.newArrayList();
        Integer count = 0;
        for (int i = 0; i < columns.size(); i++) {
            Column column = columns.get(i);
            if(isNeedProcessColumn(columns,column)){
                processColumn.add( new AbstractMap.SimpleEntry<>(column,i));
                Column targetColumn = new Column(getTargetName(column));
                Column indexColumn = new Column(getIndexName(column));
                targetColumn.setTable(column.getTable());
                indexColumn.setTable(column.getTable());
                int targetIndex = i +1+ columnIndices.size();
                int indexIndex = targetIndex+1;
                processColumnIndex.add(targetIndex - 1 - count);
                count++;
                Map.Entry entry = new AbstractMap.SimpleEntry<Object,Integer>(targetColumn,targetIndex);
                columnIndices.add(entry);
                entry = new AbstractMap.SimpleEntry<Object,Integer>(indexColumn,indexIndex);
                columnIndices.add(entry);
            }
        }
        for (Map.Entry<Column, Integer> column : columnIndices) {
            columns.add(column.getValue(), column.getKey());
        }
        if(!this.isWriteSource() && !CollectionUtils.isEmpty(processColumnIndex)){
            for (int columnIndex : processColumnIndex) {
                columns.remove(columnIndex);
            }
        }
        return processColumn;
    }
    /**
     * 处理表达式
     * @author dtssv
     * @date 2020/12/7 14:50
     * @param expressions
     * @return void
     **/
    protected void processExpression(List<Expression> expressions,List<Map.Entry<Column,Integer>> columnIndices){
        List<Map.Entry<Expression,Integer>> expressionIndices = Lists.newArrayList();
        List<Integer> processColumnIndex = Lists.newArrayList();
        List<String> processColumnProperty = Lists.newArrayList();
        for (Map.Entry<Column, Integer> column : columnIndices) {
            processColumnIndex.add(column.getValue());
            Expression expression = expressions.get(column.getValue());
            if (expression instanceof JdbcParameter) {
                JdbcParameter jdbcParameter = (JdbcParameter) expression;
                jdbcParameter.getIndex();
            }
            String property = null;
            Expression targetExpression =  null;
            Expression indexExpression =  null;
            if (expression instanceof JdbcParameter) {
                // 创建新的表达式部分
                targetExpression = new JdbcParameter();
                indexExpression = new JdbcParameter();
                ParameterMapping parameterMapping = getParameterMappingFromBoundSql(((JdbcParameter) expression).getIndex() - 1);
                property = parameterMapping.getProperty();
                processColumnProperty.add(property);
                // 增加参数映射 及 参数值
                Object valueOfColumn = getValueOfParameterMapping(parameterMapping);
                String encryptValue = baseSecurityHandler.encrypt(valueOfColumn);
                String indexValue = baseSecurityHandler.index(valueOfColumn);
                // 增加加密列参数
                addParams(UUID.randomUUID().toString(), encryptValue, property,0);
                // 增加索引列参数
                addParams(UUID.randomUUID().toString(), indexValue, property,1);
            } else if (expression instanceof StringValue) {
                String sourceValue = ((StringValue) expression).getValue();
                String targetValue = baseSecurityHandler.encrypt(sourceValue);
                String indexValue = baseSecurityHandler.index(sourceValue);
                // 创建新的表达式部分
                targetExpression = new StringValue(targetValue);
                indexExpression = new StringValue(indexValue);
            }
            Map.Entry entry1 = new AbstractMap.SimpleEntry<>(targetExpression,column.getValue() + 1 + expressionIndices.size());
            expressionIndices.add(entry1);
            entry1 = new AbstractMap.SimpleEntry<>(indexExpression,column.getValue() + 1 + expressionIndices.size());
            expressionIndices.add(entry1);
        }
        for (Map.Entry<Expression, Integer> expression : expressionIndices) {
            expressions.add(expression.getValue(),expression.getKey());
        }
        if(!this.isWriteSource() && !CollectionUtils.isEmpty(processColumnIndex)){
            for (int i = 0; i < processColumnIndex.size(); i++) {
                int columnIndex = processColumnIndex.get(i);
                expressions.remove(columnIndex + i * 2 - i);
            }
//            for (int columnIndex : processColumnIndex) {
//                expressions.remove(columnIndex );
//            }
            if(!CollectionUtils.isEmpty(processColumnProperty)){
                for (String property : processColumnProperty) {
                    removeParams(property);
                }
            }
        }
    }

    /**
     * 
     */
    protected String getTargetName(Column column){
        String columnName = columnExtract(column.getColumnName());
        SecurityColumnConfig target = sourceTargetMap.get(columnName);
        return Objects.nonNull(target) ? target.getTargetColumn() : null;
    }
    /**
     *
     */
    protected String getIndexName(Column column){
        String columnName = columnExtract(column.getColumnName());
        SecurityColumnConfig target = sourceTargetMap.get(columnName);
        return Objects.nonNull(target) ? target.getIndexColumn() : null;
    }
}