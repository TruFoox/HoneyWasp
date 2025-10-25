package utils;

import org.json.JSONObject;

// StringToJson
//
// StringToJson.getData  ; Extract a specific value from a JSON-formatted string
// Inputs : JSON string to parse, key name to retrieve value from
//
// StringToJson.getJSON  ; Convert a JSON-formatted string into a JSONObject
// Inputs : JSON string to convert

public class StringToJson {
    public static String getData(String data, String keyName) {
        JSONObject obj = new JSONObject(data);

        Object value = obj.get(keyName);

        return String.valueOf(value);

    }
    public static JSONObject getJSON(String data) {
        return new JSONObject(data);
    }
}