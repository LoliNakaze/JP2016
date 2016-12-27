package thaw.sql;

import thaw.sql.requests.SQLRequests;

import java.sql.*;

/**
 * Created by nakaze on 25/11/16.
 */
public class SQLHandler {
    private final String database;

    private SQLHandler(String database) {
        this.database = "jdbc:sqlite:" + database + ".db";
    }

    public static SQLHandler createSQLHandler(String database) throws SQLException {
        SQLHandler tmp = new SQLHandler(database);
        tmp.createAllTables();
        return tmp;
    }

    private static Statement connect(String databasePath, int time) throws SQLException {
        Statement statement = DriverManager.getConnection(databasePath).createStatement();
        statement.setQueryTimeout(time);
        return statement;
    }

    public void addChannel(String[] arguments) throws SQLException {
        Statement statement = connect(database, 1);

        if (1 != statement.executeUpdate(SQLRequests.createSQLChannelsRequest().add(arguments)))
            throw new IllegalStateException("There is already a channel with that name.");
        String channelForm = "(id INTEGER PRIMARY KEY AUTOINCREMENT, username VARCHAR2(20),content VARCHAR2(500), date date)";
        statement.execute("CREATE TABLE " + arguments[0] + " " + channelForm);
        statement.executeUpdate("INSERT INTO " + arguments[0] + " VALUES (0, '__System', 'This is the first message of the channel.', '" + new Date(System.currentTimeMillis()) + "')");
    }

    public ResultSet getAllChannels() throws SQLException {
        Statement statement = connect(database, 1);

        return statement.executeQuery(SQLRequests.createSQLChannelsRequest().getAll(null));
    }

    public ResultSet getAllInChannel(String[] arguments) throws SQLException {
        Statement statement = connect(database, 1);

        return statement.executeQuery(SQLRequests.createSQLChannelRequest().getAll(arguments));
    }

    public int postInChannel(String[] arguments) throws SQLException {
        Statement statement = connect(database, 1);

        String s = SQLRequests.createSQLChannelRequest().add(arguments);
        System.out.println(s);

        return statement.executeUpdate(s);
    }

    public int addUser(String[] arguments) throws SQLException {
        Statement statement = connect(database, 1);

        return statement.executeUpdate(SQLRequests.createSQLUserRequest().add(arguments));
    }

    public ResultSet getAllUsers() throws SQLException {
        Statement statement = connect(database, 1);

        return statement.executeQuery(SQLRequests.createSQLUserRequest().getAll(null));
    }

    private void createAllTables() throws SQLException {
        Statement statement = connect(database, 10);

        statement.execute("CREATE TABLE IF NOT EXISTS users (username varchar2(15) NOT NULL PRIMARY KEY," +
                "password varchar2(30) NOT NULL," +
                "avatar INT NOT NULL DEFAULT 1)");

//        statement.execute("DROP TABLE channels");
//        statement.execute("DROP TABLE general");
        statement.execute("CREATE TABLE IF NOT EXISTS channels (cname varchar2(20) NOT NULL PRIMARY KEY)");
        addChannel(new String[]{"general"});
    }
}