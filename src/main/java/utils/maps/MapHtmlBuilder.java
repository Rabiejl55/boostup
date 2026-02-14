package utils.maps;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class MapHtmlBuilder {

    private MapHtmlBuilder() {}

    public static String buildLeafletHtml(double lat, double lon, String title, String subtitle) {
        String safeTitle = escapeHtml(title == null ? "" : title);
        String safeSub = escapeHtml(subtitle == null ? "" : subtitle);

        // IMPORTANT: String#formatted utilise %... et Leaflet utilise {s}{z}{x}{y}.
        // Donc on échappe les accolades en {{ }} pour éviter une erreur de format.
        String template = """
                <!doctype html>
                <html>
                <head>
                  <meta charset='utf-8'/>
                  <meta name='viewport' content='width=device-width, initial-scale=1.0'/>
                  <link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css' crossorigin=''/>
                  <script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js' crossorigin=''></script>
                  <style>
                    html, body { height: 100%%; margin: 0; background: #0b1020; }
                    #map { height: 100%%; width: 100%%; }
                    .leaflet-popup-content-wrapper { border-radius: 14px; }
                  </style>
                </head>
                <body>
                  <div id='map'></div>
                  <script>
                    const lat = %f;
                    const lon = %f;
                    const map = L.map('map', { zoomControl: true }).setView([lat, lon], 15);
                    L.tileLayer('https://{{s}}.tile.openstreetmap.org/{{z}}/{{x}}/{{y}}.png', {
                      maxZoom: 19,
                      attribution: '&copy; OpenStreetMap'
                    }).addTo(map);

                    const marker = L.marker([lat, lon]).addTo(map);
                    marker.bindPopup('<b>%s</b><br/>%s').openPopup();
                    setTimeout(() => map.setView([lat, lon], 16, { animate: true }), 250);
                  </script>
                </body>
                </html>
                """;

        return template.formatted(lat, lon, safeTitle, safeSub);
    }

    public static String toDataUrl(String html) {
        String encoded = URLEncoder.encode(html, StandardCharsets.UTF_8);
        return "data:text/html;charset=utf-8," + encoded;
    }

    private static String escapeHtml(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
