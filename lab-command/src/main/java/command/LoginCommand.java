package command;

import endpoint.RequestSender;

import java.util.Scanner;

public class LoginCommand extends OneArgCommand{
    public LoginCommand(RequestSender sender) {
        super(sender);
    }

    @Override
    void execute(String arg) {
        getSender().send(LoginCommand.class, arg);
    }
}
