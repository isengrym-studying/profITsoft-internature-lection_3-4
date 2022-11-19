import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ua.klieshchunov.ParsingUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ParsingUtilsTest {
    @Test
    public void testCombineNameAndSurname() throws IOException {
        String inputPath = "src/main/resources/persons";
        String outputPath = inputPath + "_modified";

        String fileTextBefore = """
                <persons>
                    <person name="Іван" surname="Котляревський" birthDate="09.09.1769" />
                    <person surname="Шевченко" name="Тарас" birthDate="09.03.1814" />
                    <person
                        birthData="27.08.1856"
                        name = "Іван"
                        surname = "Франко" />
                    <person name="Леся"
                            surname="Українка"
                            birthData="13.02.1871" />
                </persons>
                """;
        String expectedFileText = """
                <persons>
                    <person name="Іван Котляревський" birthDate="09.09.1769" />
                    <person name="Тарас Шевченко" birthDate="09.03.1814" />
                    <person
                        birthData="27.08.1856"
                        name = "Іван Франко"
                        />
                    <person name="Леся Українка"
                                                
                            birthData="13.02.1871" />
                </persons>
                """;

        try (BufferedWriter out = new BufferedWriter(
                new FileWriter(inputPath))) {
            out.write(fileTextBefore);
        }

        ParsingUtils.combineNameAndSurnameXml(inputPath, outputPath);

        try(BufferedReader br = new BufferedReader(new FileReader(outputPath))) {
            StringBuilder actualFileTextSB = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                actualFileTextSB.append(line).append("\n");
            }

            String actualFileText = actualFileTextSB.toString();
            Assertions.assertEquals(expectedFileText, actualFileText);
        }
    }

    @Test
    public void testGenerateLawViolationsStatistic() throws IOException {
        String inputPath = "src/main/resources/lawViolations";
        String outputPath = "src/main/resources/lawViolations/statistics.xml";

        ParsingUtils.generateLawViolationStatistics(inputPath, outputPath);

        String actual = Files.readString(Paths.get(outputPath));
        String expected = """
                <lawViolations>
                  <items>
                    <ALCOHOL_INTOXICATION>17000.0</ALCOHOL_INTOXICATION>
                    <SPEEDING>4860.0</SPEEDING>
                    <NO_DRIVER_LICENSE_AT_ALL>3700.0</NO_DRIVER_LICENSE_AT_ALL>
                    <NOT_WEARING_SEATBELT>2040.0</NOT_WEARING_SEATBELT>
                    <PARKING>1260.0</PARKING>
                    <NO_LICENSE_PLATE>850.0</NO_LICENSE_PLATE>
                  </items>
                </lawViolations>         
                """;
        // For some reason 'expected' and 'actual' have got different line breakers (LF and CRLF).
        // So for the test to be successful, I need to make a little change
        expected = expected.replaceAll("\n", "\r\n");

        Assertions.assertEquals(expected, actual);

    }
}
