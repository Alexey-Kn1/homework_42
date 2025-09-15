import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class Settings {
    public static final String DEFAULT_SERVER_IP = "127.0.0.1";
    public static final int DEFAULT_SERVER_PORT = 4444;
    public static final String DEFAULT_LOG_FILE_PATH = "log.txt";

    public String serverIp;
    public int serverPort;
    public String logFilePath;

    public Settings() {
        serverIp = DEFAULT_SERVER_IP;
        serverPort = DEFAULT_SERVER_PORT;
        logFilePath = DEFAULT_LOG_FILE_PATH;
    }

    public static Settings readForm(String path) throws IOException {
        Gson gson = new Gson();
        String fileContent;

        try (FileReader input = new FileReader(path)) {
            StringBuilder builder = new StringBuilder();

            for (int ch = input.read(); ch != -1; ch = input.read()) {
                builder.append((char) ch);
            }

            fileContent = builder.toString();
        }

        return gson.fromJson(fileContent, Settings.class);
    }

    public static void writeTo(Settings s, String path) throws IOException {
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();

        String fileContent = gson.toJson(s);

        try (FileWriter output = new FileWriter(path)) {
            output.write(fileContent);
        }
    }
}
