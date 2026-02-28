package utils;

import java.util.Map;

public class ErrorHandling {
    public static Map<String, String> messageTable = Map.of(
            "Connection reset", "Your internet likely cut out mid-request, causing a crash",
            "", ""
    );
}
