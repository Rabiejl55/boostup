package services.ai;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

/**
 * Fallback 100% local: génère une affiche "premium" sans dépendre d'une API.
 * Objectif: produire une image téléchargeable et présentable même hors-ligne.
 */
public final class LocalPosterGenerator {

    private LocalPosterGenerator() {
    }

    /**
     * @param qrFxImage image JavaFX déjà générée (ex: via QrCodeUtils.toQrImage(payload, size)).
     */
    public static Image generate(int width,
                                 int height,
                                 String titre,
                                 String description,
                                 String date,
                                 String lieu,
                                 String type,
                                 Image eventImageOrNull,
                                 Image qrFxImage) {

        int w = Math.max(720, width);
        int h = Math.max(720, height);

        BufferedImage canvas = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            // ===== Background premium (gradient + waves + particles) =====
            GradientPaint bg = new GradientPaint(0, 0, new Color(0x0B1020), w, h, new Color(0x241A4A));
            g.setPaint(bg);
            g.fillRect(0, 0, w, h);

            // Waves overlay
            paintWave(g, w, h, new Color(0x1E3CFD), 0.18f, 0.22f);
            paintWave(g, w, h, new Color(0x6F42C1), 0.18f, 0.55f);
            paintWave(g, w, h, new Color(0x00E5FF), 0.10f, 0.75f);

            // soft glows
            paintGlow(g, w * 0.18f, h * 0.14f, w * 0.60f, new Color(0x3D8BFD), 0.10f);
            paintGlow(g, w * 0.82f, h * 0.22f, w * 0.48f, new Color(0xB388FF), 0.10f);
            paintGlow(g, w * 0.50f, h * 0.88f, w * 0.76f, new Color(0x00E5FF), 0.08f);

            // particles
            paintParticles(g, w, h);

            int padding = (int) (w * 0.055);
            int cardX = padding;
            int cardY = padding;
            int cardW = w - padding * 2;
            int cardH = h - padding * 2;

            // ===== Glass card =====
            g.setComposite(AlphaComposite.SrcOver.derive(0.22f));
            g.setColor(Color.WHITE);
            g.fill(new RoundRectangle2D.Float(cardX, cardY, cardW, cardH, 42, 42));
            g.setComposite(AlphaComposite.SrcOver.derive(0.18f));
            g.setStroke(new BasicStroke(2f));
            g.setColor(Color.WHITE);
            g.draw(new RoundRectangle2D.Float(cardX, cardY, cardW, cardH, 42, 42));
            g.setComposite(AlphaComposite.SrcOver);

            // ===== Hero image =====
            int heroH = (int) (cardH * 0.42);
            int heroX = cardX + 18;
            int heroY = cardY + 18;
            int heroW = cardW - 36;

            BufferedImage hero = toBufferedImageSafe(eventImageOrNull);
            if (hero != null) {
                BufferedImage scaled = scaleCrop(hero, heroW, heroH);
                drawRoundedImage(g, scaled, heroX, heroY, heroW, heroH, 30);
            } else {
                GradientPaint ph = new GradientPaint(heroX, heroY, new Color(0x10121C), heroX + heroW, heroY + heroH, new Color(0x1A244A));
                g.setPaint(ph);
                g.fill(new RoundRectangle2D.Float(heroX, heroY, heroW, heroH, 30, 30));
            }

            // LED border effect
            g.setStroke(new BasicStroke(7f));
            g.setPaint(new GradientPaint(heroX, heroY, new Color(0x3D8BFD), heroX + heroW, heroY + heroH, new Color(0x6F42C1)));
            g.setComposite(AlphaComposite.SrcOver.derive(0.35f));
            g.draw(new RoundRectangle2D.Float(heroX + 2, heroY + 2, heroW - 4, heroH - 4, 30, 30));
            g.setComposite(AlphaComposite.SrcOver);

            // subtle dark vignette on hero bottom
            g.setComposite(AlphaComposite.SrcOver.derive(0.22f));
            g.setPaint(new GradientPaint(heroX, heroY + heroH * 0.55f, new Color(0, 0, 0, 0), heroX, heroY + heroH, new Color(0, 0, 0, 255)));
            g.fill(new RoundRectangle2D.Float(heroX, heroY, heroW, heroH, 30, 30));
            g.setComposite(AlphaComposite.SrcOver);

            // ===== Text section =====
            int textX = heroX;
            int textY = heroY + heroH + 26;

            // Type badge
            String badge = safe(type).isBlank() ? "ÉVÉNEMENT" : safe(type).toUpperCase();
            g.setFont(new Font("Segoe UI", Font.BOLD, 18));
            int badgePadX = 14;
            int badgePadY = 8;
            int badgeW = g.getFontMetrics().stringWidth(badge) + badgePadX * 2;
            int badgeH = g.getFontMetrics().getHeight() + badgePadY;
            g.setComposite(AlphaComposite.SrcOver.derive(0.92f));
            g.setPaint(new GradientPaint(textX, textY, new Color(0x00E5FF), textX + badgeW, textY + badgeH, new Color(0x6F42C1)));
            g.fill(new RoundRectangle2D.Float(textX, textY, badgeW, badgeH, 999, 999));
            g.setColor(Color.WHITE);
            g.drawString(badge, textX + badgePadX, textY + badgeH - badgePadY);
            g.setComposite(AlphaComposite.SrcOver);

            textY += badgeH + 16;

            // Title
            String title = safe(titre).isBlank() ? "(Sans titre)" : safe(titre);
            g.setFont(new Font("Segoe UI", Font.BOLD, 46));
            g.setColor(Color.WHITE);
            textY = drawWrappedText(g, title, textX, textY, heroW, 50, 2);

            // Meta line
            g.setFont(new Font("Segoe UI", Font.PLAIN, 22));
            g.setColor(new Color(255, 255, 255, 220));
            String meta = "📅 " + safe(date) + "   •   📍 " + safe(lieu);
            textY = drawWrappedText(g, meta, textX, textY + 6, heroW, 30, 2);

            // Description
            g.setFont(new Font("Segoe UI", Font.PLAIN, 20));
            g.setColor(new Color(255, 255, 255, 200));
            String desc = safe(description);
            if (desc.length() > 520) desc = desc.substring(0, 520) + "…";
            drawWrappedText(g, desc, textX, textY + 12, heroW, 26, 7);

            // Footer line
            g.setFont(new Font("Segoe UI", Font.BOLD, 18));
            g.setColor(new Color(255, 255, 255, 200));
            String footer = "BoostUp • Affiche générée automatiquement";
            int footerY = cardY + cardH - 20;
            g.drawString(footer, textX, footerY);

            // ===== QR code bottom-right =====
            if (qrFxImage != null) {
                BufferedImage qr = toBufferedImageSafe(qrFxImage);
                if (qr != null) {
                    int qrSize = (int) Math.min(230, cardW * 0.22);
                    int qrX = cardX + cardW - qrSize - 28;
                    int qrY = cardY + cardH - qrSize - 28;

                    // white rounded container
                    g.setComposite(AlphaComposite.SrcOver.derive(0.92f));
                    g.setColor(Color.WHITE);
                    g.fill(new RoundRectangle2D.Float(qrX - 12, qrY - 12, qrSize + 24, qrSize + 40, 22, 22));

                    // small label
                    g.setComposite(AlphaComposite.SrcOver);
                    g.setFont(new Font("Segoe UI", Font.BOLD, 14));
                    g.setColor(new Color(0x10121C));
                    g.drawString("Scan ↗", qrX - 2, qrY + qrSize + 18);

                    // QR itself
                    BufferedImage qrScaled = scaleFit(qr, qrSize, qrSize);
                    drawRoundedImage(g, qrScaled, qrX, qrY, qrSize, qrSize, 16);

                    // tiny accent dot
                    g.setColor(new Color(0x6F42C1));
                    g.fill(new Ellipse2D.Float(qrX + qrSize - 10, qrY - 10, 10, 10));
                }
            }

        } finally {
            g.dispose();
        }

        return SwingFXUtils.toFXImage(canvas, null);
    }

    // --- Backward compatible helper (sans QR) ---
    public static Image generate(int width,
                                 int height,
                                 String titre,
                                 String description,
                                 String date,
                                 String lieu,
                                 String type,
                                 Image eventImageOrNull) {
        return generate(width, height, titre, description, date, lieu, type, eventImageOrNull, null);
    }

    private static void paintWave(Graphics2D g, int w, int h, Color color, float alpha, float yFactor) {
        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.SrcOver.derive(alpha));
        g.setColor(color);

        int baseY = (int) (h * yFactor);
        int amp = (int) (h * 0.06);

        Polygon p = new Polygon();
        p.addPoint(0, baseY);
        for (int x = 0; x <= w; x += 30) {
            int y = (int) (baseY + Math.sin(x / 120.0) * amp);
            p.addPoint(x, y);
        }
        p.addPoint(w, h);
        p.addPoint(0, h);
        g.fillPolygon(p);
        g.setComposite(old);
    }

    private static void paintParticles(Graphics2D g, int w, int h) {
        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.SrcOver.derive(0.10f));
        for (int i = 0; i < 65; i++) {
            int x = (int) (Math.random() * w);
            int y = (int) (Math.random() * h);
            int r = 2 + (int) (Math.random() * 4);
            g.setColor(i % 3 == 0 ? new Color(0x00E5FF) : (i % 3 == 1 ? new Color(0xB388FF) : new Color(0x3D8BFD)));
            g.fillOval(x, y, r, r);
        }
        g.setComposite(old);
    }

    private static void paintGlow(Graphics2D g, float cx, float cy, float radius, Color color, float alpha) {
        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.SrcOver.derive(alpha));
        for (int i = 10; i >= 1; i--) {
            float r = radius * i / 10f;
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(255, (int) (255 * alpha / i)))));
            g.fillOval((int) (cx - r / 2), (int) (cy - r / 2), (int) r, (int) r);
        }
        g.setComposite(old);
    }

    private static BufferedImage toBufferedImageSafe(Image fx) {
        if (fx == null) return null;
        try {
            return SwingFXUtils.fromFXImage(fx, null);
        } catch (Exception e) {
            return null;
        }
    }

    private static BufferedImage scaleCrop(BufferedImage src, int targetW, int targetH) {
        double srcRatio = src.getWidth() / (double) src.getHeight();
        double tgtRatio = targetW / (double) targetH;

        int cropW = src.getWidth();
        int cropH = src.getHeight();
        int cropX = 0;
        int cropY = 0;

        if (srcRatio > tgtRatio) {
            cropW = (int) Math.round(src.getHeight() * tgtRatio);
            cropX = (src.getWidth() - cropW) / 2;
        } else {
            cropH = (int) Math.round(src.getWidth() / tgtRatio);
            cropY = (src.getHeight() - cropH) / 2;
        }

        BufferedImage cropped = src.getSubimage(cropX, cropY, cropW, cropH);
        BufferedImage scaled = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(cropped, 0, 0, targetW, targetH, null);
        g.dispose();
        return scaled;
    }

    private static BufferedImage scaleFit(BufferedImage src, int targetW, int targetH) {
        BufferedImage scaled = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double sx = targetW / (double) src.getWidth();
        double sy = targetH / (double) src.getHeight();
        double s = Math.min(sx, sy);
        int w = (int) Math.round(src.getWidth() * s);
        int h = (int) Math.round(src.getHeight() * s);
        int x = (targetW - w) / 2;
        int y = (targetH - h) / 2;

        g.drawImage(src, x, y, w, h, null);
        g.dispose();
        return scaled;
    }

    private static void drawRoundedImage(Graphics2D g, BufferedImage img, int x, int y, int w, int h, int arc) {
        Shape oldClip = g.getClip();
        RoundRectangle2D rr = new RoundRectangle2D.Float(x, y, w, h, arc, arc);
        g.setClip(rr);
        g.drawImage(img, x, y, w, h, null);
        g.setClip(oldClip);

        // subtle inner shadow
        g.setComposite(AlphaComposite.SrcOver.derive(0.18f));
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(2f));
        g.draw(rr);
        g.setComposite(AlphaComposite.SrcOver);
    }

    private static int drawWrappedText(Graphics2D g, String text, int x, int y, int maxWidth, int lineHeight, int maxLines) {
        if (text == null) text = "";
        FontMetrics fm = g.getFontMetrics();
        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();
        int lines = 0;

        for (String w : words) {
            String candidate = line.isEmpty() ? w : line + " " + w;
            if (fm.stringWidth(candidate) > maxWidth) {
                g.drawString(line.toString(), x, y);
                y += lineHeight;
                lines++;
                line.setLength(0);
                line.append(w);
                if (lines >= maxLines) {
                    return y;
                }
            } else {
                line.setLength(0);
                line.append(candidate);
            }
        }

        if (!line.isEmpty() && lines < maxLines) {
            g.drawString(line.toString(), x, y);
            y += lineHeight;
        }
        return y;
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
