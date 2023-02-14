import org.apache.commons.validator.routines.InetAddressValidator;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.Properties;

public class Client {
    private static Connection connection;
    private static ModelGuiClient model;
    private static ViewGuiClient gui;
    private static volatile boolean isConnect = false; //флаг отображающий состояние подключения клиента серверу
    private static Socket socket;
    private static String addressServer;
    private static int portServer;

    //точка входа в клиентское приложение
    public static void main(String[] args) {

        Client client = new Client();
        model = new ModelGuiClient();
        gui = new ViewGuiClient();
        client.addAdrAndPortInConfigurationFile();
        client.connectToServer();
        if (isConnect) {
            client.nameUserRegistration();
            new Thread(new SenderRunnable()).start();
            new Thread(new ReceiverRunnable()).start();
        }
    }

    // метод подключения клиента к серверу
    protected void connectToServer() {
        //проверка на подключение клиента к серверу
        if (!isConnect) {
            while (true) {
                try {
                    //создаем сокет и объект connection
                    socket = createSocket();
                    connection = createConnection(socket);
                    isConnect = true;
                    gui.serviceMessage("Сервисное сообщение: Вы подключились к серверу.");
                    break;
                } catch (Exception e) {
                    gui.errorMessage("Произошла ошибка! Возможно Вы ввели не верный адрес сервера или порт. Попробуйте еще раз");
                    break;
                }
            }
        } else gui.errorMessage("Вы уже подключены!");
    }

    protected Socket createSocket() throws IOException {
        return new Socket(addressServer, portServer);
    }

    protected Connection createConnection(Socket socket) throws IOException {
        return  new Connection(socket);
    }

    protected void closeClient() throws IOException {
        connection.close();
        isConnect = false;
    }

    protected static void stopClient() {
        try {
            // если клиентский сокет не имеет ссылки или не запущен
            if (socket != null && !socket.isClosed()) {
                gui.closeBuf();
                socket.close();
                model.getUsers().clear();
                gui.serviceMessage("Клиент остановлен.");
            } else gui.serviceMessage("Клиент не запущен останавливать нечего!");
        } catch (IOException e) {
            gui.errorMessage("Остановить клиента не удалось");
        }
    }

    protected void addAdrAndPortInConfigurationFile() {
        String projectPath = System.getProperty("user.dir");
        Properties properties = new Properties();
        try (InputStream fis = new FileInputStream(projectPath + "/Client/src/main/resources/client.properties")) {
            properties.load(fis);
            String address = properties.getProperty("address");
            if (addressVerification(address)) {
                addressServer = address;
                gui.serviceMessage("Сервисное сообщение: Адрес принят!");
            } else {
                gui.serviceMessage("Сервисное сообщение: Неверный адрес!");
            }
            portServer = Integer.parseInt(properties.getProperty("port"));
            gui.serviceMessage("Сервисное сообщение: Порт принят!");
        } catch (IOException e) {
            gui.errorMessage("Ошибка: Файл конфигурации отсутствует!");
        }
    }

    protected boolean addressVerification (String address) {
        InetAddressValidator validator = new InetAddressValidator();
        return validator.isValidInet4Address(address);
    }

    // метод реализующий регистрацию имени пользователя со стороны пользовательского приложения
    protected void nameUserRegistration() {
        while (true) {
            try {
                Message message = connection.receive();
                // приняли от сервера сообщение, если это запрос имени, то вызываем окна ввода имени, отправляем на сервер имя
                if (message.getMessageType() == MessageType.REQUEST_NAME_USER) {
                    gui.serviceMessage("Введите имя пользователя: ");
                    String nameUser = gui.messagesFromTheConsole();
                    connection.send(new Message(MessageType.USER_NAME, nameUser));
                }
                // если сообщение - имя уже используется, выводим соответствующее окно с ошибкой, повторяем ввод имени
                if (message.getMessageType() == MessageType.NAME_USER) {
                    gui.serviceMessage("Сервисное сообщение: Данное имя уже используется, введите другое");
                    continue;
                }
                // если имя принято получаем от сервера множество всех подключившихся пользователей, выходим из цикла
                if (message.getMessageType() == MessageType.NAME_ACCEPTED) {
                    gui.serviceMessage("Сервисное сообщение: Ваше имя принято!");
                    model.setUsers(message.getListUser());
                    gui.refreshListUsers(model.getUsers());
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
                gui.errorMessage("Произошла ошибка при регистрации имени! Попробуйте переподключиться.");
                try {
                    closeClient();
                    break;
                } catch (Exception exception) {
                    gui.errorMessage("Ошибка при закрытии соединения.");
                }
            }
        }
    }

    // Отправка сообщений с консоли
    static class SenderRunnable implements Runnable {

        private final String EXIT = "/exit";
        String msg;

        @Override
        public void run() {
            while (isConnect) {
                try {
                    msg = gui.messagesFromTheConsole();
                    if (msg.equals(EXIT)) {
                        connection.send(new Message(MessageType.DISABLE_USER));
                        stopClient();
                        break;
                    }
                    String[] ms = msg.split(" ");
                    if (model.getUsers().contains(ms[0])) {
                        connection.send(new Message(MessageType.USER_TEXT_MESSAGE, ms[0], msg));
                    } else {
                        connection.send(new Message(MessageType.TEXT_MESSAGE, msg));
                    }
                } catch (IOException e) {
                    gui.errorMessage("Ошибка при отправке сообщения из консоли.");
                }
            }
        }
    }

    // Получение сообщений с сервера от других пользователей
    static class ReceiverRunnable implements Runnable {
        @Override
        public void run() {
            while (isConnect) {
                try {
                    Message message = connection.receive();
                    //если тип TEXT_MESSAGE, то добавляем текст сообщения в окно переписки
                    if (message.getMessageType() == MessageType.TEXT_MESSAGE) {
                        gui.usersMessage(message.getTextMessage());
                    }
                    //если сообщение тип USER_ADDED добавляем сообщение в окно переписки о новом пользователе
                    if (message.getMessageType() == MessageType.USER_ADDED) {
                        model.addUser(message.getTextMessage());
                        gui.serviceMessage(String.format("Сервисное сообщение: пользователь %s присоединился к чату.\n", message.getTextMessage()));
                    }
                    //аналогично для отключения других пользователей
                    if (message.getMessageType() == MessageType.REMOVED_USER) {
                        model.removeUser(message.getTextMessage());
                        gui.serviceMessage(String.format("Сервисное сообщение: пользователь %s покинул чат.\n", message.getTextMessage()));
                    }
                } catch (Exception e) {
                    gui.errorMessage("Ошибка при приеме сообщения от сервера.");
                    isConnect = false;
                    break;
                }
            }
        }
    }
}
