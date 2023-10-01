package command;

import endpoint.RequestSender;

public class RegisterCommand extends OneArgCommand{
    public RegisterCommand(RequestSender sender) {
        super(sender);
    }
    @Override
    void execute(String arg) {
        getSender().send(RegisterCommand.class, arg);
    }
}
