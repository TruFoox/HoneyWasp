package utils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

// DateTime
//
// DateTime.time  ; Get current local time in HH:mm:ss format
// DateTime.fullTimestamp  ; Get full system timestamp as a formatted string

public class DateTime {
    public static String time() {
        Date date = new Date();
        LocalDateTime ldt = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();

        int hour = ldt.getHour();
        int minute = ldt.getMinute();
        int second = ldt.getSecond();
        return String.format("%02d:%02d:%02d", hour, minute, second);
    }
    public static String fullTimestamp() {
        return String.format(String.valueOf(new Date()));
    }
}
