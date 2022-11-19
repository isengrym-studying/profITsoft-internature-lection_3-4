package ua.klieshchunov;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import ua.klieshchunov.entity.LawViolation;
import ua.klieshchunov.entity.LawViolationsWrapper;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ParsingUtils {
    public static void combineNameAndSurnameXml(String inputPath, String outputPath) {
        Pattern personOpeningTagPattern = Pattern.compile("^.*<person.*$");
        Pattern personClosingTagPattern = Pattern.compile("^.*/>(?!>).*$");

        try (BufferedReader br = new BufferedReader(new FileReader(inputPath));
             BufferedWriter bw = new BufferedWriter(new FileWriter(outputPath))) {
            String line;

            while ((line = br.readLine()) != null) {
                Matcher openingMatcher = personOpeningTagPattern.matcher(line);

                if (openingMatcher.find()) {
                    StringBuilder person = new StringBuilder();
                    person.append(line).append("\n");

                    while(!personClosingTagPattern.matcher(line).find()) {
                        line = br.readLine();
                        person.append(line).append("\n");
                    }
                    String personStr = person.toString();

                    Matcher nameMatcher =  Pattern.compile(".*\\s(name\\s?=\\s?\"(?<name>.*?)\")").matcher(personStr);
                    Matcher surnameMatcher = Pattern.compile(".*\\s(?<surnameKeyValue>surname\\s?=\\s?\"(?<surname>.*?)\"\\s?)").matcher(personStr);

                    if (!nameMatcher.find() || !surnameMatcher.find()) {
                        throw new IllegalStateException("Match not found, check the correctness of your xml file");
                    }

                    String surnameKeyValue = surnameMatcher.group("surnameKeyValue");
                    String name = nameMatcher.group("name");
                    String surname = surnameMatcher.group("surname");

                    // Replacing construction 'name="name" ' with 'name="name surname"'
                    personStr = personStr.replace(name, name + ' ' + surname);

                    // Replacement for strings that contain only surname="surname".
                     personStr = personStr.replaceAll(".*\\s(surname\\s?=\\s?\"(.*?)\"\\s?\\R)", "\r");

                    // Replacing construction 'surname="surname" ' with empty
                    personStr = personStr.replace(surnameKeyValue, "");
                    bw.write(personStr);
                }
                else {
                    bw.write(line + "\n");
                }
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Couldn't find file", e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static void generateLawViolationStatistics(String directoryPath, String statisticsFilePath) {
        ObjectMapper mapper = new ObjectMapper();
        XmlMapper xmlMapper = new XmlMapper();

        File[] lawViolationFiles = getArrayOfFiles(directoryPath);
        List<LawViolation> lawViolationList = readObjectsFromFileToList(mapper, lawViolationFiles);
        Map<String, Double> resultingMap = collectToSortedMap(lawViolationList);
        writeToXml(xmlMapper, statisticsFilePath, resultingMap);
    }

    private static Map<String, Double> collectToSortedMap(List<LawViolation> lawViolationList) {
        return lawViolationList.stream()
                .collect(Collectors.groupingBy(
                        LawViolation::getType,
                        Collectors.summingDouble(LawViolation::getFineAmount))
                )
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue
                        , (oldVal, newVal) -> oldVal, LinkedHashMap::new));
    }

    private static File[] getArrayOfFiles(String directoryPath) {
        File violationsDirectory = new File(directoryPath);
        return violationsDirectory.listFiles(path -> {
            Pattern pattern = Pattern.compile("^violations_\\d{4}$");
            return pattern.matcher(path.getName()).matches();
        });
    }

    private static List<LawViolation> readObjectsFromFileToList(ObjectMapper mapper, File[] lawViolationFiles) {
        List<LawViolation> lawViolationList = new ArrayList<>();

        for (File lawViolationFile : lawViolationFiles) {
            try (InputStream is = new FileInputStream(lawViolationFile.getPath())) {
                List<LawViolation> partialLawViolationList = mapper.readValue(is.readAllBytes(), new TypeReference<>() {
                });
                lawViolationList.addAll(partialLawViolationList);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return lawViolationList;
    }

    private static void writeToXml(XmlMapper xmlMapper, String statisticsFilePath, Map<String, Double> resultingMap) {
        try {
            File statisticsFile = new File(statisticsFilePath);
            statisticsFile.createNewFile();
            xmlMapper.writerWithDefaultPrettyPrinter().writeValue(statisticsFile, new LawViolationsWrapper(resultingMap));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
