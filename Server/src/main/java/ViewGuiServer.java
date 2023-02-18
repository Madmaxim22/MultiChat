import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ViewGuiServer {
    private static final Logger logger = LoggerFactory.getLogger(ViewGuiServer.class);

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
}
