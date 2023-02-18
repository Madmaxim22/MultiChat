import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Set;


public class ViewGuiClient {
    private static final Logger logger = LoggerFactory.getLogger(ViewGuiClient.class);

    BufferedReader buf;

    public ViewGuiClient() {
        buf = new BufferedReader(new InputStreamReader(System.in));
    }

    // Вывод сервисных сообщений в консоль
    void serviceMessage(String text) {
        System.out.println(text);
        logger.warn(text);
    }

    // Вывод сообщений в консоль
    void usersMessage(String text) {
        System.out.print(text);
        logger.info(text);
    }

    // Вывод ошибок в консоль
    protected void errorMessage(String text) {
        System.err.println(text);
        logger.error(text);
    }

    // Ввод сообщения из консоли
    protected String messagesFromTheConsole() throws IOException {
        String msg = buf.readLine();
        logger.info(msg);
        return msg;
    }

    //метод обновляющий список имен подключившихся пользователей
    protected void refreshListUsers(Set<String> listUsers) {
        int number = 0;
        if (listUsers.size() == 1) {
            System.out.println("Вы единственный в пользователь в чате.");
        } else {
            System.out.println("Пользователи в сети: ");
            for (String user : listUsers) {
                System.out.print("{ " + user + " }");
                number++;
                if (number % 10 == 0) {
                    System.out.println("->");
                }
            }
            System.out.println();
        }
    }
}
