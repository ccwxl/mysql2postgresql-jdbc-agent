package cn.agent.infrastructure.mysql2postgresql.driver;

import cn.agent.infrastructure.mysql2postgresql.wrapper.ConnectionWrapper;
import org.postgresql.Driver;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * @author wxl
 */
public class MysqlToPostgresDriver extends Driver {
    private static final java.sql.Driver INSTANCE = new MysqlToPostgresDriver();

    static {
        try {
            DriverManager.registerDriver(MysqlToPostgresDriver.INSTANCE);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not register MysqlToOracleDriver with DriverManager.", e);
        }
    }

    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        return ConnectionWrapper.wrap(super.connect(url, info));
    }
}
