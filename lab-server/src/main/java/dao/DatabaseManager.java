package dao;

import authentication.UserCredentials;
import model.*;
import org.postgresql.util.PSQLException;
import utils.PropertiesUtil;
import utils.ZonedDateTimeAdapter;
import worker.PasswordHashed;
import worker.Worker;

import java.sql.*;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.logging.Logger;

public class DatabaseManager {
    public static final String URL_KEY = "db.url";
    public static final String USERNAME_KEY = "db.username";
    public static final String PASSWORD_KEY = "db.password";
    public static Worker worker = Worker.getInstance();
    private Connection connection; // abstract class for connection
    private String salt = PasswordHashed.generateSalt();
    private ZonedDateTimeAdapter zonedDateTimeAdapter = new ZonedDateTimeAdapter();
    private static UserCredentials userCredentials;
    private static Logger logger = Logger.getLogger(DatabaseManager.class.getName());

    public DatabaseManager() {
        while (true) {
            try {
                this.connection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/studs",
                        "s367911", "VA2515SVYu1DvXR1"
//                        PropertiesUtil.get(URL_KEY),
//                        PropertiesUtil.get(USERNAME_KEY),
//                        PropertiesUtil.get(PASSWORD_KEY)
                );
                System.out.println("Connection was established successfully.");
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


//    public String load() {
//        try (ResultSet resultSet = connection.createStatement().executeQuery(DatabaseCommands.getAllObjects)) {
//            worker.getCollection().clear();
//            while (resultSet.next()) {
//                int id = resultSet.getInt("id");
//                String name = resultSet.getString("name");
//                Coordinates coordinates = new Coordinates(resultSet.getDouble("x"),
//                        resultSet.getLong("y"));
//                String creationDate = resultSet.getString("creation_date");
//                long minimalPoint = resultSet.getInt("minimal_point");
//                Difficulty difficulty = Difficulty.valueOf(resultSet.getString("difficulty"));
//                Person author = new Person(resultSet.getString("name"), resultSet.
//                        getString("passport_id"), EyeColor.valueOf(resultSet.getString("eye_color")),
//                        HairColor.valueOf(resultSet.getString("hair_color")), Country.valueOf(resultSet.
//                                getString("nationality")), new Location(resultSet.
//                                getFloat("x_location"),
//                                resultSet.getLong("y_location"), resultSet.getInt("z_location")));
//                EyeColor eyeColor = EyeColor.valueOf(resultSet.getString("eye_color"));
//                HairColor hairColor = HairColor.valueOf(resultSet.getString("hair_color"));
//                Country nationality = Country.valueOf(resultSet.getString("nationality"));
//                Location location = new Location(resultSet.getFloat("x_location"),
//                        resultSet.getLong("y_location"), resultSet.getInt("z_location"));
//                String login = resultSet.getString("owner_login");
//                worker.getCollection().add(new LabWork(id, name, coordinates,
//                        zonedDateTimeAdapter.unmarshal(creationDate), minimalPoint, difficulty, author, login));
//            }
//            return "Collection was loaded..";
//        } catch (SQLException e) {
//            e.printStackTrace();
//            return "Collection can not be loaded now. Please try later.";
//        }
//    }

    /**
     * Loads collection from database
     *
     * @return result of loading
     */
    public ArrayList<LabWork> loadCollection() {
        try {
            PreparedStatement statement = connection.prepareStatement(DatabaseCommands.getAllObjects);
            ResultSet resultSet = statement.executeQuery();
            ArrayList<LabWork> collection = new ArrayList<>();
            while (resultSet.next()) {
                Coordinates coordinates = new Coordinates(
                        resultSet.getDouble("cord_x"), resultSet.getLong("cord_y"));
                Location location = new Location(
                        resultSet.getFloat("author_location_x"),
                        resultSet.getLong("author_location_y"),
                        resultSet.getInt("author_location_z"));
                Difficulty difficulty = null;
                if (resultSet.getString("difficulty") != null) {
                    difficulty = Difficulty.valueOf(resultSet.getString("difficulty"));
                }
                EyeColor eyeColor = null;
                if (resultSet.getString("author_eye_color") != null) {
                    eyeColor = EyeColor.valueOf(resultSet.getString("author_eye_color"));
                }
                HairColor hairColor = null;
                if (resultSet.getString("author_hair_color") != null) {
                    hairColor = HairColor.valueOf(resultSet.getString("author_hair_color"));
                }
                Country nationality = null;
                if (resultSet.getString("author_nationality") != null) {
                    nationality = Country.valueOf(resultSet.getString("author_nationality"));
                }

                Person author = new Person(
                        resultSet.getString("author_name"),
                        resultSet.getString("author_passport_id"),
                        eyeColor,
                        hairColor,
                        nationality,
                        location
                );

                LabWork labWork = new LabWork(
                        resultSet.getInt("id"),
                        resultSet.getString("lab_work_name"),
                        coordinates,
                        resultSet.getDate("creation_date").toLocalDate().atStartOfDay(ZoneId.systemDefault()),
                        resultSet.getDouble("minimal_point"),
                        difficulty,
                        author,
                        resultSet.getString("owner_login")
                );
                collection.add(labWork);
            }
            System.out.println("The user's collection is loaded. Number of items – " + collection.size());
            return collection;
        } catch (SQLException e) {
            throw new RuntimeException(e);
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

    public boolean loginExist(String login) throws SQLException{
            PreparedStatement checkStatement = connection.prepareStatement(DatabaseCommands.getUserCredentials);
            checkStatement.setString(1, login);
            ResultSet resultSet = checkStatement.executeQuery();
            return resultSet.next();
    }
    public boolean register(String login, String password){
        try {
            if (loginExist(login)) {
                return false;
            }
            PreparedStatement preparedStatement = connection.prepareStatement(DatabaseCommands.register);
            preparedStatement.setString(1, login);
            preparedStatement.setString(2, PasswordHashed.hashPassword(password, salt));
            preparedStatement.setString(3, salt);
            preparedStatement.executeUpdate();
            preparedStatement.close();
            System.out.println("New user added.");
            return true;

        } catch (SQLException sqlException) {
            sqlException.printStackTrace();
            System.out.println("Failed to add a user.");
            return false;
        }
    }

    public boolean login(String login, String password) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(DatabaseCommands.getUserCredentials);
            preparedStatement.setString(1, login);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                String truePassword = resultSet.getString("password");
                String trueSalt = resultSet.getString("salt");
                String checkPassword = PasswordHashed.hashPassword(password, trueSalt);
                return checkPassword.equals(truePassword);
            } else {
                return false;
            }
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

