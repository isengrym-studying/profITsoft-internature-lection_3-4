import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ua.klieshchunov.XmlParser;

import java.io.*;

public class XmlParserTest {
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

        XmlParser.combineNameAndSurname(inputPath, outputPath);

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
}
