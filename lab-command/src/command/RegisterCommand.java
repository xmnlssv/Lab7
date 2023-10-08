package command;

import endpoint.RequestSender;

public class RegisterCommand extends OneArgCommand{
    private String login;
    private String password;
    public RegisterCommand(RequestSender sender, String login, String password) {
        super(sender);
        this.login = login;
        this.password = password;
    }
    @Override
    void execute(String arg) {
        getSender().send(RegisterCommand.class, arg, login, password);
    }
}
