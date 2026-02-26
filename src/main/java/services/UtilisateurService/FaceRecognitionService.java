package services.UtilisateurService;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.scene.image.Image;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service de reconnaissance faciale utilisant Face++ (Megvii) Cloud AI API.
 * Utilise du deep learning (embeddings faciaux 128D) pour comparer deux visages.
 * Fallback local (LBP+SSIM+NCC) si pas d'internet.
 * API gratuite : 1000 appels/jour.
 */
public class FaceRecognitionService {

    private static final Logger LOGGER = Logger.getLogger(FaceRecognitionService.class.getName());

    private static final String FACES_DIR = System.getProperty("user.home") + File.separator +
            ".boostup" + File.separator + "faces";

    // ═══ Face++ API credentials (free tier: 1000 calls/day) ═══
    private static final String FACEPP_API_KEY = "KHPniVH_r_w33bQKVGHgU3Fy42fJyRd_";
    private static final String FACEPP_API_SECRET = "YlX3FNzVWaGPCVRqCvHjB-EjN2VBPMQH";
    private static final String FACEPP_COMPARE_URL = "https://api-us.faceplusplus.com/facepp/v3/compare";

    // Confidence threshold (0-100). Face++ recommends:
    //   1e-3 (~62) = standard security (good for webcam)
    //   1e-4 (~73) = high security
    //   1e-5 (~80) = maximum security
    private static final double MATCH_THRESHOLD = 62.0;
    private static final int FACE_SIZE = 128;

    static {
        try { Files.createDirectories(Paths.get(FACES_DIR)); }
        catch (IOException e) { LOGGER.log(Level.WARNING, "Cannot create faces directory", e); }
    }

    // ═══════════════════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════════════════

    public static boolean enrollFace(int userId, Image image) {
        try {
            BufferedImage bi = fxImageToBufferedImage(image);
            if (bi == null) return false;
            // Save at 512x512 for better Face++ recognition
            ImageIO.write(resizeImage(bi, 512, 512), "png", new File(getFaceImagePath(userId)));
            LOGGER.info("Face enrolled for user ID: " + userId);
            return true;
        } catch (Exception e) { LOGGER.log(Level.SEVERE, "Error enrolling face", e); return false; }
    }

    public static boolean enrollFaceFromFile(int userId, File imageFile) {
        try {
            BufferedImage original = ImageIO.read(imageFile);
            if (original == null) return false;
            // Save at 512x512 for better Face++ recognition
            ImageIO.write(resizeImage(original, 512, 512), "png", new File(getFaceImagePath(userId)));
            LOGGER.info("Face enrolled from file for user ID: " + userId);
            return true;
        } catch (Exception e) { LOGGER.log(Level.SEVERE, "Error enrolling face from file", e); return false; }
    }

    public static boolean hasFaceEnrolled(int userId) {
        File f = new File(getFaceImagePath(userId));
        return f.exists() && f.length() > 0;
    }

    /**
     * Compare a captured image file directly with the enrolled face.
     * This avoids JavaFX Image conversion issues.
     * @return 0-100 confidence, -1 = no face detected, -2 = error
     */
    public static double compareFacesFromFile(int userId, File capturedFile) {
        try {
            if (!hasFaceEnrolled(userId)) {
                System.out.println("❌ Face ID: Aucune photo enregistrée pour user " + userId);
                return -2;
            }
            if (capturedFile == null || !capturedFile.exists() || capturedFile.length() == 0) {
                System.out.println("❌ Face ID: Fichier capture invalide");
                return -2;
            }

            File enrolledFile = new File(getFaceImagePath(userId));
            System.out.println("📸 Face ID — Enrolled: " + enrolledFile.getAbsolutePath()
                    + " (" + enrolledFile.length() / 1024 + " KB)");
            System.out.println("📸 Face ID — Captured: " + capturedFile.getAbsolutePath()
                    + " (" + capturedFile.length() / 1024 + " KB)");

            // Try Face++ Cloud AI first
            double cloudScore = compareFacesCloud(enrolledFile, capturedFile);
            System.out.println("🧠 Face++ Cloud score: " + cloudScore);

            if (cloudScore >= -1) return cloudScore;

            // Fallback: local comparison
            LOGGER.warning("Face++ unavailable — local fallback");
            BufferedImage captured = ImageIO.read(capturedFile);
            BufferedImage enrolled = ImageIO.read(enrolledFile);
            double localScore = compareFacesLocal(captured, enrolled);
            System.out.println("🖥️ Local fallback score: " + localScore);
            return localScore;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error comparing faces from file", e);
            e.printStackTrace();
            return -2;
        }
    }

    /**
     * Compare via Face++ Cloud AI.
     * @return 0-100 confidence, -1 = no face detected, -2 = error
     */
    public static double compareFaces(int userId, Image capturedImage) {
        try {
            if (!hasFaceEnrolled(userId)) {
                LOGGER.warning("❌ No face enrolled for user " + userId);
                System.out.println("❌ Face ID: Aucune photo enregistrée pour user " + userId);
                return -2;
            }
            BufferedImage captured = fxImageToBufferedImage(capturedImage);
            if (captured == null) {
                LOGGER.warning("❌ Failed to convert captured image to BufferedImage");
                System.out.println("❌ Face ID: Impossible de convertir l'image capturée");
                return -2;
            }

            File enrolledFile = new File(getFaceImagePath(userId));
            System.out.println("📸 Face ID — Image enrolled: " + enrolledFile.getAbsolutePath()
                    + " (" + enrolledFile.length() / 1024 + " KB)");
            System.out.println("📸 Face ID — Image captured: " + captured.getWidth() + "x" + captured.getHeight());

            // Save captured as PNG (lossless — better for Face++ AI)
            File capturedFile = File.createTempFile("boostup_capture_", ".png");
            capturedFile.deleteOnExit();
            ImageIO.write(captured, "png", capturedFile);
            System.out.println("📸 Face ID — Captured saved: " + capturedFile.getAbsolutePath()
                    + " (" + capturedFile.length() / 1024 + " KB)");

            // Try Face++ Cloud AI first
            double cloudScore = compareFacesCloud(enrolledFile, capturedFile);
            System.out.println("🧠 Face++ Cloud score: " + cloudScore);

            if (cloudScore >= -1) return cloudScore;

            // Fallback: local comparison
            LOGGER.warning("Face++ unavailable — local fallback");
            double localScore = compareFacesLocal(captured, ImageIO.read(enrolledFile));
            System.out.println("🖥️ Local fallback score: " + localScore);
            return localScore;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error comparing faces", e);
            e.printStackTrace();
            return -2;
        }
    }

    public static boolean authenticate(int userId, Image capturedImage) {
        double score = compareFaces(userId, capturedImage);
        if (score < 0) {
            LOGGER.warning("Face auth REJECTED — " + (score == -1 ? "no face" : "error") + " — user " + userId);
            return false;
        }
        boolean success = score >= MATCH_THRESHOLD;
        LOGGER.info(String.format("Face auth %s — user %d — confidence: %.1f%% (threshold: %.1f%%)",
                success ? "SUCCESS ✅" : "FAILED ❌", userId, score, MATCH_THRESHOLD));
        return success;
    }

    public static boolean deleteFace(int userId) {
        try { File f = new File(getFaceImagePath(userId)); return !f.exists() || f.delete(); }
        catch (Exception e) { return false; }
    }

    public static Image getEnrolledFaceImage(int userId) {
        try { File f = new File(getFaceImagePath(userId)); if (f.exists()) return new Image(f.toURI().toString()); }
        catch (Exception e) { LOGGER.log(Level.WARNING, "Cannot load face image", e); }
        return null;
    }

    // ═══════════════════════════════════════════════════════════
    // FACE++ CLOUD AI — uses java.net.HttpURLConnection (no external lib)
    // ═══════════════════════════════════════════════════════════

    private static double compareFacesCloud(File image1, File image2) {
        HttpURLConnection conn = null;
        try {
            byte[] bytes1 = Files.readAllBytes(image1.toPath());
            byte[] bytes2 = Files.readAllBytes(image2.toPath());
            String base64_1 = Base64.getEncoder().encodeToString(bytes1);
            String base64_2 = Base64.getEncoder().encodeToString(bytes2);

            System.out.println("🌐 Face++ API — Enrolled image: " + (bytes1.length / 1024) + " KB, base64: " + (base64_1.length() / 1024) + " KB");
            System.out.println("🌐 Face++ API — Captured image: " + (bytes2.length / 1024) + " KB, base64: " + (base64_2.length() / 1024) + " KB");

            String boundary = "----BoostUp" + UUID.randomUUID().toString().replace("-", "");

            conn = (HttpURLConnection) new URL(FACEPP_COMPARE_URL).openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            try (OutputStream os = conn.getOutputStream()) {
                writeFormField(os, boundary, "api_key", FACEPP_API_KEY);
                writeFormField(os, boundary, "api_secret", FACEPP_API_SECRET);
                writeFormField(os, boundary, "image_base64_1", base64_1);
                writeFormField(os, boundary, "image_base64_2", base64_2);
                os.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            System.out.println("🌐 Face++ API — Request sent, waiting for response...");

            int responseCode = conn.getResponseCode();
            String responseBody = readStream(
                    responseCode >= 200 && responseCode < 300 ? conn.getInputStream() : conn.getErrorStream());

            System.out.println("🌐 Face++ API — HTTP " + responseCode);
            System.out.println("🌐 Face++ API — Response: " + responseBody.substring(0, Math.min(500, responseBody.length())));

            if (responseCode < 200 || responseCode >= 300) {
                try {
                    JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
                    String errorMsg = json.has("error_message") ? json.get("error_message").getAsString() : responseBody;
                    LOGGER.warning("Face++ API error: " + errorMsg);
                    System.out.println("❌ Face++ Error: " + errorMsg);
                    if (errorMsg.contains("NO_FACE_FOUND") || errorMsg.contains("INVALID_IMAGE")) return -1;
                } catch (Exception ex) {
                    LOGGER.warning("Face++ error: " + responseBody);
                }
                return -2;
            }

            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();

            if (!json.has("confidence")) {
                LOGGER.warning("Face++ no confidence field — response: " + responseBody);
                System.out.println("❌ Face++ — Pas de champ 'confidence'. Aucun visage détecté?");
                // Check if faces1 or faces2 is empty
                if (json.has("faces1") && json.getAsJsonArray("faces1").size() == 0) {
                    System.out.println("❌ Face++ — Aucun visage dans l'image ENROLLED");
                }
                if (json.has("faces2") && json.getAsJsonArray("faces2").size() == 0) {
                    System.out.println("❌ Face++ — Aucun visage dans l'image CAPTURED");
                }
                return -1;
            }

            double confidence = json.get("confidence").getAsDouble();

            // Log Face++ recommended thresholds
            if (json.has("thresholds")) {
                JsonObject t = json.getAsJsonObject("thresholds");
                double t3 = t.has("1e-3") ? t.get("1e-3").getAsDouble() : 0;
                double t4 = t.has("1e-4") ? t.get("1e-4").getAsDouble() : 0;
                double t5 = t.has("1e-5") ? t.get("1e-5").getAsDouble() : 0;
                System.out.println(String.format("🧠 Face++ thresholds: 1e-3=%.1f, 1e-4=%.1f, 1e-5=%.1f", t3, t4, t5));
                System.out.println(String.format("🧠 Face++ confidence: %.2f%% | Match (>=73): %s",
                        confidence, confidence >= 73.0 ? "✅ YES" : "❌ NO"));
            }

            LOGGER.info(String.format("Face++ AI confidence: %.2f%%", confidence));
            return confidence;

        } catch (java.net.UnknownHostException | java.net.ConnectException e) {
            LOGGER.warning("No internet — Face++ unavailable");
            return -2;
        } catch (java.net.SocketTimeoutException e) {
            LOGGER.warning("Face++ timeout");
            return -2;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Face++ API call failed", e);
            return -2;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static void writeFormField(OutputStream os, String boundary, String name, String value) throws IOException {
        String field = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n"
                + value + "\r\n";
        os.write(field.getBytes(StandardCharsets.UTF_8));
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

    // ═══════════════════════════════════════════════════════════
    // LOCAL FALLBACK (no internet) — scaled to 0-100
    // ═══════════════════════════════════════════════════════════

    private static double compareFacesLocal(BufferedImage captured, BufferedImage enrolled) {
        try {
            if (captured == null || enrolled == null) return -2;
            BufferedImage img1 = prepareForComparison(captured);
            BufferedImage img2 = prepareForComparison(enrolled);
            double lbp = compareLBP(img1, img2);
            double ssim = computeSSIM(img1, img2);
            double ncc = computeNCC(img1, img2);
            double composite = ((lbp * 0.4) + (ssim * 0.3) + (ncc * 0.3)) * 100.0;
            LOGGER.info(String.format("[LOCAL] LBP:%.3f SSIM:%.3f NCC:%.3f => %.1f/100", lbp, ssim, ncc, composite));
            return composite;
        } catch (Exception e) { return -2; }
    }

    // ═══ LBP ═══
    private static double compareLBP(BufferedImage img1, BufferedImage img2) {
        return 1.0 - chiSquaredDistance(computeLBPHistogram(img1), computeLBPHistogram(img2));
    }
    private static int[] computeLBPHistogram(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();
        int[][] g = toGray(img); int[] hist = new int[256];
        for (int y=1;y<h-1;y++) for (int x=1;x<w-1;x++) {
            int c=g[y][x], code=0;
            code|=(g[y-1][x-1]>=c?1:0)<<7; code|=(g[y-1][x]>=c?1:0)<<6;
            code|=(g[y-1][x+1]>=c?1:0)<<5; code|=(g[y][x+1]>=c?1:0)<<4;
            code|=(g[y+1][x+1]>=c?1:0)<<3; code|=(g[y+1][x]>=c?1:0)<<2;
            code|=(g[y+1][x-1]>=c?1:0)<<1; code|=(g[y][x-1]>=c?1:0);
            hist[code]++;
        }
        return hist;
    }

    // ═══ SSIM ═══
    private static double computeSSIM(BufferedImage i1, BufferedImage i2) {
        int[][] g1=toGray(i1), g2=toGray(i2);
        int h=Math.min(g1.length,g2.length), w=Math.min(g1[0].length,g2[0].length);
        double C1=6.5025,C2=58.5225; int bs=8; double tot=0; int cnt=0;
        for (int by=0;by+bs<=h;by+=bs) for (int bx=0;bx+bs<=w;bx+=bs) {
            double m1=0,m2=0;
            for (int y=by;y<by+bs;y++) for (int x=bx;x<bx+bs;x++) {m1+=g1[y][x];m2+=g2[y][x];}
            int n=bs*bs; m1/=n; m2/=n;
            double v1=0,v2=0,cv=0;
            for (int y=by;y<by+bs;y++) for (int x=bx;x<bx+bs;x++) {
                double d1=g1[y][x]-m1,d2=g2[y][x]-m2; v1+=d1*d1;v2+=d2*d2;cv+=d1*d2;
            }
            v1/=(n-1);v2/=(n-1);cv/=(n-1);
            tot+=((2*m1*m2+C1)*(2*cv+C2))/((m1*m1+m2*m2+C1)*(v1+v2+C2)); cnt++;
        }
        return cnt>0?Math.max(0,tot/cnt):0;
    }

    // ═══ NCC ═══
    private static double computeNCC(BufferedImage i1, BufferedImage i2) {
        int[][] g1=toGray(i1),g2=toGray(i2);
        int h=Math.min(g1.length,g2.length),w=Math.min(g1[0].length,g2[0].length);
        double m1=0,m2=0; int t=h*w;
        for (int y=0;y<h;y++) for (int x=0;x<w;x++){m1+=g1[y][x];m2+=g2[y][x];}
        m1/=t;m2/=t;
        double num=0,d1=0,d2=0;
        for (int y=0;y<h;y++) for (int x=0;x<w;x++){
            double a=g1[y][x]-m1,b=g2[y][x]-m2; num+=a*b;d1+=a*a;d2+=b*b;
        }
        double den=Math.sqrt(d1*d2);
        return den<1e-10?0:Math.max(0,(num/den+1.0)/2.0);
    }

    // ═══════════════════════════════════════════════════════════
    // UTILITIES
    // ═══════════════════════════════════════════════════════════

    private static BufferedImage prepareForComparison(BufferedImage img) {
        BufferedImage resized = resizeImage(img, FACE_SIZE, FACE_SIZE);
        BufferedImage gray = new BufferedImage(FACE_SIZE, FACE_SIZE, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = gray.createGraphics(); g.drawImage(resized, 0, 0, null); g.dispose();
        return equalizeHistogram(gray);
    }

    private static BufferedImage equalizeHistogram(BufferedImage gi) {
        int w=gi.getWidth(),h=gi.getHeight(),tp=w*h;
        int[] hist=new int[256];
        for (int y=0;y<h;y++) for (int x=0;x<w;x++) hist[gi.getRaster().getSample(x,y,0)]++;
        int[] cdf=new int[256]; cdf[0]=hist[0];
        for (int i=1;i<256;i++) cdf[i]=cdf[i-1]+hist[i];
        int cm=0; for (int i=0;i<256;i++) if(cdf[i]>0){cm=cdf[i];break;}
        int[] map=new int[256];
        for (int i=0;i<256;i++) map[i]=tp>cm?Math.round((float)(cdf[i]-cm)/(tp-cm)*255):0;
        BufferedImage r=new BufferedImage(w,h,BufferedImage.TYPE_BYTE_GRAY);
        for (int y=0;y<h;y++) for (int x=0;x<w;x++)
            r.getRaster().setSample(x,y,0,map[gi.getRaster().getSample(x,y,0)]);
        return r;
    }

    private static int[][] toGray(BufferedImage img) {
        int w=img.getWidth(),h=img.getHeight(); int[][] g=new int[h][w];
        for (int y=0;y<h;y++) for (int x=0;x<w;x++) {
            int rgb=img.getRGB(x,y);
            g[y][x]=(int)(0.299*((rgb>>16)&0xFF)+0.587*((rgb>>8)&0xFF)+0.114*(rgb&0xFF));
        }
        return g;
    }

    private static double chiSquaredDistance(int[] h1, int[] h2) {
        double t1=0,t2=0;
        for (int i=0;i<h1.length;i++){t1+=h1[i];t2+=h2[i];}
        if (t1==0||t2==0) return 1.0;
        double d=0;
        for (int i=0;i<h1.length;i++){double a=h1[i]/t1,b=h2[i]/t2; if(a+b>0) d+=(a-b)*(a-b)/(a+b);}
        return Math.min(1.0,d/2.0);
    }

    private static BufferedImage fxImageToBufferedImage(Image fxImage) {
        if (fxImage == null) return null;
        try {
            String url = fxImage.getUrl();
            if (url != null && !url.isEmpty()) {
                if (url.startsWith("file:")) return ImageIO.read(new File(java.net.URI.create(url)));
                else return ImageIO.read(new URL(url));
            }
            int w=(int)fxImage.getWidth(),h=(int)fxImage.getHeight();
            if (w<=0||h<=0) return null;
            BufferedImage bi=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
            javafx.scene.image.PixelReader r=fxImage.getPixelReader();
            if (r==null) return null;
            for (int y=0;y<h;y++) for (int x=0;x<w;x++) bi.setRGB(x,y,r.getArgb(x,y));
            return bi;
        } catch (Exception e) { LOGGER.log(Level.WARNING,"FX->BufferedImage error",e); return null; }
    }

    private static BufferedImage resizeImage(BufferedImage orig, int tw, int th) {
        BufferedImage r=new BufferedImage(tw,th,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=r.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(orig,0,0,tw,th,null); g.dispose();
        return r;
    }

    private static String getFaceImagePath(int userId) {
        return FACES_DIR + File.separator + "face_" + userId + ".png";
    }

    public static String imageToBase64(Image image) {
        try {
            BufferedImage b=fxImageToBufferedImage(image); if(b==null) return null;
            ByteArrayOutputStream o=new ByteArrayOutputStream();
            ImageIO.write(b,"png",o); return Base64.getEncoder().encodeToString(o.toByteArray());
        } catch (Exception e) { return null; }
    }

    public static Image base64ToImage(String base64) {
        try {
            byte[] bytes=Base64.getDecoder().decode(base64);
            File temp=File.createTempFile("boostup_face_",".png"); temp.deleteOnExit();
            Files.write(temp.toPath(),bytes); return new Image(temp.toURI().toString());
        } catch (Exception e) { return null; }
    }
}

