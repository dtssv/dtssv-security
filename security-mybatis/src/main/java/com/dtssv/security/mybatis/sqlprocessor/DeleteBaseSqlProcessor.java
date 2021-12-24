package com.dtssv.security.mybatis.sqlprocessor;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.statement.delete.Delete;
import org.apache.ibatis.mapping.MappedStatement;

/**
 * delete语句处理器
 * @date
 * @author dtssv
 */
public class DeleteBaseSqlProcessor extends BaseSqlProcessor<Delete> {
    /**
     *
     */
    public DeleteBaseSqlProcessor(String sql, MappedStatement mappedStatement) {
        super(sql, mappedStatement);
    }

    @Override
    protected void doProcess(Delete delete) {
        Expression where = delete.getWhere();
        // 处理where部分
        processWhereExpression(where);
    }
}
