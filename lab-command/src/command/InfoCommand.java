package command;

import com.fasterxml.jackson.core.JsonProcessingException;
import endpoint.RequestSender;

public class InfoCommand extends NoArgsCommand {
    private String login;
    private String password;
    public InfoCommand(RequestSender sender, String login, String password) {
        super(sender);
        this.login = login;
        this.password = password;
    }

    @Override
    public void execute() {
        getSender().send(InfoCommand.class, login, password);
    }
}
