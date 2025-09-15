import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.ClosedByInterruptException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/*
Сервер работает следующим образом:

Создаются два потока: для принятия новых подключений и для отправки сообщений.
Также для каждого нового клиента создаётся по потоку для чтения сообщений.
Каждое отправляемое сообщение попадает в очередь, которую разбирает поток для их отправки.
После подключения сокет попадает в коллекцию, которая перебирается при рассылке сообщений,
после отключения он от туда удаляется.
*/
public class ChatServer {

    // Класс, необходимый для того, чтобы, разбирая очередь отправляемых сообщений,
    // не отослать сообщение его же отправителю.
    private static class MessageWithSender {
        public String message;
        public ClientConnection sender;

        public MessageWithSender(String message, ClientConnection sender) {
            this.message = message;
            this.sender = sender;
        }
    }

    private final Set<ClientConnection> clients;
    private final LinkedBlockingQueue<MessageWithSender> messagesToSend;
    private Thread messagesBroadcasting;
    private Thread connectionsAccepting;
    private ExecutorService connectionsServing;
    private final ServerSocket listener;
    private final Logger logger;
    private boolean isRunning;
    private final String helloMessage;

    public ChatServer(ServerSocket listener, String helloMessage, Logger logger) {
        this.logger = logger;
        this.helloMessage = helloMessage;
        this.listener = listener;

        clients = new HashSet<>();
        messagesToSend = new LinkedBlockingQueue<>();
    }

    // Отправляет сообщения всем подключённым клиентам.
    public void sendBroadcast(String message) {
        sendBroadcast(message, null);
    }

    // Отправляет сообщения всем подключённым клиентам, кроме заданного (можно указать null).
    private void sendBroadcast(String message, ClientConnection exceptOne) {
        try {
            messagesToSend.put(new MessageWithSender(message, exceptOne));
        } catch (InterruptedException e) {
            logger.log("got exception on putting a message '%s' in output queue: %s", message, e.getMessage());
        }
    }

    public boolean isRunning() {
        return isRunning;
    }

    // Запускает обслуживание входящих соединений.
    public void start() {
        isRunning = true;

        messagesToSend.clear();

        connectionsServing = Executors.newCachedThreadPool();

        messagesBroadcasting = new Thread(this::broadcastMessages);
        connectionsAccepting = new Thread(this::acceptConnections);

        messagesBroadcasting.start();
        connectionsAccepting.start();

        logger.log("server started");
    }

    // Останавливает обслуживание входящих соединений, не дожидаясь завершения потоков.
    // Также закрывает сокет, переданный в конструктор (по-другому не удалось прервать вызов accept()).
    public void stop() {
        connectionsAccepting.interrupt();
        messagesBroadcasting.interrupt();

        try {
            listener.close();
        } catch (IOException e) {
            logger.log("exception on listener closing: %s", e.getMessage());
        }

        sendBroadcast("");

        logger.log("server began to stop (not accepting new connections)");
    }

    // Запускает сервер и дожидается завершения выполнения всех его потоков.
    // Не возвращает управление до вызова stop().
    public void run() throws InterruptedException {
        start();
        waitForStop();
    }

    // Ожидает окончания работы всех потоков сервера.
    public void waitForStop() throws InterruptedException {
        connectionsAccepting.join();
        messagesBroadcasting.join();

        connectionsServing.close();

        isRunning = false;

        logger.log("server stopped");
    }

    // Разбирает очередь отправляемых сообщений, рассылает сообщения всем подключённым клиентам.
    private void broadcastMessages() {
        MessageWithSender yetAnotherMessage;

        while (true) {
            try {
                yetAnotherMessage = messagesToSend.take();
            } catch (InterruptedException e) {
                break;
            }

            if (Thread.interrupted()) {
                break;
            }

            synchronized (clients) {
                for (ClientConnection client : clients) {
                    if (client.equals(yetAnotherMessage.sender)) {
                        continue;
                    }

                    client.send(yetAnotherMessage.message);
                }
            }
        }
    }

    // Принимает новые соединения, запускает потоки для обслуживания новых клиентов.
    private void acceptConnections() {
        while (true) {
            try {
                Socket clientSocket = listener.accept();

                if (Thread.interrupted()) {
                    break;
                }

                logger.log(
                        "connected client with address %s",
                        clientSocket.getInetAddress().toString()
                );

                ClientConnection connection = new ClientConnection(clientSocket);

                connectionsServing.execute(
                        () -> serveClient(connection)
                );
            } catch (ClosedByInterruptException e) {
                break;
            } catch (IOException e) {
                logger.log("connection accepting exception: %s", e.getMessage());

                break;
            }
        }
    }

    // Поток обслуживания конкретного клиента. Сначала выводит приветственное сообщение,
    // потом спрашивает никнейм и циклически передаёт его сообщения остальным клиентам,
    // пока не получит сообщение "/exit".
    private void serveClient(ClientConnection client) {
        boolean addedToSet = false;

        try (client) {
            client.send(helloMessage);

            String nickname = client.receive();

            synchronized (clients) {
                clients.add(client);

                addedToSet = true;
            }

            logger.log(
                    "user with nickname '%s' joined chat",
                    nickname
            );

            sendBroadcast(
                    String.format("%s joined chat", nickname)
            );

            while (true) {
                String message = client.receive();

                if (message.equals("/exit")) {
                    break;
                }

                logger.log(
                        "user with nickname '%s' sent a message: %s",
                        nickname,
                        message
                );

                sendBroadcast(
                        String.format("%s: %s", nickname, message),
                        client
                );
            }

            logger.log(
                    "user with nickname '%s' left chat",
                    nickname
            );

            sendBroadcast(
                    String.format("%s left chat", nickname)
            );
        } catch (IOException e) {
            logger.log("client talking exception: %s", e.getMessage());
        } finally {
            if (addedToSet) {
                synchronized (clients) {
                    if (!clients.remove(client)) {
                        logger.log("failed to remove connection from set");
                    }
                }
            }
        }
    }

}
