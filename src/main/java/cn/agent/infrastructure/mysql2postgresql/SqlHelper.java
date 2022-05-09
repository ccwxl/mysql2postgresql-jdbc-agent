package cn.agent.infrastructure.mysql2postgresql;

import cn.agent.infrastructure.mysql2postgresql.function.BaseFunction;
import cn.agent.infrastructure.mysql2postgresql.function.DateAddFunction;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.arithmetic.Concat;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.StatementVisitorAdapter;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.update.UpdateSet;
import net.sf.jsqlparser.util.TablesNamesFinder;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author wxl
 */
public class SqlHelper {

    private static final String SEMICOLON = ";";
    private static final Map<String, String> MAPPING = new HashMap<>();

    static {
        //初始化一些直接转换的SQL语句
        MAPPING.put("select username from users where username like '%' ? '%'", "SELECT username FROM users WHERE username ILIKE '%' ||?|| '%'");
        MAPPING.put("SELECT role FROM roles WHERE role LIKE '%' ? '%'", "SELECT role FROM roles WHERE role ILIKE '%' ||?|| '%'");

        //初始化函数处理
        DateAddFunction.init();
    }

    /**
     * @param sql 原mysql语法
     * @return postgresql 支持的语法
     */
    public static String mysql2postgresql(String sql) {
        AtomicBoolean needModify = new AtomicBoolean(false);
        String newSql = getNewSql(needModify, sql.trim());
        if (needModify.get()) {
            return newSql;
        }
        //System.out.printf(" 源sql:%s\n 转换后:%s%n \n", sql, newSql);
        return sql;
    }

    private static String getNewSql(AtomicBoolean needModify, String sqlTrim) {
        if (MAPPING.containsKey(sqlTrim)) {
            //固定的SQL写法
            needModify.set(true);
            return MAPPING.get(sqlTrim);
        }
        return parserAndConverter(needModify, sqlTrim);
    }

    private static String parserAndConverter(AtomicBoolean needModify, String sqlTrim) {
        try {
            Statement parse = CCJSqlParserUtil.parse(sqlTrim);
            if (sqlTrim.endsWith(SEMICOLON)) {
                needModify.set(true);
            }

            parse.accept(new TablesNamesFinder() {
                @Override
                public void visit(SelectExpressionItem item) {
                    super.visit(item);

                    if (item.getAlias() != null &&
                            (item.getExpression() instanceof StringValue
                                    || item.getExpression() instanceof Column
                                    || item.getExpression() instanceof Function)) {
                        if (!item.getAlias().getName().startsWith("\"")
                                && !item.getAlias().getName().endsWith("\"")) {
                            item.getAlias().setName("\"" + item.getAlias().getName() + "\"");
                            needModify.set(true);
                        }
                    }
                }

                @Override
                public void visit(Table tableName) {
                    if (tableName.getName() != null && tableName.getName().contains("`")) {
                        tableName.setName(tableName.getName().replaceAll("`", ""));
                        needModify.set(true);
                    }

                    if (tableName.getAlias() != null && tableName.getAlias().isUseAs()) {
                        tableName.getAlias().setUseAs(false);
                        needModify.set(true);
                    }
                }

                @Override
                public void visit(SubSelect subSelect) {
                    super.visit(subSelect);
                    if (subSelect.getAlias() != null && subSelect.getAlias().isUseAs()) {
                        subSelect.getAlias().setUseAs(false);
                        needModify.set(true);
                    }
                }

                @Override
                public void visit(Column tableColumn) {
                    if (tableColumn.getColumnName().contains("`")) {
                        tableColumn.setColumnName(tableColumn.getColumnName().replaceAll("`", ""));
                        needModify.set(true);
                    }
                }

                @Override
                public void visit(Insert insert) {
                    super.visit(insert);
                    columnsProcess(insert.getColumns());

                }

                @Override
                public void visit(Update update) {
                    super.visit(update);
                    ArrayList<UpdateSet> updateSets = update.getUpdateSets();
                    for (UpdateSet updateSet : updateSets) {
                        columnsProcess(updateSet.getColumns());
                    }
                }

                @Override
                public void visit(NotExpression notExpr) {
                    //非等条件,不使用感叹号.
                    if (notExpr.isExclamationMark()) {
                        notExpr.setExclamationMark(false);
                        needModify.set(true);
                    }
                }

                @Override
                public void visit(Function function) {
                    super.visit(function);
                    //对函数进行替换处理
                    if (BaseFunction.process(function)) {
                        needModify.set(true);
                    }
                }

                private void columnsProcess(List<Column> columns) {
                    columns.stream()
                            .filter(c -> c.getColumnName().contains("`"))
                            .peek(c -> needModify.set(true))
                            .forEach(c -> c.setColumnName(c.getColumnName().replaceAll("`", "")));
                }
            });

            parse.accept(new StatementVisitorAdapter() {
                @Override
                public void visit(Select select) {
                    SelectBody selectBody = select.getSelectBody();
                    selectBody.accept(new SelectVisitorAdapter() {
                        @Override
                        public void visit(PlainSelect plainSelect) {
                            //替换分页
                            if (replacePageSql(plainSelect)) {
                                needModify.set(true);
                            }
                        }
                    });
                }

                @Override
                public void visit(Delete delete) {
                    Expression where = delete.getWhere();
                    if (where != null) {
                        where.accept(new ExpressionVisitorAdapter() {
                            @Override
                            public void visit(SubSelect subSelect) {
                                SelectBody selectBody = subSelect.getSelectBody();
                                if (selectBody instanceof PlainSelect) {
                                    replacePageSql((PlainSelect) selectBody);
                                }
                                needModify.set(true);
                            }
                        });
                    }

                    if (delete.getLimit() != null) {
                        // 重写delete limit , 使用pg的 ctid 删除.
                        InExpression inExpression = new InExpression();
                        inExpression.setLeftExpression(new Column("ctid"));

                        PlainSelect selectBody = new PlainSelect();
                        selectBody.setSelectItems(Collections.singletonList(new SelectExpressionItem(new Column("ctid"))));
                        selectBody.setFromItem(delete.getTable());
                        selectBody.setWhere(delete.getWhere());
                        selectBody.setLimit(delete.getLimit());

                        SubSelect subSelect = new SubSelect();
                        subSelect.setSelectBody(selectBody);

                        inExpression.setRightExpression(subSelect);

                        //重新设置值
                        delete.setWhere(inExpression);
                        delete.setLimit(null);

                        needModify.set(true);
                    }
                }
            });

            //返回重新替换后的 SQL
            return parse.toString();
        } catch (Exception e) {
            needModify.set(false);
            System.err.println("SQL Conversion exception SQL:" + sqlTrim + " Ex: " + e);
        }
        return sqlTrim;
    }


    /**
     * 替换分页语句
     *
     * @param plainSelect select
     */
    private static boolean replacePageSql(PlainSelect plainSelect) {
        Limit limit = plainSelect.getLimit();
        if (limit != null) {
            // 替换分页
            plainSelect.setLimit(null);
            if (limit.getRowCount() instanceof LongValue && limit.getOffset() instanceof LongValue) {
                //非预编译limit,  limit 0, 10
                plainSelect.setLimit(new Limit().withRowCount(limit.getRowCount()));
                plainSelect.setOffset(new Offset().withOffset(limit.getOffset()));
            } else if (limit.getRowCount() instanceof JdbcParameter && limit.getOffset() instanceof JdbcParameter) {
                //翻转limit 与 offset
                plainSelect.setLimit(new FakeLimit().withRowCount(limit.getOffset()));
                plainSelect.setOffset(new FakeOffset().withOffset(limit.getRowCount()));
            } else if (limit.getRowCount() instanceof JdbcParameter && limit.getOffset() instanceof LongValue) {
                //limit 0 ?
                plainSelect.setLimit(new FakeLimit().withRowCount(new LongValue(0)));
                plainSelect.setOffset(new FakeOffset().withOffset(limit.getRowCount()));
            } else if (limit.getRowCount() instanceof JdbcParameter) {
                //limit ?
                plainSelect.setLimit(new Limit().withRowCount(limit.getRowCount()));
                plainSelect.setOffset(new Offset().withOffset(new LongValue(0)));
            } else if (limit.getRowCount() instanceof LongValue) {
                // limit 10
                plainSelect.setLimit(new Limit().withRowCount(limit.getRowCount()));
                plainSelect.setOffset(new Offset().withOffset(new LongValue(0)));
            } else {
                plainSelect.setLimit(limit);
                System.err.println("暂不支持的分页语法. [" + plainSelect + "]");
                return false;
            }
        }

        //子查询
        FromItem fromItem = plainSelect.getFromItem();
        if (fromItem instanceof SubSelect) {
            SubSelect subSelect = (SubSelect) fromItem;
            SelectBody selectBody = subSelect.getSelectBody();
            if (selectBody instanceof PlainSelect) {
                return replacePageSql((PlainSelect) selectBody);
            }
        }

        // where 是不是子查询
        Expression where = plainSelect.getWhere();
        if (where instanceof InExpression) {
            InExpression inExpression = (InExpression) where;
            Expression rightExpression = inExpression.getRightExpression();
            if (rightExpression instanceof SubSelect) {
                SubSelect subSelect = (SubSelect) rightExpression;
                SelectBody selectBody = subSelect.getSelectBody();
                if (selectBody instanceof PlainSelect) {
                    return replacePageSql((PlainSelect) selectBody);
                }
            }
        }
        return true;
    }
}
