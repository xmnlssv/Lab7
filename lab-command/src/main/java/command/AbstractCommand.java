package command;

import endpoint.RequestSender;

public abstract class AbstractCommand implements Command {
    private RequestSender sender;
    private String login;
    String password;
    public AbstractCommand(RequestSender sender) {
        this.sender = sender;
    }

    public RequestSender getSender() {
        return sender;
    }
}
