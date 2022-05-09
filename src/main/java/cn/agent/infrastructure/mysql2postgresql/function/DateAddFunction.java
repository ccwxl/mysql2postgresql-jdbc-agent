package cn.agent.infrastructure.mysql2postgresql.function;

import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.arithmetic.Concat;
import net.sf.jsqlparser.expression.operators.arithmetic.Subtraction;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.statement.create.table.ColDataType;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SubSelect;

import java.util.Collections;
import java.util.List;

/**
 * mysql: DATE_ADD(?, INTERVAL -? SECOND)
 * postgresql: (SELECT ?::timestamp - (? || ' SECOND')::INTERVAL)
 *
 * @author wxl
 */
public class DateAddFunction extends BaseFunction {

    private DateAddFunction() {
        super("date_add");
    }

    public static void init() {
        new DateAddFunction();
    }

    @Override
    public Boolean apply(Function function) {
        ExpressionList parameters = function.getParameters();
        List<Expression> expressions = parameters.getExpressions();
        Expression expression = expressions.get(0);
        if (expression instanceof JdbcParameter) {
            JdbcParameter jp = (JdbcParameter) expression;
            function.setName("");
            expressions.clear();

            //直接构造select
            PlainSelect plainSelect = new PlainSelect();
            SelectExpressionItem selectExpressionItem = new SelectExpressionItem();
            Subtraction subtraction = new Subtraction();

            //left: ?::timestamp
            CastExpression leftExpression = new CastExpression();
            leftExpression.setUseCastKeyword(false);
            leftExpression.setLeftExpression(new JdbcParameter(jp.getIndex(), false));
            leftExpression.setType(new ColDataType("timestamp"));
            subtraction.setLeftExpression(leftExpression);

            //right: (? || ' SECOND')::INTERVAL
            CastExpression rightExpression = new CastExpression();
            rightExpression.setUseCastKeyword(false);
            Parenthesis parenthesis = new Parenthesis();
            parenthesis.setExpression(new Concat().withLeftExpression(new JdbcParameter(jp.getIndex() + 1, false))
                    .withRightExpression(new StringValue("' SECOND'")));
            rightExpression.setLeftExpression(parenthesis);
            rightExpression.setType(new ColDataType("INTERVAL"));
            subtraction.setRightExpression(rightExpression);

            //放入表达式中
            selectExpressionItem.setExpression(subtraction);
            plainSelect.setSelectItems(Collections.singletonList(selectExpressionItem));

            //构造子查询
            SubSelect subSelect = new SubSelect();
            subSelect.setSelectBody(plainSelect);

            //设置子查询
            expressions.add(subSelect);
            return true;
        } else {
            System.err.println("暂不支持的函数转换. [" + function + "]");
            return false;
        }
    }
}
