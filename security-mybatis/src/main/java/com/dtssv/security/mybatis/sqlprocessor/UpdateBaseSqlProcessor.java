package com.dtssv.security.mybatis.sqlprocessor;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.update.Update;
import org.apache.ibatis.mapping.MappedStatement;

import java.util.List;
import java.util.Map;

/**
 * 更新sql处理器
 * @date
 * @author dtssv
 */
public class UpdateBaseSqlProcessor extends BaseSqlProcessor<Update> {
    /**
     *
     */
    public UpdateBaseSqlProcessor(String sql, MappedStatement mappedStatement) {
        super(sql, mappedStatement);
    }

    @Override
    protected void doProcess(Update update) {
        List<Column> columns = update.getColumns();
        List<Expression> expressions = update.getExpressions();
        // 处理update表达式
        List<Map.Entry<Column,Integer>> processColumns = processColumn(columns);
        processExpression(expressions,processColumns);
        Expression where = update.getWhere();
        // 处理where部分
        processWhereExpression(where);
    }

}
