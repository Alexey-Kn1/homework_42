import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

/*
Каждая строка считается отдельным сообщением. После подключения получим приветственное сообщение сервера,
потом (первым сообщением) отправим никнейм. Далее - общаемся.
 */

public class Main {
    private static final String SETTINGS_FILE_PATH = "client_settings.json";

    public static void main(String[] args) {
        Settings s;

        try {
            s = Settings.readForm(SETTINGS_FILE_PATH);
        } catch (IOException ex) {
            s = new Settings();

            try {
                Settings.writeTo(s, SETTINGS_FILE_PATH);

                System.out.printf("не найден файл настроек\nдефолтные настройки записаны в файл '%s'\n", SETTINGS_FILE_PATH);
            } catch (Exception ex1) {
                System.out.printf("не найден файл настроек\nне удалось записать дефолтные настройки в файл '%s'\n", SETTINGS_FILE_PATH);
            }

            return;
        }

        try (Logger l = new Logger(s.logFilePath, false)) {

            try (Socket connection = new Socket(s.serverIp, s.serverPort)) {

                PrintWriter output = new PrintWriter(connection.getOutputStream(), true);
                Scanner input = new Scanner(connection.getInputStream());

                l.log("connected to server %s:%d", s.serverIp, s.serverPort);

                System.out.println("connected");

                Thread displayingMessages = new Thread(
                        () -> printMessagesFromServer(input, l)
                );

                displayingMessages.start();

                try {
                    Scanner keyboard = new Scanner(System.in);

                    while (true) {
                        String typedMessage = keyboard.nextLine();

                        output.println(typedMessage);

                        l.log("message '%s' sent", typedMessage);

                        if (typedMessage.equals("/exit")) {
                            break;
                        }
                    }
                } finally {
                    displayingMessages.interrupt(); // Не работает, поток продолжает выполнение.

                    connection.close(); // Работает, поток останавливается.

                    displayingMessages.join();
                }

                System.out.println("disconnected");

                l.log("graceful shutdown");

            } catch (Exception ex) {
                System.out.printf("failed to connect to %s:%d\n", s.serverIp, s.serverPort);
            }

        } catch (IOException ex) {
            System.err.printf("failed to open log file '%s'", s.logFilePath);
        }
    }

    private static void printMessagesFromServer(Scanner input, Logger l) {
        while (true) {
            try {
                String lineFromServer = input.nextLine();

                System.out.println(lineFromServer);

                l.log("received from server: '%s'", lineFromServer);
            } catch (Exception ignored) {
                break;
            }
        }
    }
}
