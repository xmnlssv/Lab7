package server;

import ch.qos.logback.classic.Logger;
import connection.ConnectionManager;
import dao.DatabaseCommands;
import dao.DatabaseHandler;
import dao.DatabaseManager;
import model.LabWork;
import model.LabWorksArrayList;
import utils.LogUtil;
import worker.Worker;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

public class Server {
    public static Worker worker = Worker.getInstance();
    private static Logger logger = LogUtil.getLogger("server");
    private static Connection connection;
    private LabWorksArrayList labWorksArrayList;

    private static void save() throws SQLException {
        logger.info("Saving collection to database...");
        Statement request = DatabaseHandler.getDatabaseManager().getConnection().createStatement();
        DatabaseHandler.getDatabaseManager().getConnection().setAutoCommit(false);
        request.addBatch("DELETE FROM lab_works");
        for(LabWork labWork : LabWorksArrayList.getLabWorks()) {
            String sql = DatabaseCommands.addObject;
            PreparedStatement preparedStatement = DatabaseHandler.getDatabaseManager().getConnection().
                    prepareStatement(sql);
            DatabaseManager.populatePreparedStatementForLabWork(preparedStatement, labWork);
            String st = preparedStatement.toString();
            request.addBatch(st);
        }
        request.executeBatch();
        DatabaseHandler.getDatabaseManager().getConnection().commit();
        System.out.println("Changes are saved successfully");
    }
//    private static void save(String collectionFile) {
//        logger.info("Saving collection to database...");
//        String asString = CollectionSerializer.marshal(worker.getCollection());
//        try {
//            PrintWriter printWriter = new PrintWriter(collectionFile);
//            printWriter.println(asString);
//            printWriter.flush();
//            printWriter.close();
//            logger.info("Successfully saved collection to file!");
//            System.out.println("Successfully saved collection to file!");
//        } catch (FileNotFoundException exception) {
//            System.err.println(String.format("Error saving collection: file %s not found", collectionFile));
//            logger.error("Error saving collection: file {} not found.", collectionFile);
//        }
//    }

    public static void main(String[] args) throws SQLException {
        String collectionFile = System.getenv("PATH999");
        ConnectionManager.initialize();
        Worker.initialize();
        Scanner in = new Scanner(System.in);
        while (true) {
            System.out.println("Enter a server command:");
            String command = in.nextLine();
            if (command.equals("save")) {
                save();
            } else if (command.equals("exit")) {
                save();
                logger.info("Server exiting...");
                System.exit(0);
            } else {
                System.out.println("Unknown command. Please type [save] or [exit].");
            }
        }
    }
}
