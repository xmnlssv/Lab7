package dao;

public class DatabaseCommands {
    public static final String allTablesCreation = """
            CREATE TYPE DIFFICULTY AS ENUM(
                'EASY',
                'NORMAL',
                'IMPOSSIBLE',
                'INSANE'
            );
            CREATE TYPE EYE_COLOR AS ENUM (
                'BLUE',
                'YELLOW',
                'ORANGE',
                'WHITE'
            );
            CREATE TYPE HAIR_COLOR AS ENUM (
                'GREEN',
                'BLUE',
                'YELLOW',
                'ORANGE',
                'WHITE'
            );
            CREATE TYPE COUNTRY AS ENUM (
                'UNITED_KINGDOM',
                'FRANCE',
                'INDIA',
                'SOUTH_KOREA'
            );    
            CREATE TABLE IF NOT EXISTS lab_works (
                id SERIAL PRIMARY KEY,
                lab_work_name TEXT NOT NULL,
                cord_x NUMERIC NOT NULL,
                cord_y NUMERIC NOT NULL,
                creation_date TIMESTAMP NOT NULL,
                minimal_point INT NOT NULL,
                difficulty DIFFICULTY NOT NULL,
                author_name TEXT NOT NULL,
                author_passport_id TEXT NOT NULL,
                author_eye_color EYE_COLOR NOT NULL,
                author_hair_color HAIR_COLOR NOT NULL,
                author_nationality COUNTRY,
                author_location_x NUMERIC NOT NULL,
                author_location_y NUMERIC NOT NULL,
                author_location_z NUMERIC NOT NULL,
                owner_login TEXT NOT NULL
            );
            CREATE TABLE IF NOT EXISTS user_credentials (
                 id SERIAL PRIMARY KEY,
                 lab_work_id INT REFERENCES lab_work(id),
                 login TEXT UNIQUE NOT NULL,
                 password_hash TEXT NOT NULL      
                 salt TEXT NOT NULL
            );
            """;
    public static final String addUserCredentials = """
            INSERT INTO user_credentials(login, password_hash, salt) VALUES(?, ?, ?);""";

    public static final String getUserCredentials = """
            SELECT * FROM user_credentials WHERE (login = ?);""";

    public static final String addObject = """
            INSERT INTO lab_works(lab_work_name, cord_x, cord_y, creation_date, minimal_point, difficulty, author_name,
            author_passport_id, author_eye_color, author_hair_color, author_nationality, author_location_x,
            author_location_y, author_location_z, owner_login)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id;
            """;

    public static final String getAllObjects = """
            SELECT * FROM lab_works;
            """;

    public static final String deleteUserObject = """
            DELETE FROM lab_works WHERE (owner_login = ?) AND (id = ?) RETURNING id;
            """;

    public static final String deleteAllUserObject = """
            DELETE FROM lab_works WHERE (owner_login = ?) RETURNING id;
            """;

    public static final String updateUserObject = """
            UPDATE lab_works
            SET (lab_work_name, cord_x, cord_y, creation_date, minimal_point, difficulty, author_name,
            author_passport_id, author_eye_color, author_hair_color, author_nationality, author_location_x,
            author_location_y, author_location_z)
            = (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            WHERE (id = ?) AND (owner_login = ?)
            RETURNING id;
            """;

    public static final  String login = """
            SELECT COUNT(*) FROM user_credentials WHERE login = ?;""";

    public static final String register = """
            INSERT INTO user_credentials (login, password) values (?, ?);""";

}
