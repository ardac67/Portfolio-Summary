package org.example;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

public class DateTimeBuilder {
    private DateTimeFormatterBuilder dateTimeFormatterBuilder;
    private DateTimeFormatter dateTimeFormatter;
    public DateTimeBuilder(){
        dateTimeFormatterBuilder = new DateTimeFormatterBuilder();
        dateTimeFormatterBuilder = new DateTimeFormatterBuilder()
                .append(DateTimeFormatter.ofPattern("[yyyy-MM-dd]"));
        dateTimeFormatter = dateTimeFormatterBuilder.toFormatter();
    }
    public LocalDate returnFormattedDate(String date){
        LocalDate dateStart = LocalDate.parse(date, dateTimeFormatter);
        return dateStart;
    }
    public String returnFormattedWithTime(String date){
        LocalDate dateTime = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        String outputDateString = dateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return outputDateString;
    }
}
