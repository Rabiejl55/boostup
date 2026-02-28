package services.api;

import entities.Api.NewsItem;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
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
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static List<NewsItem> search(String query, int limit) throws Exception {
        String q = (query == null) ? "" : query.trim();
        if (q.isEmpty()) q = "financement tunisie";

        String url = "https://news.google.com/rss/search?q=" +
                URLEncoder.encode(q, StandardCharsets.UTF_8) +
                "&hl=fr&gl=TN&ceid=TN:fr";

        String xml = fetch(url);
        return parseRss(xml, limit);
    }

    private static String fetch(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("User-Agent", "Mozilla/5.0 JavaFX-App/1.0")
                .header("Accept", "application/rss+xml, application/xml;q=0.9, */*;q=0.8")
                .GET()
                .build();

        HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            throw new RuntimeException("HTTP " + resp.statusCode() + " => " + resp.body());
        }
        return resp.body();
    }

    private static List<NewsItem> parseRss(String xml, int limit) throws Exception {
        List<NewsItem> out = new ArrayList<>();
        if (xml == null || xml.isBlank()) return out;

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

        DocumentBuilder db = dbf.newDocumentBuilder();
        org.w3c.dom.Document doc = db.parse(new org.xml.sax.InputSource(new java.io.StringReader(xml)));
        doc.getDocumentElement().normalize();

        NodeList items = doc.getElementsByTagName("item");
        for (int i = 0; i < items.getLength() && out.size() < limit; i++) {
            Element item = (Element) items.item(i);

            String title = textOf(item, "title").trim();
            String link  = textOf(item, "link").trim();
            String date  = textOf(item, "pubDate").trim();
            String source = textOf(item, "source").trim();

            if (link.isBlank()) link = textOf(item, "guid").trim();

            // 1) Essayer image depuis RSS
            String img = extractRssImage(item);

            // 2) fallback og:image (lent) => on le fait seulement si pas d’image
            if (img.isBlank() && !link.isBlank()) {
                img = tryFetchOgImage(link);
            }

            out.add(new NewsItem(
                    title.isBlank() ? "Sans titre" : title,
                    link,
                    source,
                    date,
                    img
            ));
        }
        return out;
    }

    private static String extractRssImage(Element item) {
        // Google News: parfois media:thumbnail
        NodeList thumbs = item.getElementsByTagNameNS("*", "thumbnail");
        if (thumbs != null && thumbs.getLength() > 0) {
            org.w3c.dom.Node n = thumbs.item(0);
            if (n != null && n.getAttributes() != null && n.getAttributes().getNamedItem("url") != null) {
                return n.getAttributes().getNamedItem("url").getNodeValue();
            }
        }

        // parfois enclosure url=
        NodeList encl = item.getElementsByTagName("enclosure");
        if (encl != null && encl.getLength() > 0) {
            org.w3c.dom.Node n = encl.item(0);
            if (n != null && n.getAttributes() != null && n.getAttributes().getNamedItem("url") != null) {
                return n.getAttributes().getNamedItem("url").getNodeValue();
            }
        }

        return "";
    }

    private static String tryFetchOgImage(String url) {
        try {
            Document d = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 JavaFX-App/1.0")
                    .timeout(6000)
                    .followRedirects(true)
                    .get();

            String og = d.select("meta[property=og:image]").attr("content");
            if (og != null && !og.isBlank()) return og.trim();

            String tw = d.select("meta[name=twitter:image]").attr("content");
            if (tw != null && !tw.isBlank()) return tw.trim();

            return "";
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String textOf(Element parent, String tag) {
        NodeList nl = parent.getElementsByTagName(tag);
        if (nl == null || nl.getLength() == 0) return "";
        org.w3c.dom.Node n = nl.item(0);
        if (n == null) return "";
        return n.getTextContent() == null ? "" : n.getTextContent();
    }
}