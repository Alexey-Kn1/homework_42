import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;

public class SettingsTest {
    final String SETTINGS_FILE_PATH = "./src/test/resources/test_settings.json";

    @Test
    public void check() throws Exception {
        Settings written = new Settings();

        written.serverPort = 999;
        written.serverIp = "255.255.255.255";
        written.logFilePath = "example/file/path";

        Settings.writeTo(written, SETTINGS_FILE_PATH);

        Settings read = Settings.readForm(SETTINGS_FILE_PATH);

        Assertions.assertEquals(written.serverPort, read.serverPort);
        Assertions.assertEquals(written.logFilePath, read.logFilePath);
        Assertions.assertEquals(written.serverIp, read.serverIp);

        File f = new File(SETTINGS_FILE_PATH);

        Assertions.assertTrue(f.delete());
    }
}
