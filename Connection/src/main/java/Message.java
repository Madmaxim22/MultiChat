import java.io.Serializable;
import java.util.Set;

public class Message implements Serializable {

    private MessageType messageType; // тип сообщения
    private String textMessage; // текст сообщения

    private String userName; // имя пользователя
    private Set<String> listUser; // множество имен уже подключившихся пользователей

    public Message(MessageType messageType, String userName, String textMessage) {
        this.messageType = messageType;
        this.userName = userName;
        this.textMessage = textMessage;
        this.listUser = null;
    }

    public Message(MessageType messageType, String textMessage) {
        this.messageType = messageType;
        this.userName = null;
        this.textMessage = textMessage;
        this.listUser = null;
    }

    public Message(MessageType messageType, Set<String> listUser) {
        this.messageType = messageType;
        this.userName = null;
        this.textMessage = null;
        this.listUser = listUser;
    }

    public Message(MessageType messageType) {
        this.messageType = messageType;
        this.userName = null;
        this.textMessage = null;
        this.listUser = null;
    }

    public String getUserName() {
        return userName;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public String getTextMessage() {
        return textMessage;
    }

    public Set<String> getListUser() {
        return listUser;
    }
}
