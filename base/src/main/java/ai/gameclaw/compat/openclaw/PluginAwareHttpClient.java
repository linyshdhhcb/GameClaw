package ai.gameclaw.compat.openclaw;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

public class PluginAwareHttpClient {

    private static final Logger log = LoggerFactory.getLogger(PluginAwareHttpClient.class);

    private final HttpClient httpClient;
    private final String pluginName;
    private final List<String> allowedHosts;

    public PluginAwareHttpClient(String pluginName, List<String> allowedHosts) {
        this.pluginName = pluginName;
        this.allowedHosts = allowedHosts != null ? allowedHosts : List.of();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .proxy(ProxySelector.of(null))
                .build();
    }

    public String get(String url) throws IOException, InterruptedException {
        assertNetworkAllowed(url);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    public String post(String url, String body, String contentType) throws IOException, InterruptedException {
        assertNetworkAllowed(url);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", contentType)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    void assertNetworkAllowed(String url) {
        if (allowedHosts.isEmpty()) {
            log.warn("[PluginAwareHttpClient] No network permissions for plugin={}, url={}", pluginName, url);
            throw new SecurityException("Plugin '" + pluginName + "' has no network permissions");
        }
        if (allowedHosts.contains("*")) return;

        URI uri = URI.create(url);
        String host = uri.getHost();
        if (host == null) {
            throw new SecurityException("Invalid URL: " + url);
        }
        boolean allowed = allowedHosts.stream()
                .anyMatch(perm -> {
                    if (perm.startsWith("net:host:")) {
                        String allowedHost = perm.substring("net:host:".length());
                        return host.equals(allowedHost) || host.endsWith("." + allowedHost);
                    }
                    return false;
                });
        if (!allowed) {
            log.warn("[PluginAwareHttpClient] Network access blocked: plugin={}, host={}, url={}",
                    pluginName, host, url);
            throw new SecurityException("Plugin '" + pluginName + "' cannot access host: " + host);
        }
    }
}
