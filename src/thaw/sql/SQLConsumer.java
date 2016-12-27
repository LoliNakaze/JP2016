package thaw.sql;

import java.sql.SQLException;

/**
 * Created by nakaze on 16/11/16.
 */
@FunctionalInterface
public interface SQLConsumer<T> {
    void accept (T t) throws SQLException;
}
