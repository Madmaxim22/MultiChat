import org.apache.commons.validator.routines.InetAddressValidator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class Client {
    private static Connection connection;
    private static ModelGuiClient model;
    private static ViewGuiClient gui;
    private static volatile boolean isConnect = false; //флаг отображающий состояние подключения клиента серверу
    private static String addressServer;
    private static int portServer;
    private Socket socket;

    //точка входа в клиентское приложение
    public static void main(String[] args) throws InterruptedException {
        Client client = new Client();
        model = new ModelGuiClient();
        gui = new ViewGuiClient();
        client.addAdrAndPortInConfigurationFile();
        client.connectToServer();
        if (isConnect) {
            client.nameUserRegistration();
            Thread sender = new Thread(new SenderRunnable(client));
            Thread receiver = new Thread(new ReceiverRunnable(client));
            sender.start();
            receiver.start();
            sender.join();
            receiver.join();
        }
    }


    protected void addAdrAndPortInConfigurationFile() {
        AppSettings appSettings = new AppSettings();
        try {
            appSettings.getSettingsFile();
            String address = appSettings.getAddressServer();
            if (addressVerification(address)) {
                addressServer = address;
                gui.serviceMessage("Сервисное сообщение: Адрес принят!");
            } else {
                gui.serviceMessage("Сервисное сообщение: Неверный адрес!");
            }
            portServer = appSettings.getPortServer();
        } catch (IOException e) {
            gui.errorMessage("Ошибка: Файл конфигурации отсутствует!");
        }
    }

    // метод подключения клиента к серверу
    protected void connectToServer() {
        //проверка на подключение клиента к серверу
        if (!isConnect) {
            try {
                //создаем сокет и объект connection
                socket = createSocket();
                connection = createConnection(socket);
                isConnect = true;
                gui.serviceMessage("Сервисное сообщение: Вы подключились к серверу.");
            } catch (Exception e) {
                gui.errorMessage("Произошла ошибка! Возможно сервер не запущен!");
            }
        } else gui.errorMessage("Вы уже подключены!");
    }

    protected void stopClient() {
        try {
            // если клиентский сокет не имеет ссылки или не запущен
            if (socket != null && !socket.isClosed()) {
                isConnect = false;
                socket.close();
                model.getUsers().clear();
                gui.serviceMessage("Клиент остановлен.");
            } else gui.serviceMessage("Клиент не запущен останавливать нечего!");
        } catch (IOException e) {
            gui.errorMessage("Остановить клиента не удалось");
        }
    }

    protected Socket createSocket() throws IOException {
        return new Socket(addressServer, portServer);
    }

    protected Connection createConnection(Socket socket) throws IOException {
        return new Connection(socket);
    }

    protected boolean addressVerification(String address) {
        InetAddressValidator validator = new InetAddressValidator();
        return validator.isValidInet4Address(address);
    }

    // метод реализующий регистрацию имени пользователя со стороны пользовательского приложения
    protected void nameUserRegistration() {
        while (true) {
            try {
                Message message = connection.receive();
                switch (message.getMessageType()) {
                    // приняли от сервера сообщение, если это запрос имени, то вызываем окна ввода имени, отправляем на сервер имя
                    case REQUEST_NAME_USER -> {
                        gui.serviceMessage("Введите имя пользователя: ");
                        String nameUser = gui.messagesFromTheConsole();
                        connection.send(new Message(MessageType.USER_NAME, nameUser));
                    }
                    // если имя принято получаем от сервера множество всех подключившихся пользователей, выходим из цикла
                    case NAME_USER -> {
                        gui.serviceMessage("Сервисное сообщение: Данное имя уже используется, введите другое");
                    }
                    case NAME_ACCEPTED -> {
                        gui.serviceMessage("Сервисное сообщение: Ваше имя принято!");
                        model.setUsers(message.getListUser());
                        gui.refreshListUsers(model.getUsers());
                        return;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                gui.errorMessage("Произошла ошибка при регистрации имени! Попробуйте переподключиться.");
            }
        }
    }

    // Отправка сообщений с консоли
    static class SenderRunnable implements Runnable {

        Client client;

        public SenderRunnable(Client client) {
            this.client = client;
        }

        @Override
        public void run() {
            try (BufferedReader buf = new BufferedReader(new InputStreamReader(System.in))) {
                while (isConnect) {
                    try {
                        if (buf.ready()) {
                            String msg = buf.readLine();
                            String EXIT = "/exit";
                            String[] ms = msg.split(" ");
                            if (msg.equals(EXIT)) {
                                connection.send(new Message(MessageType.DISABLE_USER));
                                client.stopClient();
                                return;
                            } else if (model.getUsers().contains(ms[0])) {
                                connection.send(new Message(MessageType.USER_TEXT_MESSAGE, ms[0], msg));
                            } else {
                                connection.send(new Message(MessageType.TEXT_MESSAGE, msg));
                            }
                        }
                    } catch (IOException e) {
                        gui.errorMessage("Ошибка при отправке сообщения из консоли.");
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    // Получение сообщений с сервера от других пользователей
    static class ReceiverRunnable implements Runnable {

        Client client;

        public ReceiverRunnable(Client client) {
            this.client = client;
        }

        @Override
        public void run() {
            while (isConnect) {
                try {
                    Message message = connection.receive();
                    switch (message.getMessageType()) {
                        //если тип TEXT_MESSAGE, то добавляем текст сообщения в окно переписки
                        case TEXT_MESSAGE -> {
                            gui.usersMessage(message.getTextMessage());
                        }
                        //если сообщение тип USER_ADDED добавляем сообщение в окно переписки о новом пользователе
                        case USER_ADDED -> {
                            model.addUser(message.getTextMessage());
                            gui.serviceMessage(String.format("Сервисное сообщение: пользователь %s присоединился к чату."
                                    , message.getTextMessage()));
                        }
                        //аналогично для отключения других пользователей
                        case REMOVED_USER -> {
                            model.removeUser(message.getTextMessage());
                            gui.serviceMessage(String.format("Сервисное сообщение: пользователь %s покинул чат."
                                    , message.getTextMessage()));
                        }
                        // если
                        case DISABLE_SERVER -> {
                            gui.serviceMessage("Сервисное сообщение: сервер отключен!");
                            client.stopClient();
                        }
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
