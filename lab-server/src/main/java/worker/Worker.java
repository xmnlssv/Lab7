package worker;

import authentication.UserCredentials;
import command.*;
import dao.DatabaseCommands;
import dao.DatabaseHandler;
import dao.DatabaseManager;
import input.InputMode;
import input.InputReader;
import response.Response;
import response.Result;
import model.LabWork;
import model.Person;
import serializer.CollectionSerializer;
import serializer.ServerJsonSerializer;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Represents receiver.
 */
public class Worker {
    private static String filePath;
    private static ArrayList<LabWork> collection = new ArrayList<>();
    private static final Set<String> CALL_STACK = new LinkedHashSet<>();
    private static HashMap<String, String> commands = new HashMap<>();
    private static final HashMap<String, Supplier<Response>> zeroArgMappings = new HashMap<>();
    private static final HashMap<String, Function<String, Response>> oneArgMappings = new HashMap<>();
    private static Worker INSTANCE = null;

    private Worker() {

    }

    public static void initialize() throws SQLException {
        collection = DatabaseHandler.getDatabaseManager().loadCollection();
        PassportIdHelper.initialize(collection);
        IdHelper.initialize(collection);
    }

    static {
        commands.put("help", "get help on alternative commands");
        commands.put("info", "print all collection items into the string representation");
        commands.put("show", "output to the standard output stream all elements of the collection " +
                "in string representation");
        commands.put("add {element}", "add new item to collection");
        commands.put("update id {element}", "update the value of the collection element " +
                "whose Id matches the given one");
        commands.put("remove_by_id id", "remove an element from the collection by its Id");
        commands.put("clear", "remove all items from collection");
        commands.put("execute_script filename", "read and execute script from given file. " +
                "The same views are found in the script as in the interactive mode");
        commands.put("remove_last", "remove the last item from the collection");
        commands.put("remove_greater {element}", "remove all items from the collection that exceed the specified");
        commands.put("sort", "sort the collection in natural order");
        commands.put("remove_any_by_author author", "remove one item from the collection whose author field value" +
                " is equivalent to a given");
        commands.put("min_by_author", "output any object in the collection whose author field value is the minimum");
        commands.put("print_field_descending_minimal_point",
                "output the values of the field minimalPoint of all elements in descending order");
        commands.put("log_in", "Sign in to your account");
        commands.put("register", "Create your account");

        zeroArgMappings.put("help", () -> getInstance().helpAction());
//        zeroArgMappings.put("info", () -> getInstance().infoAction());
        zeroArgMappings.put("show", () -> getInstance().showAction());
        zeroArgMappings.put("sort", () -> getInstance().sortAction());
        zeroArgMappings.put("clear", () -> getInstance().clearAction());
        zeroArgMappings.put("min_by_author", () -> getInstance().minByAuthorAction());
        zeroArgMappings.put("remove_last", () -> getInstance().removeLastAction());
        zeroArgMappings.put("print_field_descending_minimal_point", () -> getInstance().descendingMinimalPointAction());
        oneArgMappings.put("add", (arg) -> getInstance().addAction((LabWork) ServerJsonSerializer.
                deserialize(arg, LabWork.class)));
        oneArgMappings.put("remove_greater", (arg) -> getInstance().removeGreaterAction(Integer.parseInt(arg)));
        oneArgMappings.put("remove_any_by_author", (arg) -> getInstance().removeByAuthorAction((Person)
                ServerJsonSerializer.deserialize(arg, Person.class)));
        oneArgMappings.put("remove_by_id", (arg) -> getInstance().removeByIdAction(Integer.parseInt(arg)));
//        oneArgMappings.put("log_in", (arg) -> getInstance().loginAction((UserCredentials) ServerJsonSerializer.
//                deserialize(arg, UserCredentials.class)));
//        oneArgMappings.put("register", (arg) -> getInstance().registerAction((UserCredentials) ServerJsonSerializer.
//            deserialize(arg, UserCredentials.class)));
    }

    public static Worker getInstance() {
        if (INSTANCE != null) {
            return INSTANCE;
        }
        return new Worker();
    }

    public ArrayList<LabWork> getCollection() {
        return collection;
    }

    public Response addAction(LabWork labWork) {
        try {
            if (DatabaseManager.getUserCredentials().getLogin() != null) {
                labWork.setId(IdHelper.generateId());
                if (IdHelper.containsId(labWork.getId()) || PassportIdHelper.containsId(labWork.getAuthor().getPassportId())) {
                    return new Response(
                            AddCommand.class,
                            new String[]{ServerJsonSerializer.serialize(labWork)},
                            "Duplicate id / passport id",
                            Result.FAILURE
                    );
                } else {
                    IdHelper.saveId(labWork.getId());
                    boolean isInsertSuccessful = DatabaseHandler.getDatabaseManager().addObject(labWork);
                    if (isInsertSuccessful) {
                        collection.add(labWork);
                    } else {
                        return new Response(
                                AddCommand.class,
                                new String[]{ServerJsonSerializer.serialize(labWork)},
                                "Element was not added.",
                                Result.FAILURE
                        );
                    }
                    PassportIdHelper.saveId(labWork.getAuthor().getPassportId());
                    return new Response(
                            AddCommand.class,
                            new String[]{ServerJsonSerializer.serialize(labWork)},
                            null,
                            Result.SUCCESS
                    );
                }
            } else {
                return new Response(
                        AddCommand.class,
                        new String[]{ServerJsonSerializer.serialize(labWork)},
                        "Unauthorised users can only use the \"log_in\", \"register\", \"show\", \"help\" commands",
                        Result.SUCCESS
                );
            }
        } catch (NullPointerException nullPointerException) {
            System.out.println("Unauthorised users can only use the \"log_in\", \"register\", \"show\", \"help\" commands");
        }
        return null;
    }

    public Response clearAction() {
        if (DatabaseManager.getUserCredentials().getLogin() != null) {
            boolean isDeleteSuccessful = DatabaseHandler.getDatabaseManager().deleteCollection();
            if (isDeleteSuccessful) {
                collection.clear();
            } else {
                return new Response(
                        ClearCommand.class,
                        new String[]{},
                        "The table has not clear",
                        Result.SUCCESS
                );
            }
            IdHelper.removeAll();
            return new Response(
                    ClearCommand.class,
                    new String[]{},
                    null,
                    Result.SUCCESS
            );
        } else {

        }
        return new Response(
                ClearCommand.class,
                new String[]{},
                "Unauthorised users can only use the \"log_in\", \"register\", \"show\", \"help\" commands",
                Result.FAILURE
        );
    }

    public Response descendingMinimalPointAction() {
        if (DatabaseManager.getUserCredentials().getLogin() != null) {
            if (collection.isEmpty()) {
                return new Response(
                        DescendingMinimalPointCommand.class,
                        new String[]{},
                        "The list is empty",
                        Result.SUCCESS
                );
            }
            StringBuilder stringBuilder = new StringBuilder();
            collection.stream().sorted(Comparator.comparing(LabWork::getMinimalPoint).reversed()).forEach((labWork) -> {
                stringBuilder.append(labWork.getMinimalPoint()).append("\n");
            });
            return new Response(
                    DescendingMinimalPointCommand.class,
                    new String[]{},
                    stringBuilder.toString(),
                    Result.SUCCESS
            );
        } else {
            return new Response(
                    DescendingMinimalPointCommand.class,
                    new String[]{},
                    "\"Unauthorised users can only use the \"log_in\", \"register\", \"show\", \"help\" \"commands\"",
                    Result.FAILURE
            );
        }
    }

    public Response[] executeScriptAction(String scriptPath) {
        if (DatabaseManager.getUserCredentials().getLogin() != null) {
            if (!CALL_STACK.contains(scriptPath)) {
                CALL_STACK.add(scriptPath);
                InputReader inputReader;
                try {
                    Scanner scanner = new Scanner(new FileInputStream(scriptPath));
                    inputReader = new InputReader(scanner, InputMode.AUTO);
                    List<Response> responses = new ArrayList<>();
                    responses.add(new Response(
                            ExecuteScriptCommand.class,
                            new String[]{scriptPath},
                            null,
                            Result.SUCCESS
                    ));
                    while (scanner.hasNextLine()) {
                        String line = scanner.nextLine();
                        String[] split = line.split(" ");
                        if (split.length == 0) {
                            continue;
                        }
                        if (zeroArgMappings.containsKey(split[0])) {
                            responses.add(zeroArgMappings.get(split[0]).get());
                        } else if (oneArgMappings.containsKey(split[0])) {
                            responses.add(oneArgMappings.get(split[0]).apply(split[1]));
                        }
                    }
                    Response[] responseArray = new Response[responses.size()];
                    responses.toArray(responseArray);
                    CALL_STACK.remove(scriptPath);
                    return responseArray;
                } catch (FileNotFoundException exception) {
                    return new Response[]{new Response(
                            ExecuteScriptCommand.class,
                            new String[]{scriptPath},
                            null,
                            Result.FAILURE
                    )};
                }
            } else {
                return new Response[]{new Response(
                        ExecuteScriptCommand.class,
                        new String[]{scriptPath},
                        null,
                        Result.FAILURE
                )};
            }
        } else {
            return new Response[]{new Response(
                    ExecuteScriptCommand.class,
                    new String[]{scriptPath},
                    "Unauthorised users can only use the \"log_in\", \"register\", \"show\", \"help\" commands",
                    Result.FAILURE
            )};
        }
    }

    public Response helpAction() {
        StringBuilder tutorial = new StringBuilder();
        for (Map.Entry<String, String> entry : commands.entrySet()) {
            String key = String.format("%-43s", entry.getKey());
            String value = String.format(" : %1$s", entry.getValue());
            tutorial.append(key).append(value).append("\n");
        }
        return new Response(
                HelpCommand.class,
                new String[]{},
                tutorial.toString(),
                Result.SUCCESS
        );
    }

    public Response infoAction(String login, String password) {
        if (DatabaseManager.getUserCredentials().getLogin() != null) {
            if (DatabaseManager.getUserCredentials().getLogin() != null) {
                StringBuilder info = new StringBuilder();
                info
                        .append("Type of collection: ")
                        .append(collection.getClass())
                        .append("\n")
                        .append("Date of initialization: ")
                        .append(ZonedDateTime.now())
                        .append("\n")
                        .append("Amount of elements: ")
                        .append(collection.size())
                        .append("\n");
                return new Response(
                        InfoCommand.class,
                        new String[]{},
                        info.toString(),
                        Result.SUCCESS
                );
            } else {
                return new Response(
                        InfoCommand.class,
                        new String[]{},
                        "You are not logged in",
                        Result.FAILURE
                );
            }
        } else {
            return new Response(
                    InfoCommand.class,
                    new String[]{},
                    "Unauthorised users can only use the \"log_in\", \"register\", \"show\", \"help\" commands",
                    Result.FAILURE
            );
        }
    }

    public Response loginAction(UserCredentials userCredentials, String login, String password) {
        if (DatabaseHandler.getDatabaseManager().login(userCredentials.getLogin(), userCredentials.getPassword())) {
            return new Response(
                    LoginCommand.class,
                    new String[]{ServerJsonSerializer.serialize(userCredentials)},
                    null,
                    Result.SUCCESS
            );
        }
        return new Response(
                LoginCommand.class,
                new String[]{ServerJsonSerializer.serialize(userCredentials)},
                "Incorrect login or password",
                Result.FAILURE
        );
    }
    public Response minByAuthorAction() {
        if (DatabaseManager.getUserCredentials().getLogin() != null) {
            if (collection.isEmpty()) {
                return new Response(
                        MinByAuthorCommand.class,
                        new String[]{},
                        null,
                        Result.FAILURE
                );
            }
            LabWork minLabWork = collection.stream().sorted().iterator().next();
            return new Response(
                    MinByAuthorCommand.class,
                    new String[]{},
                    ServerJsonSerializer.serialize(minLabWork),
                    Result.SUCCESS
            );
        } else {
            return new Response(
                    MinByAuthorCommand.class,
                    new String[]{},
                    "ServerJsonSerializer.serialize(minLabWork)",
                    Result.SUCCESS
            );
        }
    }

    public Response registerAction(UserCredentials userCredentials) {
        if (DatabaseHandler.getDatabaseManager().register(userCredentials.getLogin(), userCredentials.getPassword())) {
            return new Response(
                    RegisterCommand.class,
                    new String[]{ServerJsonSerializer.serialize(userCredentials)},
                    null,
                    Result.SUCCESS
            );
        }
        return new Response(
                RegisterCommand.class,
                new String[]{ServerJsonSerializer.serialize(userCredentials)},
                "User with this login already exists",
                Result.FAILURE
        );
    }
    public Response removeByAuthorAction(Person author) {
        if (DatabaseManager.getUserCredentials().getLogin() != null) {
            Optional<LabWork> forDeletion = collection.stream().filter(labWork -> labWork.getAuthor().equals(author)).findAny();
            if (!forDeletion.isEmpty()) {
                return new Response(
                        RemoveByAuthorCommand.class,
                        new String[]{ServerJsonSerializer.serialize(forDeletion.get())},
                        null,
                        Result.FAILURE
                );
            } else {
                return new Response(
                        RemoveByAuthorCommand.class,
                        new String[]{ServerJsonSerializer.serialize(author)},
                        ServerJsonSerializer.serialize(forDeletion.get()),
                        Result.SUCCESS
                );
            }
        } else {
            return new Response(
                    RemoveByAuthorCommand.class,
                    new String[]{ServerJsonSerializer.serialize(author)},
                    "Unauthorised users can only use the \"log_in\", \"register\", \"show\", \"help\" commands",
                    Result.SUCCESS
            );
        }
    }

    public Response removeByIdAction(long id) {
        if (DatabaseManager.getUserCredentials().getLogin() != null) {
            Optional<LabWork> forDeletion = collection.stream().filter(labWork -> labWork.getId() == id).findAny();
            if (forDeletion.isEmpty()) {
                return new Response(
                        RemoveByIdCommand.class,
                        new String[]{id + ""},
                        null,
                        Result.FAILURE
                );
            } else {
                boolean isDeleteSuccessful = DatabaseHandler.getDatabaseManager().deleteCollection();
                if (isDeleteSuccessful) {
                    collection.remove(forDeletion.get());
                } else {
                    return new Response(
                            RemoveByIdCommand.class,
                            new String[]{id + ""},
                            "The table has not been removed",
                            Result.FAILURE
                    );
                }
                return new Response(
                        RemoveByIdCommand.class,
                        new String[]{id + ""},
                        ServerJsonSerializer.serialize(forDeletion.get()),
                        Result.SUCCESS
                );
            }
        } else {
                return new Response(
                        RemoveByIdCommand.class,
                        new String[]{id + ""},
                        "Unauthorised users can only use the \"log_in\", \"register\", \"show\", \"help\" commands",
                        Result.SUCCESS
                );
        }
    }

    public Response removeGreaterAction(long id) {
        if (DatabaseManager.getUserCredentials().getLogin() != null) {
            List<LabWork> forDeletion = collection.stream().filter(labWork -> labWork.getId() > id).toList();
            collection.removeAll(forDeletion);
            return new Response(
                    RemoveGreaterCommand.class,
                    new String[]{id + ""},
                    null,
                    Result.SUCCESS
            );
        } else {
            return new Response(
                    RemoveGreaterCommand.class,
                    new String[]{id + ""},
                    "Unauthorised users can only use the \"log_in\", \"register\", \"show\", \"help\" commands",
                    Result.SUCCESS
            );
        }
    }

    public Response removeLastAction() {
        if (collection.isEmpty()) {
            return new Response(
                    RemoveLastCommand.class,
                    new String[]{},
                    null,
                    Result.FAILURE
            );
        }
        Integer id = (Integer) collection.get(collection.size() - 1).getId();
        boolean isDeleteSuccessful = DatabaseHandler.getDatabaseManager().deleteCollection();
        if (isDeleteSuccessful) {
            IdHelper.removeId(id);
            collection.remove(collection.size() - 1);
        } else {
            return new Response(
                    RemoveLastCommand.class,
                    new String[]{},
                    "The element has been not removed",
                    Result.FAILURE
            );
        }
        return new Response(
                RemoveLastCommand.class,
                new String[]{},
                null,
                Result.SUCCESS
        );
    }

    public Response showAction() {
        StringBuilder info = new StringBuilder();
        collection.stream().sorted().forEach((labWork) -> {
            info.append(labWork.toString()).append("\n");
        });
        return new Response(
                ShowCommand.class,
                new String[]{},
                info.toString(),
                Result.SUCCESS
        );
    }

    public Response sortAction() {
        Collections.sort(collection);
        return new Response(
                SortCommand.class,
                new String[]{},
                null,
                Result.SUCCESS
        );
    }

    public Response updateAction(long id, LabWork updatedLabWork) {
        if (DatabaseManager.getUserCredentials().getLogin() != null) {
            Optional<LabWork> forUpdate = collection.stream().filter(labWork -> labWork.getId() == id).findAny();
            if (forUpdate.isEmpty()) {
                return new Response(
                        UpdateCommand.class,
                        new String[]{id + "", ServerJsonSerializer.serialize(updatedLabWork)},
                        null,
                        Result.FAILURE
                );
            }
            collection.remove(forUpdate.get());
            IdHelper.removeId(forUpdate.get().getId());
            PassportIdHelper.removeId(forUpdate.get().getAuthor().getPassportId());
            if (IdHelper.containsId(updatedLabWork.getId()) || PassportIdHelper.containsId(updatedLabWork.getAuthor().
                    getPassportId())) {
                collection.add(forUpdate.get());
                IdHelper.saveId(forUpdate.get().getId());
                PassportIdHelper.saveId(forUpdate.get().getAuthor().getPassportId());
                return new Response(
                        UpdateCommand.class,
                        new String[]{id + "", ServerJsonSerializer.serialize(updatedLabWork)},
                        "Duplicate id / passport id",
                        Result.FAILURE
                );
            } else {
                boolean isDatabaseUpdateSuccessful = DatabaseHandler.getDatabaseManager().updateObject(id, updatedLabWork);
                if (isDatabaseUpdateSuccessful) {
                    collection.add(updatedLabWork);
                } else {
                    return new Response(
                            UpdateCommand.class,
                            new String[]{id + "", ServerJsonSerializer.serialize(updatedLabWork)},
                            "The table has not been update",
                            Result.FAILURE
                    );
                }
                return new Response(
                        UpdateCommand.class,
                        new String[]{id + "", ServerJsonSerializer.serialize(updatedLabWork)},
                        null,
                        Result.SUCCESS
                );
            }
        } else {
            return new Response(
                    UpdateCommand.class,
                    new String[]{id + "", ServerJsonSerializer.serialize(updatedLabWork)},
                    "You are not logged in",
                    Result.FAILURE
            );
        }
    }
}
