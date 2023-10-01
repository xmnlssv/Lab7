package invoker;

import authentication.UserCredentials;
import com.fasterxml.jackson.core.JsonProcessingException;
import command.*;
import endpoint.RequestSender;
import input.InputReader;
import reader.CommandReader;

/**
 * Represents invoker.
 */
public class Invoker {
    private RequestSender sender = new RequestSender();

    private static boolean isLoginNotNull (String login) {
        if (login == null) {
            return false;
        }
        return true;
    }

    public void add(String arg) {
        handle(new AddCommand(sender), arg);
    }

    public void clear() {
        handle(new ClearCommand(sender));
    }

    public void descendingMinimalPoint() {
        handle(new DescendingMinimalPointCommand(sender));
    }

    public void executeScript(String arg) {
        handle(new ExecuteScriptCommand(sender), arg);
    }

    public void help() {
        handle(new HelpCommand(sender));
    }

    public void info() {
        handle(new InfoCommand(sender));
    }

    public void login(String arg) {
        handle(new LoginCommand(sender), arg);
    }

    public void minByAuthor() {
        handle(new MinByAuthorCommand(sender));
    }

    public void register(String arg) {
        handle(new RegisterCommand(sender), arg);
    }

    public void removeByAuthor(String arg) {
        handle(new RemoveByAuthorCommand(sender), arg);
    }

    public void removeById(String arg) {
        handle(new RemoveByIdCommand(sender), arg);
    }

    public void removeGreater(String arg) {
        handle(new RemoveGreaterCommand(sender), arg);
    }

    public void removeLast() {
        handle(new RemoveLastCommand(sender));
    }

    public void show() {
        handle(new ShowCommand(sender));
    }

    public void sort() {
        handle(new SortCommand(sender));
    }

    public void update(String arg1, String arg2) {
        handle(new UpdateCommand(sender), arg1, arg2);
    }

    private void handle(AbstractCommand command, String... args) {
        command.execute(args);
    }
}
