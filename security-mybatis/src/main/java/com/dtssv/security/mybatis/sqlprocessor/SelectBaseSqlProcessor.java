package com.dtssv.security.mybatis.sqlprocessor;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.Data;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.*;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

/**
 * selectSQL处理器
 * @date
 * @author dtssv
 */
@Data
public class SelectBaseSqlProcessor extends BaseSqlProcessor<Select> {
    /**
     * columnPropertyMap
     */
    protected Map<String, String> columnPropertyMap = Maps.newHashMap();
    /**
     *
     */
    public SelectBaseSqlProcessor(String sql, MappedStatement mappedStatement) {
        super(sql, mappedStatement);
    }
    /**
     *
     */
    public SelectBaseSqlProcessor(){
        
    }
    /**
     * 处理selectBody
     *
     * @param selectBody
     */
    public void processSelectBody(SelectBody selectBody) {
        if (selectBody instanceof PlainSelect) { // 如果是简单的select
            processPlainSelect((PlainSelect) selectBody);
        } else if (selectBody instanceof WithItem) { // 如果是带有子查询的sql
            WithItem withItem = (WithItem) selectBody;
            if (withItem.getSelectBody() != null) {
                processSelectBody(withItem.getSelectBody());
            }
        } else {
            if(selectBody != null && selectBody instanceof SetOperationList) {
                SetOperationList operationList = (SetOperationList) selectBody;
                if (operationList.getSelects() != null && operationList.getSelects().size() > 0) {
                    List<SelectBody> plainSelects = operationList.getSelects();
                    for (SelectBody plainSelect : plainSelects) {
                        processSelectBody(plainSelect);
                    }
                }
            }
        }
    }

    /**
     * 处理PlainSelect类型的selectBody
     *
     * @param plainSelect
     */
    public void processPlainSelect(PlainSelect plainSelect) {
        if(plainSelect.getSelectItems()!=null){
            processSelectItems(plainSelect.getSelectItems());
        }
        if (plainSelect.getFromItem() != null) {
            processFromItem(plainSelect.getFromItem());
        }
        if (plainSelect.getJoins() != null && plainSelect.getJoins().size() > 0) {
            List<Join> joins = plainSelect.getJoins();
            for (Join join : joins) {
                if (join.getRightItem() != null) {
                    processFromItem(join.getRightItem());
                }
            }
        }
        // 如果where条件存在 且需要使用target覆盖source 那么处理where语句
        if(plainSelect.getWhere() != null){
            processWhereExpression(plainSelect.getWhere());
        }
    }

    /**
     * 处理查询的字段
     * @param selectItems
     */
    private void processSelectItems(List<SelectItem> selectItems){
        // 如果不是读取target的字段覆盖source 那么直接读取source
        if(!isOverwriteSourceByTarget()){
            return ;
        }
        List<SelectItem>  newSelectItems = Lists.newArrayList();
        for (SelectItem selectItem : selectItems) {
            if(selectItem instanceof AllColumns){

            }else if(selectItem instanceof SelectExpressionItem){
                SelectExpressionItem selectExpressionItem = (SelectExpressionItem) selectItem;
                Expression expression = selectExpressionItem.getExpression();
                if(expression instanceof Column){
                    Column column = (Column) expression;
                    // 如果是需要处理的列
                    String columnName = column.getColumnName();
                    if(isNeedProcessColumn(Lists.newArrayList(),column)){
                        // 如果别名不存在 那么列名就是别名
                        if(selectExpressionItem.getAlias() == null){
                            Alias alias = new Alias(columnName);
                            selectExpressionItem.setAlias(alias);
                        }
                        // 如果使用target覆盖source 那么不再读取source 转而读取target

                        List<ResultMap> resultMaps = mappedStatement.getResultMaps();
                        String aliasName = columnExtract(selectExpressionItem.getAlias().getName());
                        // 如果有映射关系 那么取映射关系的property和column对应
                        for (ResultMap resultMap : resultMaps) {
                            List<ResultMapping> resultMappings = resultMap.getResultMappings();
                            for (ResultMapping resultMapping : resultMappings) {
                                // 如果对应的mapping不是本列的 跳过
                                if(!resultMapping.getColumn().equals(aliasName)){
                                    continue;
                                }
                                columnPropertyMap.put(columnName,resultMapping.getProperty());
                                break;
                            }
                        }
                        // 如果映射关系找不到 那么应该就是直接用别名对应
                        if(columnPropertyMap.get(columnName) == null){
                            columnPropertyMap.put(columnName,aliasName);
                        }
                        // 修改查询列为target列
                        column.setColumnName(getTargetName(column));
                    }

                }
            }else if(selectItem instanceof AllTableColumns){

            }
        }
        selectItems.addAll(newSelectItems);
    }


    @Override
    protected void doProcess(Select select) {
        SelectBody selectBody = select.getSelectBody();
        processSelectBody(selectBody);

    }

    /**
     * 处理子查询
     *
     * @param fromItem
     */
    public void processFromItem(FromItem fromItem) {
        if (fromItem instanceof SubJoin) {
            SubJoin subJoin = (SubJoin) fromItem;
            List<Join> joinList = subJoin.getJoinList();
            if(!CollectionUtils.isEmpty(joinList)){
                for (Join join : joinList) {
                    if (join.getRightItem() != null) {
                        processFromItem(join.getRightItem());
                    }
                }
            }
            if (subJoin.getLeft() != null) {
                processFromItem(subJoin.getLeft());
            }
        } else if (fromItem instanceof SubSelect) {
            SubSelect subSelect = (SubSelect) fromItem;
            if (subSelect.getSelectBody() != null) {
                processSelectBody(subSelect.getSelectBody());
            }
        } else if (fromItem instanceof ValuesList) {

        } else if (fromItem instanceof LateralSubSelect) {
            LateralSubSelect lateralSubSelect = (LateralSubSelect) fromItem;
            if (lateralSubSelect.getSubSelect() != null) {
                SubSelect subSelect = lateralSubSelect.getSubSelect();
                if (subSelect.getSelectBody() != null) {
                    processSelectBody(subSelect.getSelectBody());
                }
            }
        }
    }
}
