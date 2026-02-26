package services.api;

import entities.Api.NewsItem;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class RssNewsService {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL) // ✅ suit 301/302 automatiquement
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static List<NewsItem> search(String query, int limit) throws Exception {
        String q = (query == null) ? "" : query.trim();
        if (q.isEmpty()) q = "financement tunisie";

        String url = "https://news.google.com/rss/search?q=" +
                URLEncoder.encode(q, StandardCharsets.UTF_8) +
                "&hl=fr&gl=TN&ceid=TN:fr";

        String xml = fetchWithRedirectFallback(url);

        // ⚠️ Ici tu gardes TON parsing actuel RSS -> List<NewsItem>
        // Je te laisse la méthode parseRss(...) comme tu l’avais déjà.
        return parseRss(xml, limit);
    }

    private static String fetchWithRedirectFallback(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) JavaFX-App/1.0") // ✅ évite certains 302
                .header("Accept", "application/rss+xml, application/xml;q=0.9, */*;q=0.8")
                .GET()
                .build();

        HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());

        // ✅ Si malgré followRedirects on reçoit encore un redirect
        if (resp.statusCode() == 301 || resp.statusCode() == 302 || resp.statusCode() == 303
                || resp.statusCode() == 307 || resp.statusCode() == 308) {

            String location = resp.headers().firstValue("location").orElse("");
            if (location.isBlank()) {
                throw new RuntimeException("HTTP " + resp.statusCode() + " (redirect sans Location)");
            }

            HttpRequest req2 = HttpRequest.newBuilder()
                    .uri(URI.create(location.startsWith("http") ? location : "https://news.google.com" + location))
                    .timeout(Duration.ofSeconds(20))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) JavaFX-App/1.0")
                    .header("Accept", "application/rss+xml, application/xml;q=0.9, */*;q=0.8")
                    .GET()
                    .build();

            HttpResponse<String> resp2 = CLIENT.send(req2, HttpResponse.BodyHandlers.ofString());
            if (resp2.statusCode() < 200 || resp2.statusCode() >= 300) {
                throw new RuntimeException("HTTP " + resp2.statusCode() + " => " + resp2.body());
            }
            return resp2.body();
        }

        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            throw new RuntimeException("HTTP " + resp.statusCode() + " => " + resp.body());
        }

        return resp.body();
    }

    // ✅ Garde ton parsing RSS existant ici
    private static List<NewsItem> parseRss(String xml, int limit) throws Exception {
        List<NewsItem> out = new ArrayList<>();
        if (xml == null || xml.isBlank()) return out;

        javax.xml.parsers.DocumentBuilderFactory dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(false);
        dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

        javax.xml.parsers.DocumentBuilder db = dbf.newDocumentBuilder();
        org.w3c.dom.Document doc = db.parse(new org.xml.sax.InputSource(new java.io.StringReader(xml)));
        doc.getDocumentElement().normalize();

        org.w3c.dom.NodeList items = doc.getElementsByTagName("item");
        for (int i = 0; i < items.getLength() && out.size() < limit; i++) {
            org.w3c.dom.Element item = (org.w3c.dom.Element) items.item(i);

            String title  = textOf(item, "title");
            String link   = textOf(item, "link");
            String date   = textOf(item, "pubDate");
            String source = textOf(item, "source");
            String desc   = textOf(item, "description");

            if (link == null || link.isBlank()) link = textOf(item, "guid");
            if (title == null) title = "";
            if (link == null) link = "";

            String imageUrl = extractImageUrl(item, desc);

            NewsItem ni = new NewsItem(
                    title.trim(),
                    link.trim(),
                    source == null ? "" : source.trim(),
                    date == null ? "" : date.trim()
            );
            // ✅ nouveau champ
            ni.setImageUrl(imageUrl);

            out.add(ni);
        }
        return out;
    }

    private static String extractImageUrl(org.w3c.dom.Element item, String descriptionHtml) {
        // 1) media:content url=
        String url = firstAttr(item, "media:content", "url");
        if (!isBlank(url)) return url;

        // 2) media:thumbnail url=
        url = firstAttr(item, "media:thumbnail", "url");
        if (!isBlank(url)) return url;

        // 3) <img src="..."> dans description
        if (!isBlank(descriptionHtml)) {
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("<img[^>]+src\\s*=\\s*\"([^\"]+)\"", java.util.regex.Pattern.CASE_INSENSITIVE)
                    .matcher(descriptionHtml);
            if (m.find()) return m.group(1);
        }

        return ""; // pas d’image
    }

    private static String firstAttr(org.w3c.dom.Element parent, String tagName, String attr) {
        org.w3c.dom.NodeList nl = parent.getElementsByTagName(tagName);
        if (nl == null || nl.getLength() == 0) return "";
        org.w3c.dom.Node n = nl.item(0);
        if (!(n instanceof org.w3c.dom.Element el)) return "";
        return el.getAttribute(attr);
    }

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

    private static String textOf(org.w3c.dom.Element parent, String tag) {
        org.w3c.dom.NodeList nl = parent.getElementsByTagName(tag);
        if (nl == null || nl.getLength() == 0) return "";
        org.w3c.dom.Node n = nl.item(0);
        if (n == null) return "";
        return n.getTextContent();
    }


}