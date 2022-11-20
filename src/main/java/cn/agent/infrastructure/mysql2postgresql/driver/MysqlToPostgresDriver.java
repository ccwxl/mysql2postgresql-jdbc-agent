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

    private static final String MYSQL = "mysql";

    private static final String PG = "postgresql";

    private static final String DEF_MYSQL_PORT = "3306";

    private static final String DEF_PG_PORT = "5432";

    static {
        try {
            DriverManager.registerDriver(MysqlToPostgresDriver.INSTANCE);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not register MysqlToOracleDriver with DriverManager.", e);
        }
    }

    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        String replaceURL = replaceURL(url);
        return ConnectionWrapper.wrap(super.connect(replaceURL, info));
    }

    @Override
    public boolean acceptsURL(String url) {
        String replaceURL = replaceURL(url);
        return super.acceptsURL(replaceURL);
    }

    public String replaceURL(String url) {
        if (url.contains(MYSQL)) {
            url = url.replaceAll(MYSQL, PG);
        }
        if (url.contains(DEF_MYSQL_PORT)) {
            url = url.replaceAll(DEF_MYSQL_PORT, DEF_PG_PORT);
        }
        return url;
    }
}
