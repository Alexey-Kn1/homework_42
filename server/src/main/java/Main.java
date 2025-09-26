import java.io.IOException;
import java.net.ServerSocket;
import java.util.Scanner;

public class Main {
    private static final String SETTINGS_FILE_PATH = "server_settings.json";

    public static void main(String[] args) throws InterruptedException {
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

        try (Logger logger = new Logger(s.logFilePath, true)) {
            ChatServer server = new ChatServer(
                    new ServerSocket(s.port),
                    "Введите никнейм первым сообщением.",
                    logger
            );
            logger.log("starting server...");
            server.start();
            Scanner sc = new Scanner(System.in);
            System.out.println("для остановки сервера нажмите 'Enter'");
            sc.nextLine();
            server.stop();
            server.waitForStop();
            logger.log("graceful shutdown");
        } catch (IOException ex) {
            System.err.printf("failed to open log file '%s'", s.logFilePath);
        }
    }
}
