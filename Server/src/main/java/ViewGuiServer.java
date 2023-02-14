import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ViewGuiServer {
    private static final Logger logger = LoggerFactory.getLogger(ViewGuiServer.class);
    private final BufferedReader buf;


    public ViewGuiServer() {
        buf = new BufferedReader(new InputStreamReader(System.in));
    }

    // Вывод сервисных сообщений в консоль
    protected void serviceMessage(String text) {
        System.out.println(text);
        logger.warn(text);
    }

    // метод, который выводит ошибки
    protected void errorMessage(String text) {
        System.err.println(text);
        logger.error(text);
    }

    // логирование сообщений пользователей
    protected void logMessage(String message) {
        logger.info(message);
    }

    // Ввод сообщения из консоли
    protected String messagesFromTheConsole() throws IOException {
        String msg = buf.readLine();
        logger.info(msg);
        return msg;
    }

    protected void closeBuf() throws IOException {
        buf.close();
    }
}
