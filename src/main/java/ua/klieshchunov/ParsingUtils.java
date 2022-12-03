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
import java.util.stream.Stream;

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
        Map<String,Double> result = new HashMap<>();

        File[] lawViolationFiles = getArrayOfFiles(directoryPath);

        for (File file : lawViolationFiles) {
            List<LawViolation> lawViolationList = readObjectFromFileToList(mapper, file);
            result = appendStats(result, lawViolationList);
        }

        result = sortByFineAmount(result);
        writeToXml(xmlMapper, statisticsFilePath, result);
    }


    private static File[] getArrayOfFiles(String directoryPath) {
        File violationsDirectory = new File(directoryPath);
        return violationsDirectory.listFiles(path -> {
            Pattern pattern = Pattern.compile("^violations_\\d{4}$");
            return pattern.matcher(path.getName()).matches();
        });
    }

    private static List<LawViolation> readObjectFromFileToList(ObjectMapper mapper, File lawViolationFile) {
        List<LawViolation> lawViolationList = new ArrayList<>();

        try (InputStream is = new FileInputStream(lawViolationFile.getPath())) {
            List<LawViolation> partialLawViolationList = mapper.readValue(is.readAllBytes(), new TypeReference<>() {});
            lawViolationList.addAll(partialLawViolationList);
        } catch (IOException e) {
            throw new RuntimeException("Couldn't convert json to the LawViolation objects list", e);
        }

        return lawViolationList;
    }

    private static Map<String, Double> appendStats(Map<String,Double> map, List<LawViolation> lawViolationList) {
        Map<String, Double> newData = lawViolationList.stream()
                .collect(Collectors.groupingBy(
                        LawViolation::getType,
                        Collectors.summingDouble(LawViolation::getFineAmount))
                );

        return Stream
                .concat(map.entrySet().stream(), newData.entrySet().stream())
                .collect(Collectors.groupingBy(Map.Entry::getKey,
                        Collectors.summingDouble(Map.Entry::getValue)));
    }

    private static Map<String, Double> sortByFineAmount(Map<String,Double> map) {
        Map<String, Double> copiedMap = new HashMap<>(map);
        return copiedMap.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (oldVal, newVal) -> oldVal, LinkedHashMap::new));
    }

    private static void writeToXml(XmlMapper xmlMapper, String statisticsFilePath, Map<String, Double> resultingMap) {
        try {
            File statisticsFile = new File(statisticsFilePath);
            statisticsFile.createNewFile();
            xmlMapper.writerWithDefaultPrettyPrinter().writeValue(statisticsFile, new LawViolationsWrapper(resultingMap));
        } catch (IOException e) {
            throw new RuntimeException("Error writing results to the file", e);
        }
    }
}
