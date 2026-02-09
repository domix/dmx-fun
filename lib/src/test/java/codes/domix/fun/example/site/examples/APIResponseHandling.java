package codes.domix.fun.example.site.examples;

import codes.domix.fun.Try;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class APIResponseHandling {
    public record UserData(String name) {
    }

    private final HttpClient client;

    public APIResponseHandling() {
        this.client = HttpClient.newHttpClient();
    }

    public Try<UserData> fetchUser(String userId) {
        return Try.of(() -> {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.example.com/users/" + userId))
                    .GET()
                    .build();

                return client.send(request, HttpResponse.BodyHandlers.ofString());
            })
            .flatMap(this::parseResponse)
            .map(this::parseUserData);
    }

    private Try<String> parseResponse(HttpResponse<String> response) {
        if (response.statusCode() == 200) {
            return Try.success(response.body());
        } else {
            return Try.failure(
                new RuntimeException("HTTP " + response.statusCode())
            );
        }
    }

    private UserData parseUserData(String json) {
        // Parse JSON to UserData
        return new UserData(json);
    }

    void main() {
        // Usage
        APIResponseHandling api = new APIResponseHandling();

        Try<UserData> result = api.fetchUser("123");

        String message = result
            .map(user -> "User: " + user.name())
            .recover(throwable -> "Error: " + throwable.getMessage())
            .get();
    }
}
