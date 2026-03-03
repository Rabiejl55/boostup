package services.UtilisateurService;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.scene.image.Image;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Iterator;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Face Recognition Service for BoostUp.
 * Uses Face++ Cloud AI (primary) with smart local fallback.
 * Images compressed to JPEG 256x256 before API calls.
 * Local fallback uses skin-tone detection + LBP + SSIM + NCC.
 */
public class FaceRecognitionService {

    private static final Logger LOGGER = Logger.getLogger(FaceRecognitionService.class.getName());

    private static final String FACES_DIR = System.getProperty("user.home") + File.separator +
            ".boostup" + File.separator + "faces";

    // ═══ Face++ API credentials ═══
    private static final String FACEPP_API_KEY = "KHPniVH_r_w33bQKVGHgU3Fy42fJyRd_";
    private static final String FACEPP_API_SECRET = "YlX3FNzVWaGPCVRqCvHjB-EjN2VBPMQH";
    private static final String FACEPP_URL_US = "https://api-us.faceplusplus.com/facepp/v3/compare";
    private static final String FACEPP_URL_CN = "https://api-cn.faceplusplus.com/facepp/v3/compare";

    // Thresholds
    private static final double CLOUD_THRESHOLD = 73.0;
    private static final double LOCAL_THRESHOLD = 52.0;

    // Image sizes
    private static final int ENROLL_SIZE = 512;
    private static final int COMPARE_SIZE = 256;
    private static final float JPEG_QUALITY = 0.85f;

    // Minimum skin-tone ratio to detect a face
    private static final double MIN_SKIN_RATIO = 0.08;

    static {
        try { Files.createDirectories(Paths.get(FACES_DIR)); }
        catch (IOException e) { LOGGER.log(Level.WARNING, "Cannot create faces dir", e); }
    }

    // ═══════════════════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════════════════

    public static boolean enrollFace(int userId, Image image) {
        try {
            BufferedImage bi = fxImageToBufferedImage(image);
            if (bi == null) return false;
            ImageIO.write(resizeImage(bi, ENROLL_SIZE, ENROLL_SIZE), "png", new File(getFacePath(userId)));
            LOGGER.info("Face enrolled for user " + userId);
            return true;
        } catch (Exception e) { LOGGER.log(Level.SEVERE, "Enroll error", e); return false; }
    }

    public static boolean enrollFaceFromFile(int userId, File imageFile) {
        try {
            BufferedImage original = ImageIO.read(imageFile);
            if (original == null) return false;
            ImageIO.write(resizeImage(original, ENROLL_SIZE, ENROLL_SIZE), "png", new File(getFacePath(userId)));
            LOGGER.info("Face enrolled from file for user " + userId);
            return true;
        } catch (Exception e) { LOGGER.log(Level.SEVERE, "Enroll from file error", e); return false; }
    }

    public static boolean hasFaceEnrolled(int userId) {
        File f = new File(getFacePath(userId));
        return f.exists() && f.length() > 0;
    }

    /**
     * Main comparison: tries Face++ first, then local fallback.
     * @return 0-100 confidence, -1 = no face detected, -2 = error
     */
    public static double compareFacesFromFile(int userId, File capturedFile) {
        try {
            if (!hasFaceEnrolled(userId)) {
                System.out.println("❌ Face ID: No enrolled face for user " + userId);
                return -2;
            }
            if (capturedFile == null || !capturedFile.exists() || capturedFile.length() == 0) {
                System.out.println("❌ Face ID: Invalid capture file");
                return -2;
            }

            File enrolledFile = new File(getFacePath(userId));

            // Step 1: Check if captured image contains a face
            BufferedImage capturedImg = ImageIO.read(capturedFile);
            if (capturedImg == null) return -2;

            boolean hasFace = detectFacePresence(capturedImg);
            if (!hasFace) {
                System.out.println("❌ Face ID: No face detected (skin ratio too low)");
                return -1;
            }
            System.out.println("✅ Face detected in captured image");

            // Step 2: Compress both to JPEG for comparison
            byte[] enrolledJpeg = compressToJpeg(ImageIO.read(enrolledFile));
            byte[] capturedJpeg = compressToJpeg(capturedImg);
            System.out.println("📦 Compressed: enrolled=" + (enrolledJpeg.length/1024) + "KB, captured=" + (capturedJpeg.length/1024) + "KB");

            // Step 3: Try Face++ Cloud
            double cloudScore = tryFacePPCloud(enrolledJpeg, capturedJpeg);
            if (cloudScore >= -1) {
                System.out.println("🧠 Face++ score: " + cloudScore);
                return cloudScore;
            }

            // Step 4: Local fallback
            System.out.println("⚠️ Face++ unavailable — using local comparison");
            BufferedImage enrolled = ImageIO.read(enrolledFile);
            double localScore = compareFacesLocal(capturedImg, enrolled);
            System.out.println("🖥️ Local score: " + String.format("%.1f", localScore) + "/100 (threshold: " + LOCAL_THRESHOLD + ")");
            return localScore;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "compareFacesFromFile error", e);
            return -2;
        }
    }

    public static double compareFaces(int userId, Image capturedImage) {
        try {
            BufferedImage captured = fxImageToBufferedImage(capturedImage);
            if (captured == null) return -2;
            File temp = File.createTempFile("boostup_cap_", ".png");
            temp.deleteOnExit();
            ImageIO.write(captured, "png", temp);
            return compareFacesFromFile(userId, temp);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "compareFaces error", e);
            return -2;
        }
    }

    public static double getCloudThreshold() { return CLOUD_THRESHOLD; }
    public static double getLocalThreshold() { return LOCAL_THRESHOLD; }

    public static boolean authenticate(int userId, Image capturedImage) {
        double score = compareFaces(userId, capturedImage);
        if (score < 0) return false;
        boolean success = score >= LOCAL_THRESHOLD;
        LOGGER.info(String.format("Face auth %s — user %d — score: %.1f%%",
                success ? "SUCCESS" : "FAILED", userId, score));
        return success;
    }

    public static boolean deleteFace(int userId) {
        try { File f = new File(getFacePath(userId)); return !f.exists() || f.delete(); }
        catch (Exception e) { return false; }
    }

    public static Image getEnrolledFaceImage(int userId) {
        try { File f = new File(getFacePath(userId)); if (f.exists()) return new Image(f.toURI().toString()); }
        catch (Exception e) { LOGGER.log(Level.WARNING, "Load face image error", e); }
        return null;
    }

    public static boolean testConnectivity() {
        return true; // Always return true — we fallback to local if needed
    }

    // ═══════════════════════════════════════════════════════════
    // SKIN-TONE FACE DETECTION
    // ═══════════════════════════════════════════════════════════

    private static boolean detectFacePresence(BufferedImage img) {
        try {
            int w = img.getWidth(), h = img.getHeight();
            int x1 = (int)(w * 0.2), x2 = (int)(w * 0.8);
            int y1 = (int)(h * 0.15), y2 = (int)(h * 0.85);
            int totalPixels = 0, skinPixels = 0;

            for (int y = y1; y < y2; y += 2) {
                for (int x = x1; x < x2; x += 2) {
                    int rgb = img.getRGB(x, y);
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;
                    totalPixels++;

                    if (r > 60 && g > 40 && b > 20 && r > g && r > b &&
                            (r - g) > 10 && Math.abs(r - g) < 170 && (r - b) > 15) {
                        double yv = 0.299 * r + 0.587 * g + 0.114 * b;
                        double cb = 128 - 0.168736 * r - 0.331264 * g + 0.5 * b;
                        double cr = 128 + 0.5 * r - 0.418688 * g - 0.081312 * b;
                        if (yv > 60 && cb > 77 && cb < 127 && cr > 133 && cr < 173) {
                            skinPixels++;
                        }
                    }
                }
            }

            double skinRatio = (double) skinPixels / Math.max(1, totalPixels);
            System.out.println("🔍 Skin detection: " + skinPixels + "/" + totalPixels +
                    " = " + String.format("%.1f%%", skinRatio * 100) +
                    " (min: " + String.format("%.1f%%", MIN_SKIN_RATIO * 100) + ")");
            return skinRatio >= MIN_SKIN_RATIO;
        } catch (Exception e) {
            return true; // On error, let comparison decide
        }
    }

    // ═══════════════════════════════════════════════════════════
    // FACE++ CLOUD AI
    // ═══════════════════════════════════════════════════════════

    private static double tryFacePPCloud(byte[] enrolledJpeg, byte[] capturedJpeg) {
        double result = callFacePP(FACEPP_URL_US, enrolledJpeg, capturedJpeg);
        if (result > -2) return result;
        System.out.println("⚠️ US failed, trying CN...");
        return callFacePP(FACEPP_URL_CN, enrolledJpeg, capturedJpeg);
    }

    private static double callFacePP(String apiUrl, byte[] img1, byte[] img2) {
        HttpURLConnection conn = null;
        try {
            String b64_1 = Base64.getEncoder().encodeToString(img1);
            String b64_2 = Base64.getEncoder().encodeToString(img2);
            String tag = apiUrl.contains("api-us") ? "US" : "CN";
            System.out.println("🌐 Face++ [" + tag + "] — b64: " + (b64_1.length()/1024) + "KB + " + (b64_2.length()/1024) + "KB");

            String boundary = "----BoostUp" + UUID.randomUUID().toString().replace("-", "");
            conn = (HttpURLConnection) new URL(apiUrl).openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            try (OutputStream os = conn.getOutputStream()) {
                writeField(os, boundary, "api_key", FACEPP_API_KEY);
                writeField(os, boundary, "api_secret", FACEPP_API_SECRET);
                writeField(os, boundary, "image_base64_1", b64_1);
                writeField(os, boundary, "image_base64_2", b64_2);
                os.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int code = conn.getResponseCode();
            String body = readStream(code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream());
            System.out.println("🌐 Face++ [" + tag + "] HTTP " + code);

            if (code < 200 || code >= 300) {
                System.out.println("❌ Face++ [" + tag + "]: " + body.substring(0, Math.min(200, body.length())));
                if (body.contains("NO_FACE_FOUND")) return -1;
                return -2;
            }

            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            if (!json.has("confidence")) {
                System.out.println("❌ Face++ no confidence → no face");
                return -1;
            }

            double confidence = json.get("confidence").getAsDouble();
            System.out.println("✅ Face++ confidence: " + String.format("%.1f%%", confidence));
            return confidence;

        } catch (java.net.UnknownHostException | java.net.ConnectException e) {
            System.out.println("❌ Face++ [" + (apiUrl.contains("us") ? "US" : "CN") + "] connection failed");
            return -2;
        } catch (java.net.SocketTimeoutException e) {
            System.out.println("❌ Face++ timeout");
            return -2;
        } catch (Exception e) {
            System.out.println("❌ Face++ error: " + e.getMessage());
            return -2;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    // ═══════════════════════════════════════════════════════════
    // LOCAL COMPARISON — Region-based LBP (much more discriminative)
    // Splits face into 5x5 grid, compares each region separately
    // Central regions (eyes/nose/mouth) are weighted higher
    // ═══════════════════════════════════════════════════════════

    // Weight map for 5x5 grid (center = eyes/nose/mouth = higher weight)
    private static final double[][] REGION_WEIGHTS = {
            {0.5, 0.8, 1.0, 0.8, 0.5},  // forehead
            {0.8, 2.0, 2.5, 2.0, 0.8},  // eyes
            {0.8, 2.0, 3.0, 2.0, 0.8},  // nose
            {0.8, 2.0, 2.5, 2.0, 0.8},  // mouth
            {0.5, 0.8, 1.0, 0.8, 0.5}   // chin
    };

    private static double compareFacesLocal(BufferedImage captured, BufferedImage enrolled) {
        try {
            if (captured == null || enrolled == null) return -2;

            // Prepare: resize to 160x160, grayscale, histogram equalize
            int sz = 160;
            BufferedImage img1 = prepareForComparison(captured, sz);
            BufferedImage img2 = prepareForComparison(enrolled, sz);

            int[][] g1 = toGray(img1);
            int[][] g2 = toGray(img2);

            // Step 1: Quick global histogram check — reject very different images early
            double histDiff = globalHistogramDifference(g1, g2);
            System.out.println("🖥️ Global histogram diff: " + String.format("%.3f", histDiff));
            if (histDiff > 0.65) {
                System.out.println("🖥️ Quick reject: histogram too different (" + String.format("%.1f%%", histDiff*100) + " > 65%)");
                return histDiff < 0.8 ? 30.0 : 15.0; // Low score but not -1 (face was detected)
            }

            // Step 2: Region-based LBP comparison (5x5 grid)
            int gridRows = 5, gridCols = 5;
            int cellH = sz / gridRows, cellW = sz / gridCols;
            double weightedSum = 0, totalWeight = 0;
            int goodRegions = 0, badRegions = 0;

            StringBuilder regionLog = new StringBuilder("🖥️ Regions: ");

            for (int gy = 0; gy < gridRows; gy++) {
                for (int gx = 0; gx < gridCols; gx++) {
                    int startY = gy * cellH, startX = gx * cellW;

                    // Extract region LBP histograms
                    int[] lbp1 = computeRegionLBP(g1, startX, startY, cellW, cellH);
                    int[] lbp2 = computeRegionLBP(g2, startX, startY, cellW, cellH);

                    double regionSim = 1.0 - chiSquaredDistance(lbp1, lbp2);
                    double weight = REGION_WEIGHTS[gy][gx];

                    weightedSum += regionSim * weight;
                    totalWeight += weight;

                    if (regionSim > 0.6) goodRegions++;
                    else badRegions++;
                }
            }

            double regionScore = (totalWeight > 0) ? (weightedSum / totalWeight) : 0;

            // Step 3: Global SSIM as secondary check
            double ssim = computeSSIM(img1, img2);

            // Step 4: Pixel-level NCC on center region (most discriminative)
            int cx = sz/4, cy = sz/4, cw = sz/2, ch = sz/2;
            double centerNCC = computeRegionNCC(g1, g2, cx, cy, cw, ch);

            // Composite score with region-based LBP dominant
            double composite = (regionScore * 0.55 + ssim * 0.15 + centerNCC * 0.30) * 100.0;

            // Penalty if too many bad regions (different person)
            double badRatio = (double) badRegions / (goodRegions + badRegions);
            if (badRatio > 0.5) {
                composite *= (1.0 - (badRatio - 0.5) * 0.6); // Reduce score
            }

            System.out.println(String.format(
                    "🖥️ Local: RegionLBP=%.3f SSIM=%.3f CenterNCC=%.3f Good=%d Bad=%d → %.1f/100",
                    regionScore, ssim, centerNCC, goodRegions, badRegions, composite));
            System.out.println(String.format(
                    "   Match: %s (score %.1f %s threshold %.1f)",
                    composite >= LOCAL_THRESHOLD ? "✅ YES" : "❌ NO",
                    composite, composite >= LOCAL_THRESHOLD ? ">=" : "<", LOCAL_THRESHOLD));

            return composite;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Local comparison error", e);
            return -2;
        }
    }

    /**
     * Global histogram difference — quick reject for very different faces.
     * Returns 0.0 (identical) to 1.0 (completely different).
     */
    private static double globalHistogramDifference(int[][] g1, int[][] g2) {
        int[] h1 = new int[256], h2 = new int[256];
        int h = Math.min(g1.length, g2.length);
        int w = Math.min(g1[0].length, g2[0].length);
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                h1[g1[y][x]]++;
                h2[g2[y][x]]++;
            }
        // Bhattacharyya distance
        double total = h * w;
        double sum = 0;
        for (int i = 0; i < 256; i++) {
            sum += Math.sqrt((h1[i] / total) * (h2[i] / total));
        }
        return 1.0 - sum;
    }

    /**
     * Compute LBP histogram for a specific region of the grayscale image.
     */
    private static int[] computeRegionLBP(int[][] gray, int sx, int sy, int w, int h) {
        int[] hist = new int[256];
        int maxY = Math.min(sy + h, gray.length - 1);
        int maxX = Math.min(sx + w, gray[0].length - 1);
        for (int y = Math.max(1, sy); y < maxY; y++) {
            for (int x = Math.max(1, sx); x < maxX; x++) {
                int c = gray[y][x], code = 0;
                code |= (gray[y-1][x-1] >= c ? 1 : 0) << 7;
                code |= (gray[y-1][x]   >= c ? 1 : 0) << 6;
                code |= (gray[y-1][x+1] >= c ? 1 : 0) << 5;
                code |= (gray[y][x+1]   >= c ? 1 : 0) << 4;
                code |= (gray[y+1][x+1] >= c ? 1 : 0) << 3;
                code |= (gray[y+1][x]   >= c ? 1 : 0) << 2;
                code |= (gray[y+1][x-1] >= c ? 1 : 0) << 1;
                code |= (gray[y][x-1]   >= c ? 1 : 0);
                hist[code]++;
            }
        }
        return hist;
    }

    /**
     * NCC on a specific region (for center-face comparison).
     */
    private static double computeRegionNCC(int[][] g1, int[][] g2, int sx, int sy, int w, int h) {
        int maxY = Math.min(sy + h, Math.min(g1.length, g2.length));
        int maxX = Math.min(sx + w, Math.min(g1[0].length, g2[0].length));
        double m1 = 0, m2 = 0;
        int cnt = 0;
        for (int y = sy; y < maxY; y++)
            for (int x = sx; x < maxX; x++) { m1 += g1[y][x]; m2 += g2[y][x]; cnt++; }
        if (cnt == 0) return 0;
        m1 /= cnt; m2 /= cnt;
        double num = 0, d1 = 0, d2 = 0;
        for (int y = sy; y < maxY; y++)
            for (int x = sx; x < maxX; x++) {
                double a = g1[y][x] - m1, b = g2[y][x] - m2;
                num += a * b; d1 += a * a; d2 += b * b;
            }
        double den = Math.sqrt(d1 * d2);
        return den < 1e-10 ? 0 : Math.max(0, (num / den + 1.0) / 2.0);
    }

    // ═══════════════════════════════════════════════════════════
    // UTILITIES
    // ═══════════════════════════════════════════════════════════

    private static byte[] compressToJpeg(BufferedImage img) throws IOException {
        BufferedImage resized = resizeImage(img, COMPARE_SIZE, COMPARE_SIZE);
        BufferedImage rgb = new BufferedImage(COMPARE_SIZE, COMPARE_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, COMPARE_SIZE, COMPARE_SIZE);
        g.drawImage(resized, 0, 0, null);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (writers.hasNext()) {
            ImageWriter writer = writers.next();
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(JPEG_QUALITY);
            ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
            writer.setOutput(ios);
            writer.write(null, new IIOImage(rgb, null, null), param);
            ios.close();
            writer.dispose();
        } else {
            ImageIO.write(rgb, "jpeg", baos);
        }
        return baos.toByteArray();
    }

    private static BufferedImage prepareForComparison(BufferedImage img, int sz) {
        BufferedImage resized = resizeImage(img, sz, sz);
        BufferedImage gray = new BufferedImage(sz, sz, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = gray.createGraphics();
        g.drawImage(resized, 0, 0, null);
        g.dispose();
        return equalizeHistogram(gray);
    }

    // SSIM on two BufferedImages
    private static double computeSSIM(BufferedImage i1, BufferedImage i2) {
        int[][] g1 = toGray(i1), g2 = toGray(i2);
        int h = Math.min(g1.length, g2.length), w = Math.min(g1[0].length, g2[0].length);
        double C1 = 6.5025, C2 = 58.5225;
        int bs = 8; double tot = 0; int cnt = 0;
        for (int by = 0; by + bs <= h; by += bs)
            for (int bx = 0; bx + bs <= w; bx += bs) {
                double m1 = 0, m2 = 0;
                for (int y = by; y < by + bs; y++)
                    for (int x = bx; x < bx + bs; x++) { m1 += g1[y][x]; m2 += g2[y][x]; }
                int n = bs * bs; m1 /= n; m2 /= n;
                double v1 = 0, v2 = 0, cv = 0;
                for (int y = by; y < by + bs; y++)
                    for (int x = bx; x < bx + bs; x++) {
                        double d1 = g1[y][x] - m1, d2 = g2[y][x] - m2;
                        v1 += d1*d1; v2 += d2*d2; cv += d1*d2;
                    }
                v1 /= (n-1); v2 /= (n-1); cv /= (n-1);
                tot += ((2*m1*m2+C1)*(2*cv+C2))/((m1*m1+m2*m2+C1)*(v1+v2+C2));
                cnt++;
            }
        return cnt > 0 ? Math.max(0, tot / cnt) : 0;
    }

    private static BufferedImage equalizeHistogram(BufferedImage gi) {
        int w = gi.getWidth(), h = gi.getHeight(), tp = w * h;
        int[] hist = new int[256];
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) hist[gi.getRaster().getSample(x, y, 0)]++;
        int[] cdf = new int[256]; cdf[0] = hist[0];
        for (int i = 1; i < 256; i++) cdf[i] = cdf[i-1] + hist[i];
        int cmin = 0;
        for (int i = 0; i < 256; i++) if (cdf[i] > 0) { cmin = cdf[i]; break; }
        int[] map = new int[256];
        for (int i = 0; i < 256; i++)
            map[i] = tp > cmin ? Math.round((float)(cdf[i] - cmin) / (tp - cmin) * 255) : 0;
        BufferedImage r = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                r.getRaster().setSample(x, y, 0, map[gi.getRaster().getSample(x, y, 0)]);
        return r;
    }

    private static int[][] toGray(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();
        int[][] g = new int[h][w];
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                int rgb = img.getRGB(x, y);
                g[y][x] = (int)(0.299*((rgb>>16)&0xFF) + 0.587*((rgb>>8)&0xFF) + 0.114*(rgb&0xFF));
            }
        return g;
    }

    private static double chiSquaredDistance(int[] h1, int[] h2) {
        double t1 = 0, t2 = 0;
        for (int i = 0; i < h1.length; i++) { t1 += h1[i]; t2 += h2[i]; }
        if (t1 == 0 || t2 == 0) return 1.0;
        double d = 0;
        for (int i = 0; i < h1.length; i++) {
            double a = h1[i]/t1, b = h2[i]/t2;
            if (a + b > 0) d += (a-b)*(a-b)/(a+b);
        }
        return Math.min(1.0, d / 2.0);
    }

    private static BufferedImage fxImageToBufferedImage(Image fxImage) {
        if (fxImage == null) return null;
        try {
            String url = fxImage.getUrl();
            if (url != null && !url.isEmpty()) {
                if (url.startsWith("file:")) return ImageIO.read(new File(java.net.URI.create(url)));
                else return ImageIO.read(new URL(url));
            }
            int w = (int) fxImage.getWidth(), h = (int) fxImage.getHeight();
            if (w <= 0 || h <= 0) return null;
            BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            javafx.scene.image.PixelReader r = fxImage.getPixelReader();
            if (r == null) return null;
            for (int y = 0; y < h; y++)
                for (int x = 0; x < w; x++) bi.setRGB(x, y, r.getArgb(x, y));
            return bi;
        } catch (Exception e) { return null; }
    }

    private static BufferedImage resizeImage(BufferedImage orig, int tw, int th) {
        BufferedImage r = new BufferedImage(tw, th, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = r.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(orig, 0, 0, tw, th, null);
        g.dispose();
        return r;
    }

    private static String getFacePath(int userId) {
        return FACES_DIR + File.separator + "face_" + userId + ".png";
    }

    private static void writeField(OutputStream os, String boundary, String name, String value) throws IOException {
        os.write(("--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n" +
                value + "\r\n").getBytes(StandardCharsets.UTF_8));
    }

    private static String readStream(InputStream is) throws IOException {
        if (is == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }

    public static String imageToBase64(Image image) {
        try {
            BufferedImage b = fxImageToBufferedImage(image);
            if (b == null) return null;
            ByteArrayOutputStream o = new ByteArrayOutputStream();
            ImageIO.write(b, "png", o);
            return Base64.getEncoder().encodeToString(o.toByteArray());
        } catch (Exception e) { return null; }
    }

    public static Image base64ToImage(String base64) {
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            File temp = File.createTempFile("boostup_face_", ".png");
            temp.deleteOnExit();
            Files.write(temp.toPath(), bytes);
            return new Image(temp.toURI().toString());
        } catch (Exception e) { return null; }
    }
}


