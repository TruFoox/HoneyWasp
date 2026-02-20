package utils;

import java.net.URLEncoder;
import java.net.http.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

// HTTPSend - Some of these I made, but the highly complicated ones I didn't
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
// String HTTPSend.postForm  ; Send HTTP POST request with form fields
// Inputs : URL to send to, Map<String,String> containing form fields
public class HTTPSend {

    public static ThreadLocal<Long> HTTPCode = ThreadLocal.withInitial(() -> 0L);
    // Threadlocal = "Each thread has its own version"
    // ThreadLocal.withInitial(() -> 0L) = "Start with value

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
        HTTPCode.set((long) response.statusCode());

        return String.valueOf(response.body());
    }


    public static String get(String url) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                        + "AppleWebKit/537.36 (KHTML, like Gecko) "
                        + "Chrome/118.0.0.0 Safari/537.36")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Returns response & status code
        HTTPCode.set((long) response.statusCode()); // Set HTTP code

        return String.valueOf(response.body());  // Return response body to caller
    }

    public static String get(String url, Map<String, String> headers) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                        + "AppleWebKit/537.36 (KHTML, like Gecko) "
                        + "Chrome/118.0.0.0 Safari/537.36"
                );

        // Add headers from the map
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                builder.header(entry.getKey(), entry.getValue());
            }
        }

        HttpRequest request = builder.build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        HTTPCode.set((long) response.statusCode());
        return response.body();
    }

    public static String postFile(String url, Path filePath) throws Exception { // DESIGNED FOR USE WITH 0x0 ONLY
        HttpClient client = HttpClient.newHttpClient();

        String boundary = UUID.randomUUID().toString();
        byte[] fileBytes = Files.readAllBytes(filePath);

        String CRLF = "\r\n";
        String fileName = filePath.getFileName().toString();
        StringBuilder sb = new StringBuilder();

        // expires field
        sb.append("--").append(boundary).append(CRLF);
        sb.append("Content-Disposition: form-data; name=\"expires\"").append(CRLF);
        sb.append(CRLF);
        sb.append("1").append(CRLF); // 1 hour

        // file field
        sb.append("--").append(boundary).append(CRLF);
        sb.append("Content-Disposition: form-data; name=\"file\"; filename=\"")
                .append(fileName).append("\"").append(CRLF);
        sb.append("Content-Type: application/octet-stream").append(CRLF).append(CRLF);

        byte[] preamble = sb.toString().getBytes();
        byte[] ending = (CRLF + "--" + boundary + "--" + CRLF).getBytes();

        byte[] body = new byte[preamble.length + fileBytes.length + ending.length];
        System.arraycopy(preamble, 0, body, 0, preamble.length);
        System.arraycopy(fileBytes, 0, body, preamble.length, fileBytes.length);
        System.arraycopy(ending, 0, body, preamble.length + fileBytes.length, ending.length);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "HoneyWasp/3.0")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        HTTPCode.set((long) response.statusCode());

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
        HTTPCode.set((long) response.statusCode()); // Set HTTP code
        return response.body();
    }

}
