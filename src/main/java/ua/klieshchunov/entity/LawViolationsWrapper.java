package ua.klieshchunov.entity;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@JacksonXmlRootElement(localName = "lawViolations")
public class LawViolationsWrapper {
    public LawViolationsWrapper(Map<String, Double> items) {
        this.items = items;
    }

    Map<String, Double> items;
}
