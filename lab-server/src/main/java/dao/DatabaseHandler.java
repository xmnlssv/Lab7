package dao;

import java.util.Objects;

/**
 * The class is an implementation of the singleton programming pattern, and is needed to address the
 * to a single database manager
 */
public class DatabaseHandler {
    private static DatabaseManager databaseManager;
    static {
        databaseManager = new DatabaseManager();
    }
    public static DatabaseManager getDatabaseManager(){
        if (Objects.isNull(databaseManager)) databaseManager = new DatabaseManager();
        return databaseManager;
    }
}
