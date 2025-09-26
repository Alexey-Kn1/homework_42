import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoggingFormatCheck {
    private final String LOG_FILE_PATH = "./src/test/resources/log.txt";

    @Test
    public void checkLoggingFormat() throws Exception {
        try (Logger l = new Logger(LOG_FILE_PATH, false)) {
            l.log("line %d", 0);
            l.log("line %d", 1);
            l.log("line %d", 2);
            l.log("line %d", 3);
        }

        try (Reader r = new FileReader(LOG_FILE_PATH)) {
            Scanner sc = new Scanner(r);

            int lineIndex = 0;

            while (sc.hasNext()) {
                String lineContent = sc.nextLine();

                String regex = String.format("\\[\\d\\d\\.\\d\\d.\\d\\d\\d\\d \\d\\d\\:\\d\\d\\:\\d\\d\\] line %d", lineIndex);
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(lineContent);

                Assertions.assertTrue(matcher.find());

                lineIndex++;
            }

            Assertions.assertEquals(4, lineIndex);
        } finally {
            File f = new File(LOG_FILE_PATH);

            Assertions.assertTrue(f.delete());
        }
    }
}
