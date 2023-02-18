import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Server {

    private static ViewGuiServer gui; // Объект класса представления
    private static ModelGuiServer model; // Объект класса модели
    private static volatile boolean isServerStart = false; // Флаг отражающий состояние сервера
    int portSever;
    private ServerSocket serverSocket;

    // точка входа для приложения сервера
    public static void main(String[] args) {
        Server server = new Server();
        gui = new ViewGuiServer();
        model = new ModelGuiServer();
        server.addPortInConfigurationFile();
        server.startServer();
        new Thread(new SenderRunnable(server)).start();
        while (isServerStart) {
            server.acceptServer();
        }
    }

    protected void startServer() {
        try {
            serverSocket = createServerSocket();
            isServerStart = true;
            gui.serviceMessage("Сервер запущен на порту: " + serverSocket.getLocalPort());
        } catch (IOException e) {
            gui.errorMessage("Не удалось запустить сервер.");
        }
    }

    protected ServerSocket createServerSocket() throws IOException {
        return new ServerSocket(portSever);
    }

    protected void stopServer() {
        try {
            // если серверный сокет не имеет ссылки или не запущен
            if (serverSocket != null && !serverSocket.isClosed()) {
                MessageType messageType = MessageType.DISABLE_SERVER;
                Message message = new Message(messageType);
                for (Map.Entry<String, Connection> user : model.getAllUsersMultiChat().entrySet()) {
                    sendMessageUser(message, user.getKey());
                    user.getValue().close();
                }
                isServerStart = false;
                serverSocket.close();
                model.getAllUsersMultiChat().clear();
                gui.serviceMessage("Сервер остановлен.");
            } else gui.serviceMessage("Сервер не запущен останавливать нечего!");
        } catch (IOException e) {
            gui.errorMessage("Остановить сервер не удалось");
        }
    }

    // метод в котором бесконечный цикл сервера принимает новое сокетное подключение от клиента
    protected void acceptServer() {
        while (true) {
            try {
                Socket socket = serverSocket.accept();
                Connection connection = new Connection(socket);
                new Thread(new ReceiverRunnable(socket, connection)).start();
            } catch (IOException e) {
                gui.errorMessage("Связь с сервером потеряна!");
                return;
            }
        }
    }

    // метод рассылающий всем пользователям из мап заданное сообщение
    protected void sendMessageAllUsers(Message message, Connection connection) {
        for (Map.Entry<String, Connection> user : model.getAllUsersMultiChat().entrySet()) {
            try {
                if (user.getValue().equals(connection)) continue;
                user.getValue().send(message);
            } catch (IOException e) {
                gui.errorMessage("Ошибка отправки сообщения всем пользователям!");
            }
        }
    }

    // метод реализующий отправку личных сообщений
    protected void sendMessageUser(Message message, String userName) {
        try {
            Connection connection = model.getAllUsersMultiChat().get(userName);
            connection.send(message);
        } catch (IOException e) {
            gui.errorMessage("Ошибка отправки сообщения сем пользователям!");
        }
    }

    private void addPortInConfigurationFile() {
        AppSettings appSettings = new AppSettings();
        try {
            portSever = appSettings.getPortFromSettingsFile();
        } catch (IOException e) {
            gui.errorMessage("Ошибка: Файл конфигурации отсутствует!");
        }
    }

    // Отправка команд с консоли
    static class SenderRunnable implements Runnable {

        private final Server socket;

        SenderRunnable(Server socket) {
            this.socket = socket;
        }


        @Override
        public void run() {
            try (BufferedReader buf = new BufferedReader(new InputStreamReader(System.in))) {
                while (isServerStart) {
                    String msg = buf.readLine();
                    String STOP = "/stop";
                    if (msg.equals(STOP)) {
                        socket.stopServer();
                        break;
                    }
                    gui.logMessage(msg);
                }
            } catch (IOException e) {
                gui.errorMessage("Ошибка при отправке сообщения из консоли.");
            }
        }
    }

    class ReceiverRunnable implements Runnable {
        private final Socket socket;
        private final Connection connection;

        private ReceiverRunnable(Socket socket, Connection connection) {
            this.socket = socket;
            this.connection = connection;
        }


        // метод, который реализует запрос сервера у клиента имени и добавление имени в мапу
        private String requestAndAddingUser(Connection connection) {
            while (true) {
                try {
                    // посылаем клиенту сообщение-запрос имени
                    connection.send(new Message(MessageType.REQUEST_NAME_USER));
                    Message reponseMessage = connection.receive();
                    String userName = reponseMessage.getTextMessage();
                    //получили ответ с именем и проверяем не занято ли это имя другим клиентом
                    if (reponseMessage.getMessageType() == MessageType.USER_NAME && userName != null && !userName.isEmpty()
                            && !model.getAllUsersMultiChat().containsKey(userName)) {
                        // Добавляем имя в мапу
                        model.addUser(userName, connection);
                        Set<String> listUsers = new HashSet<>();
                        for (Map.Entry<String, Connection> users : model.getAllUsersMultiChat().entrySet()) {
                            listUsers.add(users.getKey());
                        }
                        // отправляем клиенту множество имен уже подключившихся пользователей
                        connection.send(new Message(MessageType.NAME_ACCEPTED, listUsers));
                        // отправляем всем клиентам сообщение о новом пользователе
                        sendMessageAllUsers(new Message(MessageType.USER_ADDED, userName), connection);
                        return userName;
                    } else connection.send(new Message(MessageType.NAME_USER));
                } catch (Exception e) {
                    gui.errorMessage("Возникла ошибка при запросе и добавлении нового пользователя!");
                }
            }
        }

        // метод реализующий обмен сообщения между пользователями
        private void messagingBetweenUsers(Connection connection, String userName) {
            while (isServerStart) {
                try {
                    Message message = connection.receive();
                    String textMessage;
                    switch (message.getMessageType()) {
                        // Приняли сообщение от клиента, если тип сообщения TEXT_MESSAGE то пересылаем его всем пользователям
                        case TEXT_MESSAGE -> {
                            textMessage = String.format("%s: %s\n", userName, message.getTextMessage());
                            sendMessageAllUsers(new Message(MessageType.TEXT_MESSAGE, textMessage), connection);
                            gui.logMessage(textMessage);
                        }
                        // Еcли тип сообщения USER_TEXT_MESSAGE то отправляем конкретному пользователю сообщение
                        case USER_TEXT_MESSAGE -> {
                            textMessage = String.format("%s: %s\n", userName, message.getTextMessage());
                            sendMessageUser(new Message(MessageType.TEXT_MESSAGE, textMessage), message.getUserName());
                            gui.logMessage(textMessage);
                        }
                        // Если тип сообщения DISABLE_USER, то рассылаем всем пользователям, что данный пользователь покинул чат,
                        // удаляем его из мап, закрываем его connection
                        case DISABLE_USER -> {
                            sendMessageAllUsers(new Message(MessageType.REMOVED_USER, userName), connection);
                            gui.serviceMessage(String.format("Пользователь с удаленным доступом %s отключился.", socket.getRemoteSocketAddress()));
                            model.removeUser(userName);
                            connection.close();
                            return;
                        }
                    }
                } catch (Exception e) {
                    gui.errorMessage(String.format("Произошла ошибка при рассылке сообщения от пользователя %s, либо отключился!\n", userName));
                }
            }
        }

        @Override
        public void run() {
            gui.serviceMessage(String.format("Подключился новый пользователь с удаленным сокетом - %s.", socket.getRemoteSocketAddress()));
            try {
                // запрашиваем имя, регистрируем, запускаем цикл обмена сообщениями между пользователями
                String nameUser = requestAndAddingUser(connection);
                messagingBetweenUsers(connection, nameUser);
            } catch (Exception e) {
                gui.errorMessage("Произошла ошибка при рассылке сообщения от пользователя!");
            }
        }
    }
}
