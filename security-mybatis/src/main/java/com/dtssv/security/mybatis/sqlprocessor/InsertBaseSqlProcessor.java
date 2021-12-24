package com.dtssv.security.mybatis.sqlprocessor;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.ItemsList;
import net.sf.jsqlparser.expression.operators.relational.MultiExpressionList;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.insert.Insert;
import org.apache.ibatis.mapping.MappedStatement;

import java.util.List;
import java.util.Map;

/**
 * insert语句处理器
 * @date
 * @author dtssv
 */
public class InsertBaseSqlProcessor extends BaseSqlProcessor<Insert> {
    /**
     *
     */
    public InsertBaseSqlProcessor(String sql, MappedStatement mappedStatement) {
        super(sql, mappedStatement);
    }

    @Override
    protected void doProcess(Insert insert) {
        // 获取insert部分的 字段信息
        List<Column> columns = insert.getColumns();
        if(columns ==null || columns.size() <= 0){
            return ;
        }
        List<Map.Entry<Column,Integer>> processColumns = processColumn(columns);
        // 获取insert部分的 值列表
        ItemsList itemsList = insert.getItemsList();
        if(itemsList instanceof ExpressionList){// 如果是单值列表
            ExpressionList expressionList = (ExpressionList) itemsList;
            List<Expression> expressions = expressionList.getExpressions();
            processExpression(expressions,processColumns);
        }else if(itemsList instanceof MultiExpressionList){ // 如果是多值列表
            MultiExpressionList multiExpressionList = (MultiExpressionList) itemsList;
            List<ExpressionList> exprList = multiExpressionList.getExprList();
            for (ExpressionList expressionList : exprList) {
                List<Expression> expressions = expressionList.getExpressions();
                processExpression(expressions,processColumns);
            }
        }

    }

}
