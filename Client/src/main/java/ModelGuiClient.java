import java.util.HashSet;
import java.util.Set;

public class ModelGuiClient {
    //в модели клиентского приложения хранится множество подключившихся пользователей
    private Set<String> users = new HashSet<>();

    protected Set<String> getUsers() {
        return users;
    }

    protected void setUsers(Set<String> users) {
        this.users = users;
    }

    protected void addUser(String nameUser) {
        users.add(nameUser);
    }

    protected void removeUser(String nameUser) {
        users.remove(nameUser);
    }
}
