import java.util.HashMap;
import java.util.Map;

public class ModelGuiServer {

    private Map<String, Connection> allUsersMultiChat = new HashMap<>();

    Map<String, Connection> getAllUsersMultiChat() {
        return allUsersMultiChat;
    }

    void addUser(String name, Connection connection) {
        allUsersMultiChat.put(name, connection);
    }

    void removeUser(String name) {
        allUsersMultiChat.remove(name);
    }
}
