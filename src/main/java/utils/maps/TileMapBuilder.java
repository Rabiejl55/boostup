package utils.maps;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Construit une carte à partir des tuiles OSM (tile.openstreetmap.org).
 * Utilisé en fallback quand staticmap.openstreetmap.de est bloqué.
 */
public final class TileMapBuilder {

    private TileMapBuilder() {}

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(6))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /**
     * Construit une image carte (980x520) à partir de tuiles OSM.
     * @param lat latitude
     * @param lon longitude
     * @param zoom niveau de zoom (15 recommandé)
     * @return Image JavaFX
     */
    public static CompletableFuture<Image> buildMapImage(double lat, double lon, int zoom) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                int targetW = 980;
                int targetH = 520;

                // Convertir lat/lon en coordonnées tuiles
                int centerTileX = lonToTile(lon, zoom);
                int centerTileY = latToTile(lat, zoom);

                // Charger une grille 3x2 de tuiles (768x512) puis redimensionner
                int tilesX = 3;
                int tilesY = 2;

                List<CompletableFuture<TileImage>> futures = new ArrayList<>();
                for (int dy = 0; dy < tilesY; dy++) {
                    for (int dx = 0; dx < tilesX; dx++) {
                        int tileX = centerTileX - 1 + dx;
                        int tileY = centerTileY - 1 + dy;
                        futures.add(downloadTile(zoom, tileX, tileY, dx, dy));
                    }
                }

                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

                // Assembler les tuiles
                BufferedImage canvas = new BufferedImage(tilesX * 256, tilesY * 256, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = canvas.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.setColor(Color.decode("#0b1020"));
                g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

                for (CompletableFuture<TileImage> f : futures) {
                    TileImage tile = f.getNow(null);
                    if (tile != null && tile.img != null) {
                        g.drawImage(tile.img, tile.gridX * 256, tile.gridY * 256, null);
                    }
                }

                // Marqueur au centre
                int markerX = (tilesX * 256) / 2;
                int markerY = (tilesY * 256) / 2;
                g.setColor(Color.decode("#e63956"));
                g.fillOval(markerX - 10, markerY - 10, 20, 20);
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke(2));
                g.drawOval(markerX - 15, markerY - 15, 30, 30);

                g.dispose();

                // Redimensionner vers 980x520
                BufferedImage resized = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_RGB);
                Graphics2D g2 = resized.createGraphics();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.drawImage(canvas, 0, 0, targetW, targetH, null);
                g2.dispose();

                return SwingFXUtils.toFXImage(resized, null);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static CompletableFuture<TileImage> downloadTile(int zoom, int x, int y, int gridX, int gridY) {
        String url = "https://tile.openstreetmap.org/" + zoom + "/" + x + "/" + y + ".png";
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("User-Agent", "BoostUp/1.0 (JavaFX Educational App)")
                .GET()
                .build();

        return HTTP.sendAsync(req, HttpResponse.BodyHandlers.ofByteArray())
                .thenApply(resp -> {
                    if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                        return new TileImage(null, gridX, gridY);
                    }
                    try {
                        BufferedImage img = ImageIO.read(new ByteArrayInputStream(resp.body()));
                        return new TileImage(img, gridX, gridY);
                    } catch (Exception e) {
                        return new TileImage(null, gridX, gridY);
                    }
                })
                .exceptionally(ex -> new TileImage(null, gridX, gridY));
    }

    private static int lonToTile(double lon, int zoom) {
        return (int) Math.floor((lon + 180.0) / 360.0 * (1 << zoom));
    }

    private static int latToTile(double lat, int zoom) {
        double latRad = Math.toRadians(lat);
        return (int) Math.floor((1.0 - Math.log(Math.tan(latRad) + 1.0 / Math.cos(latRad)) / Math.PI) / 2.0 * (1 << zoom));
    }

    private static class TileImage {
        final BufferedImage img;
        final int gridX;
        final int gridY;

        TileImage(BufferedImage img, int gridX, int gridY) {
            this.img = img;
            this.gridX = gridX;
            this.gridY = gridY;
        }
    }
}

