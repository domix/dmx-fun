package codes.domix.fun.example.site.examples;

import codes.domix.fun.Option;
import codes.domix.fun.Try;
import java.util.Properties;

public class ConfigLoader {
    private final Properties props;

    public ConfigLoader(Properties props) {
        this.props = props;
    }

    public Option<String> getString(String key) {
        return Option.ofNullable(props.getProperty(key));
    }

    public int getInt(String key, int defaultValue) {
        return getString(key)
            .flatMap(this::parseIntSafe)
            .getOrElse(defaultValue);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return getString(key)
            .map(Boolean::parseBoolean)
            .getOrElse(defaultValue);
    }

    private Option<Integer> parseIntSafe(String value) {
        return Try.of(() -> Integer.parseInt(value))
            .toOption();
    }

    void main() {
        // Usage
        ConfigLoader config = new ConfigLoader(props);

        int port = config.getInt("server.port", 8080);
        boolean sslEnabled = config.getBoolean("server.ssl.enabled", false);
        String host = config.getString("server.host")
            .getOrElse("localhost");

        System.out.printf("Server is running on %s:%d%n", host, port);
    }
}
