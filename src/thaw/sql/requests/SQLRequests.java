package thaw.sql.requests;

import thaw.utils.Utils;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.Instant;

import static java.util.Objects.requireNonNull;

/**
 * Created by nakaze on 12/12/16.
 */
public interface SQLRequests {
    /**
     * Returns an Insert type request string.
     *
     * @param arguments : the parameters of the request (table name, values, ...)
     * @return the request string
     */
    String add(String[] arguments);

    /**
     * Returns a Select * type request string.
     *
     * @param arguments : the list of arguments (table name, values, ...), but there may be no need to have an argument,
     *                  when the table name is fixed for example.
     * @return the request string
     */
    String getAll(String[] arguments);

    /**
     * Returns a Delete type request string.
     *
     * @param key : The condition verified by the element to be deleted.
     * @return the request string
     */
    String delete(String key);

    /**
     * Returns a Update type request string.
     *
     * @param object   : The condition verified by the element to be modified.
     * @param newValue : The new value of that element
     * @return the request string
     */
    String modify(String object, String newValue);

    default String encodeSQL(String s) {
        StringBuilder tmp = new StringBuilder();

        for (int i = 0; i < s.length(); i++) {
            switch (s.charAt(i)) {
                case '\'':
                    tmp.append('\'');
            }
            tmp.append(s.charAt(i));
        }
        return tmp.toString();
    }

    static SQLRequests createSQLChannelRequest() {
        return new SQLRequests() {
            @Override
            public String add(String[] arguments) {
                // INSERT INTO tablename VALUES (id, username, content, date)
                return "INSERT INTO " + encodeSQL(arguments[0]) + " (username, content, date) VALUES ('" + encodeSQL(arguments[1]) + "', '" + encodeSQL(arguments[2]) + "', '" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(java.util.Date.from(Instant.now())) + "' )";
            }

            @Override
            public String getAll(String[] arguments) {
                // SELECT * FROM tablename
                return "SELECT * FROM " + encodeSQL(arguments[0]);
            }

            @Override
            public String delete(String key) {
                // TODO IF NEEDED
                throw new UnsupportedOperationException();
            }

            // TODO
            @Override
            public String modify(String object, String newValue) {
                // TODO IF NEEDED
                throw new UnsupportedOperationException();
            }
        };
    }

    static SQLRequests createSQLChannelsRequest() {
        return new SQLRequests() {
            @Override
            public String add(String[] arguments) {
                // TODO WHEN TABLE IS CREATED
                return "INSERT INTO channels VALUES ('" + encodeSQL(arguments[0]) + "');";
//                throw new UnsupportedOperationException(Arrays.stream(objects).map(Object::toString).collect(Collectors.joining(" ")));
            }

            @Override
            public String getAll(String[] arguments) {
                return "SELECT * FROM channels";
            }

            @Override
            public String delete(String key) {
                // TODO WHEN TABLE IS CREATED
                return "DELETE FROM channels WHERE cname == " + encodeSQL(key);
            }

            @Override
            public String modify(String object, String newValue) {
                throw new UnsupportedOperationException();
            }
        };
    }

    static SQLRequests createSQLUserRequest() {
        return new SQLRequests() {
            @Override
            public String add(String[] arguments) {
                if (arguments.length != 3)
                    throw new IllegalArgumentException("Add user doc: need 3 arguments (" + arguments.length + " given).");
                return "INSERT INTO users VALUES ('" + encodeSQL(requireNonNull(arguments[0])) + "', '" + Utils.toSHA256(encodeSQL(requireNonNull(arguments[1]))) + "', " + encodeSQL(requireNonNull(arguments[2])) + ")";
            }

            @Override
            public String getAll(String[] arguments) {
                return "SELECT * FROM users";
            }

            @Override
            public String delete(String username) {
                return "DELETE FROM users WHERE username = '" + encodeSQL(requireNonNull(username)) + "'";
            }

            @Override
            public String modify(String key, String newValue) {
                return "UPDATE users SET avatar = " + encodeSQL(newValue) + "' WHERE username = '" + encodeSQL(key) + "'";
            }
        };
    }
}
