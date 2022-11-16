package ua.klieshchunov;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XmlParser {
    public static void combineNameAndSurname(String inputPath, String outputPath) {
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
                    
                    Matcher nameMatcher =  Pattern.compile(".*\\s(?<keyValue>name\\s?=\\s?\"(?<name>.*?)\")").matcher(personStr);
                    Matcher surnameMatcher = Pattern.compile(".*\\s(?<keyValue>surname\\s?=\\s?\"(?<surname>.*?)\"\\s?)").matcher(personStr);

                    if (!nameMatcher.find() || !surnameMatcher.find()) {
                        throw new IllegalStateException("Match not found, check the correctness of your xml file");
                    }

                    String nameKeyValue = nameMatcher.group("keyValue");
                    String surnameKeyValue = surnameMatcher.group("keyValue");
                    String name = nameMatcher.group("name");
                    String surname = surnameMatcher.group("surname");

                    //Replacing construction 'name="name" ' with 'name="name surname"'
                    personStr = personStr.replace(name, name + ' ' + surname);
                    //Replacement for strings which contain only surname="surname". Made just to match the example.
                    personStr = personStr.replaceAll(".*\\s(surname\\s?=\\s?\"(.*?)\"\\s?\\R)", "\r");
                    //Replacing construction 'surname="surname" ' with empty
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
}
