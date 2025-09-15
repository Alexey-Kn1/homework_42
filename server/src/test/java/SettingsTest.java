import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;

public class SettingsTest {
    @Test
    public void check() throws Exception {
        final String SETTINGS_FILE_PATH = "test_settings.json";

        Settings written = new Settings();

        written.port = 999;
        written.logFilePath = "example/file/path";

        Settings.writeTo(written, SETTINGS_FILE_PATH);

        Settings read = Settings.readForm(SETTINGS_FILE_PATH);

        Assertions.assertEquals(written.port, read.port);

        Assertions.assertEquals(written.logFilePath, read.logFilePath);

        File f = new File(SETTINGS_FILE_PATH);

        Assertions.assertTrue(f.delete());
    }
}
