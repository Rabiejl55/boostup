package utils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Petit diagnostic réseau pour comprendre les ConnectException.
 * Usage ponctuel: NetDiagnostics.quick();
 */
public final class NetDiagnostics {

    private NetDiagnostics() {}

    public static String quick() {
        StringBuilder sb = new StringBuilder();
        sb.append("Java version: ").append(System.getProperty("java.version")).append('\n');
        sb.append("OS: ").append(System.getProperty("os.name")).append(' ').append(System.getProperty("os.version")).append('\n');
        sb.append("Use system proxies: ").append(System.getProperty("java.net.useSystemProxies")).append('\n');

        sb.append("\n== HTTP checks ==\n");
        sb.append(check("https://www.google.com/"));
        sb.append(check("https://nominatim.openstreetmap.org/"));
        sb.append(check("https://staticmap.openstreetmap.de/"));
        sb.append(check("https://tile.openstreetmap.org/0/0/0.png"));
        return sb.toString();
    }

    private static String check(String url) {
        try {
            HttpClient http = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();

            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(8))
                    .header("User-Agent", "BoostUp/1.0 (NetDiagnostics)")
                    .GET()
                    .build();

            HttpResponse<Void> resp = http.send(req, HttpResponse.BodyHandlers.discarding());
            return "OK " + resp.statusCode() + " - " + url + "\n";
        } catch (Exception e) {
            return "FAIL " + e.getClass().getSimpleName() + " - " + url + " - " + (e.getMessage() == null ? "" : e.getMessage()) + "\n";
        }
    }
}

