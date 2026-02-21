package utils.maps;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class MapHtmlBuilder {

    private MapHtmlBuilder() {}

    public static String buildLeafletHtml(double lat, double lon, String title, String subtitle) {
        // On garde le nom historique de la méthode pour ne pas casser le code existant.
        String safeTitle = escapeHtml(title == null ? "" : title);
        String safeSub = escapeHtml(subtitle == null ? "" : subtitle);

        // Static map (image) : beaucoup plus fiable dans WebView que Leaflet/CDN ou iframe.
        // Service public OSM static map (sans clé) : https://staticmap.openstreetmap.de
        // Exemple:
        // https://staticmap.openstreetmap.de/staticmap.php?center=36.8065,10.1815&zoom=15&size=900x520&markers=36.8065,10.1815,red-pushpin
        int zoom = 15;
        int w = 980;
        int h = 520;
        String img = "https://staticmap.openstreetmap.de/staticmap.php?center=" + lat + "," + lon
                + "&zoom=" + zoom
                + "&size=" + w + "x" + h
                + "&markers=" + lat + "," + lon + ",red-pushpin";

        return """
                <!doctype html>
                <html>
                <head>
                  <meta charset='utf-8'/>
                  <meta name='viewport' content='width=device-width, initial-scale=1.0'/>
                  <style>
                    html, body { height: 100%%; margin: 0; background: #0b1020; font-family: Arial, sans-serif; }
                    .wrap { height: 100%%; display: flex; flex-direction: column; }
                    .bar {
                      padding: 10px 12px;
                      background: linear-gradient(135deg, rgba(27,42,74,0.92), rgba(45,27,78,0.92));
                      color: white;
                      font-weight: 700;
                      font-size: 14px;
                    }
                    .sub { opacity: 0.85; font-weight: 500; margin-top: 4px; font-size: 12px; }
                    .pad { padding: 10px; flex: 1; display: flex; }
                    img {
                      width: 100%%;
                      height: 100%%;
                      object-fit: cover;
                      border-radius: 14px;
                      box-shadow: 0 12px 28px rgba(0,0,0,0.35);
                      border: 1px solid rgba(255,255,255,0.08);
                      background: rgba(255,255,255,0.03);
                    }
                    .fallback {
                      color: rgba(255,255,255,0.75);
                      font-size: 13px;
                      padding-top: 10px;
                    }
                  </style>
                </head>
                <body>
                  <div class='wrap'>
                    <div class='bar'>
                      %s
                      <div class='sub'>%s</div>
                    </div>
                    <div class='pad'>
                      <div style='width:100%%; height:100%%;'>
                        <img src='%s' alt='map' onerror="document.getElementById('fb').style.display='block'"/>
                        <div id='fb' class='fallback' style='display:none;'>
                          ⚠️ La carte n'a pas pu être chargée dans WebView. Utilise le bouton « Ouvrir dans Maps ».
                        </div>
                      </div>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(safeTitle, safeSub, img);
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
