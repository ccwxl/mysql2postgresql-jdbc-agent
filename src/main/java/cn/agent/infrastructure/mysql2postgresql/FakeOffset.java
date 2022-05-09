package cn.agent.infrastructure.mysql2postgresql;

import net.sf.jsqlparser.statement.select.Offset;

/**
 * @author wxl
 */
public class FakeOffset extends Offset {

    @Override
    public String toString() {

        return " LIMIT " + getOffset() + (getOffsetParam() != null ? " " + getOffsetParam() : "");
    }
}
