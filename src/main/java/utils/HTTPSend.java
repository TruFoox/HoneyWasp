package utils;

import java.net.URLEncoder;
import java.net.http.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

// HTTPSend
//
// String HTTPSend.post  ; Send HTTP POST request
// Inputs : URL to send to, JSON data to include in body
//
// String HTTPSend.get  ; Send HTTP GET request
// Inputs : URL to send to
//
// String HTTPSend.postFile  ; Send HTTP POST request & send file
// Inputs : URL to send to, path of file
//
// String HTTPSend.postForm  ; Send HTTP POST request with form-encoded fields
// Inputs : URL to send to, Map<String,String> containing form fields
public class HTTPSend {

    public static long HTTPCode; // If I add multithreading, ensure cant use at same time

    public static String post(String URL, String jsonData) throws Exception {
        HttpClient client = HttpClient.newHttpClient(); // Creates new http instance to send request

        // Build POST
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(URL))
                .header("Content-Type", "application/json")

                // UserAgent spoofing
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                        + "AppleWebKit/537.36 (KHTML, like Gecko) "
                        + "Chrome/118.0.0.0 Safari/537.36")

                .POST(HttpRequest.BodyPublishers.ofString(jsonData))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Returns response & status code
        HTTPCode = response.statusCode();
        return String.valueOf(response.body());
    }
    public static String get(String url) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Returns response & status code
        HTTPCode = response.statusCode();
        return String.valueOf(response.body());  // Return response body to caller
    }
    public static String postFile(String url, Path filePath) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        // Generate boundary for multipart/form-data
        String boundary = UUID.randomUUID().toString();

        // Read file bytes
        byte[] fileBytes = Files.readAllBytes(filePath);

        // Build multipart body manually
        String CRLF = "\r\n";
        String fileName = filePath.getFileName().toString();
        StringBuilder sb = new StringBuilder();

        sb.append("--").append(boundary).append(CRLF);
        sb.append("Content-Disposition: form-data; name=\"file\"; filename=\"")
                .append(fileName).append("\"").append(CRLF);
        sb.append("Content-Type: application/octet-stream").append(CRLF).append(CRLF);

        byte[] preamble = sb.toString().getBytes();

        byte[] ending = (CRLF + "--" + boundary + "--" + CRLF).getBytes();

        // Combine preamble + file bytes + ending
        byte[] body = new byte[preamble.length + fileBytes.length + ending.length];
        System.arraycopy(preamble, 0, body, 0, preamble.length);
        System.arraycopy(fileBytes, 0, body, preamble.length, fileBytes.length);
        System.arraycopy(ending, 0, body, preamble.length + fileBytes.length, ending.length);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                        + "AppleWebKit/537.36 (KHTML, like Gecko) "
                        + "Chrome/118.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        HTTPCode = response.statusCode();
        return response.body();
    }
    public static String postForm(String url, Map<String, String> data) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        StringBuilder form = new StringBuilder();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            if (form.length() > 0) form.append("&");
            form.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            form.append("=");
            form.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        HTTPCode = response.statusCode();
        return response.body();
    }

}
