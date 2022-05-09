package cn.agent.infrastructure.mysql2postgresql;

import net.sf.jsqlparser.expression.AllValue;
import net.sf.jsqlparser.expression.NullValue;
import net.sf.jsqlparser.statement.select.Limit;

/**
 * @author wxl
 */
public class FakeLimit extends Limit {

    @Override
    public String toString() {
        String retVal = " OFFSET ";
        if (getRowCount() instanceof AllValue || getRowCount() instanceof NullValue) {
            // no offset allowed
            retVal += getRowCount();
        } else {
            if (null != getOffset()) {
                retVal += getOffset() + ", ";
            }
            if (null != getRowCount()) {
                retVal += getRowCount();
            }
        }

        return retVal;
    }
}
