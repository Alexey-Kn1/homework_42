import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

// Класс для общения с клиентом. Протокол этого общения очень прост.
// Каждая строка - отдельное сообщение (в рамках консольного мессенджера нет смысла
// писать обмен многострочными сообщениями).
public class ClientConnection implements Closeable {
    private final Socket socket;
    private final PrintWriter writer;
    private final Scanner scanner;

    public ClientConnection(Socket socket) throws IOException {
        this.socket = socket;

        writer = new PrintWriter(socket.getOutputStream(), true);
        scanner = new Scanner(socket.getInputStream());
    }

    public void send(String message) {
        writer.println(message);
    }

    public String receive() {
        return scanner.nextLine();
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
