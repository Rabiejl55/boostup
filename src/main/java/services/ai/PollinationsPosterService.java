package services.ai;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Génération d'affiches via Pollinations (sans clé API).
 * On génère une image en construisant une URL basée sur un prompt.
 */
public class PollinationsPosterService {

    public String buildPrompt(String titre, String description, String date, String lieu) {
        return ("Create a professional modern event poster. " +
                "Event Title: " + nvl(titre, "(Untitled)") + ". " +
                "Description: " + nvl(description, "") + ". " +
                "Date: " + nvl(date, "-") + ". " +
                "Location: " + nvl(lieu, "-") + ". " +
                "Style: premium, modern typography, vibrant blue/violet colors, cinematic lighting, high resolution, social media ready poster.")
                .trim();
    }

    public String buildImageUrl(String prompt, int width, int height) {
        Objects.requireNonNull(prompt, "prompt");

        int w = width <= 0 ? 1024 : width;
        int h = height <= 0 ? 1024 : height;

        // URLEncoder => application/x-www-form-urlencoded (espaces -> +). C'est OK ici.
        String encoded = URLEncoder.encode(prompt, StandardCharsets.UTF_8);

        // seed variable pour éviter le cache quand on régénère
        long seed = System.currentTimeMillis();

        // nologo=true : évite certains watermarks/overlays, model=flux : plutôt stable
        return "https://image.pollinations.ai/prompt/" + encoded
                + "?width=" + w
                + "&height=" + h
                + "&seed=" + seed
                + "&nologo=true"
                + "&model=flux";
    }

    private static String nvl(String s, String fallback) {
        return (s == null || s.trim().isEmpty()) ? fallback : s.trim();
    }
}
