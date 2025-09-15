import java.io.Closeable;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger implements Closeable {
    private final PrintWriter output;
    private boolean isOpen;
    private final boolean printToStdout;

    public Logger(String outputFilePath, boolean printToStdout) throws IOException {
        isOpen = true;

        this.printToStdout = printToStdout;

        output = new PrintWriter(new FileWriter(outputFilePath, true), true);
    }

    public void log(String format, Object... args) {
        String message = String.format(
                "[%s] %s",
                LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")
                ),
                String.format(format, args)
        );

        synchronized (this) {
            output.println(message);
        }

        if (printToStdout) {
            System.out.println(message);
        }
    }

    @Override
    public synchronized void close() {
        if (isOpen) {
            output.close();

            isOpen = false;
        }
    }
}