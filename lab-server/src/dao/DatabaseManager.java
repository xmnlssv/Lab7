package dao;

import authentication.UserCredentials;
import model.*;
import org.postgresql.util.PSQLException;
import utils.PropertiesUtil;
import utils.ZonedDateTimeAdapter;
import worker.PasswordHashed;
import worker.Worker;

import java.sql.*;

public class DatabaseManager {
    public static final String URL_KEY = "db.url";
    public static final String USERNAME_KEY = "db.username";
    public static final String PASSWORD_KEY = "db.password";
    public static Worker worker = Worker.getInstance();
    private Connection connection; // abstract class for connection
    private String salt = PasswordHashed.generateSalt();
    private ZonedDateTimeAdapter zonedDateTimeAdapter = new ZonedDateTimeAdapter();
    private static UserCredentials userCredentials;

    public DatabaseManager() {
        while (true) {
            try {
                this.connection = DriverManager.getConnection("***********",
                        "s******", "***"
//                        PropertiesUtil.get(URL_KEY),
//                        PropertiesUtil.get(USERNAME_KEY),
//                        PropertiesUtil.get(PASSWORD_KEY)
                );
                System.out.println("Connection was established  successfully.");
                break;
            } catch (PSQLException psqlException) {
                System.out.println("Database is not available at the moment therefore SSH tunnel inactive." +
                        " Reconnecting...");
                //psqlException.printStackTrace();
            } catch (SQLException sqlException) {
                System.out.println("Database is not available at the moment. Reconnecting...");
            }
        }
    }

    /**
     * Loads collection from database
     *
     * @return result of loading
     */
    public String load() {
        try (ResultSet resultSet = connection.createStatement().executeQuery(DatabaseCommands.getAllObjects)) {
            worker.getCollection().clear();
            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String name = resultSet.getString("name");
                Coordinates coordinates = new Coordinates(resultSet.getDouble("x"),
                        resultSet.getLong("y"));
                String creationDate = resultSet.getString("creation_date");
                long minimalPoint = resultSet.getInt("minimal_point");
                Difficulty difficulty = Difficulty.valueOf(resultSet.getString("difficulty"));
                Person author = new Person(resultSet.getString("name"), resultSet.
                        getString("passport_id"), EyeColor.valueOf(resultSet.getString("eye_color")),
                        HairColor.valueOf(resultSet.getString("hair_color")), Country.valueOf(resultSet.
                                getString("nationality")), new Location(resultSet.
                                getFloat("x_location"),
                                resultSet.getLong("y_location"), resultSet.getInt("z_location")));
                EyeColor eyeColor = EyeColor.valueOf(resultSet.getString("eye_color"));
                HairColor hairColor = HairColor.valueOf(resultSet.getString("hair_color"));
                Country nationality = Country.valueOf(resultSet.getString("nationality"));
                Location location = new Location(resultSet.getFloat("x_location"),
                        resultSet.getLong("y_location"), resultSet.getInt("z_location"));
                String login = resultSet.getString("owner_login");
                worker.getCollection().add(new LabWork(id, name, coordinates,
                        zonedDateTimeAdapter.unmarshal(creationDate), minimalPoint, difficulty, author, login));
            }
            return "Collection was loaded..";
        } catch (SQLException e) {
            e.printStackTrace();
            return "Collection can not be loaded now. Please try later.";
        }
    }

    public static void populatePreparedStatementForLabWork
            (PreparedStatement preparedStatement, LabWork labWork) throws SQLException {
        preparedStatement.setString(1, labWork.getName());
        preparedStatement.setDouble(2, labWork.getCoordinates().getX());
        preparedStatement.setLong(3, labWork.getCoordinates().getY());
        preparedStatement.setString(4, labWork.getCreationDate().toString());
        preparedStatement.setDouble(5, labWork.getMinimalPoint());
        preparedStatement.setObject(6, labWork.getDifficulty());
        preparedStatement.setString(7, labWork.getAuthor().getName());
        preparedStatement.setString(8, labWork.getAuthor().getPassportId());
        preparedStatement.setObject(9, labWork.getAuthor().getEyeColor());
        preparedStatement.setObject(10, labWork.getAuthor().getHairColor());
        preparedStatement.setObject(11, labWork.getAuthor().getNationality());
        preparedStatement.setFloat(12, labWork.getAuthor().getLocation().getX());
        preparedStatement.setLong(13, labWork.getAuthor().getLocation().getY());
        preparedStatement.setInt(14, labWork.getAuthor().getLocation().getZ());
        preparedStatement.setString(15, labWork.getLogin());
    }

//    public void createMainBase() throws SQLException {
//        connection.prepareStatement(DatabaseCommands.allTablesCreation).execute();
//        System.out.println("Tables have been created");
//    }

    public boolean register(String login, String password) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(DatabaseCommands.login);
            preparedStatement.setString(1, login);
            ResultSet resultSet = preparedStatement.executeQuery();
            resultSet.next();
            if (resultSet.getInt(1) == 0) {
                PreparedStatement preparedStatement2 = connection.prepareStatement(DatabaseCommands.register);
                preparedStatement2.setString(1, login);
                preparedStatement2.setString(2, PasswordHashed.hashPassword(password, salt));
                preparedStatement2.execute();
                return true;
            } else {
                System.out.println("User is already exist.");
                return false;
            }
        } catch (SQLException sqlException) {
            sqlException.printStackTrace();
            System.out.println("User is already exist.");
            return false;
        }
    }

    public boolean login(String login, String password) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(DatabaseCommands.login);
            preparedStatement.setString(1, login);
            preparedStatement.setString(2, PasswordHashed.hashPassword(password, salt));
            ResultSet resultSet = preparedStatement.executeQuery();
            resultSet.next();
            if (resultSet.getInt(1) == 1) {
                DatabaseManager.getUserCredentials().setLogin(login);
                return true;
            } else if (resultSet.getInt(1) > 1) {
                System.out.println("Database error. Login is not unique. Try later.");
                System.exit(1);
                return false;
            } else throw new SQLException();
        } catch (SQLException sqlException) {
            System.out.println("User not found. You can register this account.");
            return false;
        }
    }

    public boolean addObject(LabWork labWork) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(DatabaseCommands.addObject);
            populatePreparedStatementForLabWork(preparedStatement, labWork);
            preparedStatement.executeUpdate();
            return true;
        } catch (SQLException sqlException) {
            System.out.println("Data loading error. Try later.");
            return false;
        }
    }

    public boolean updateObject(long id, LabWork labWork) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(DatabaseCommands.updateUserObject);
            populatePreparedStatementForLabWork(preparedStatement, labWork);
            preparedStatement.setLong(16, id);
            preparedStatement.executeUpdate();
            return true;
        } catch (SQLException sqlException) {
            System.out.println("Data loading error. Try later.");
            return false;
        }
    }

    public boolean deleteObject(int id, LabWork labWork) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(DatabaseCommands.deleteUserObject);
            preparedStatement.setInt(1, labWork.getId());
            preparedStatement.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Data removing error. Try later.");
            return false;
        }
    }

    public boolean deleteCollection() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(DatabaseCommands.deleteAllUserObject);
            preparedStatement.executeUpdate();
            return true;
        } catch (SQLException sqlException) {
            System.out.println("Data removing error. Try later.");
            return false;
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public static UserCredentials getUserCredentials() {
        return userCredentials;
    }
}

