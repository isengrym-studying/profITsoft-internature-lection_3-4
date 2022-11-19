package ua.klieshchunov.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class LawViolation {
    @JsonProperty("date_time")
    public String dateTime;

    @JsonProperty("first_name")
    public String firstName;

    @JsonProperty("last_name")
    public String lastName;

    @JsonProperty("type")
    public String type;

    @JsonProperty("fine_amount")
    public double fineAmount;

}
