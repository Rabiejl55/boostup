package services.UtilisateurService;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service SMS via Twilio pour l'envoi de codes OTP (2FA).
 *
 * <p><b>Configuration Twilio (compte trial gratuit) :</b></p>
 * <ol>
 *   <li>Créer un compte sur <a href="https://www.twilio.com/try-twilio">twilio.com/try-twilio</a></li>
 *   <li>Aller dans Console → Phone Numbers → "Get a Trial Phone Number" (gratuit)</li>
 *   <li>Aller dans Console → Verified Caller IDs → Ajouter votre numéro personnel</li>
 *   <li>Copier Account SID + Auth Token + Trial Phone Number ci-dessous</li>
 * </ol>
 *
 * <p><b>⚠️ Limitations du compte Trial :</b></p>
 * <ul>
 *   <li>Vous ne pouvez envoyer des SMS qu'aux numéros vérifiés (Verified Caller IDs)</li>
 *   <li>Les SMS sont préfixés par "Sent from your Twilio trial account"</li>
 *   <li>Le numéro FROM doit être votre numéro Twilio trial (PAS votre numéro personnel)</li>
 * </ul>
 */
public class SmsService {

    private static final Logger LOGGER = Logger.getLogger(SmsService.class.getName());

    // ═══════════════════════════════════════════════════════════════
    // CONFIGURATION TWILIO
    //
    // 1) Account SID et Auth Token : trouvables sur https://console.twilio.com
    // 2) TWILIO_FROM : votre numéro Twilio trial
    //    → Console → Phone Numbers → Active Numbers → copier le numéro
    //    → Si vous n'en avez pas : cliquer "Get a Trial Phone Number"
    // 3) Verified numbers : pour le trial, vous devez vérifier chaque
    //    numéro de destination dans Console → Verified Caller IDs
    // ═══════════════════════════════════════════════════════════════
    private static final String TWILIO_SID   = System.getenv("TWILIO_ACCOUNT_SID");
    private static final String TWILIO_TOKEN = System.getenv("TWILIO_ACCOUNT_SID");

    // ⚠️ REMPLACEZ CECI par votre numéro Twilio trial !
    // Trouvez-le ici : https://console.twilio.com/us1/develop/phone-numbers/manage/incoming
    // Il ressemble à : +1XXXXXXXXXX (numéro américain gratuit)
    private static final String TWILIO_FROM  = System.getenv("TWILIO_ACCOUNT_SID");;

    // ═══ Cache des codes OTP ═══
    private static final Map<String, OtpEntry> otpCache = new ConcurrentHashMap<>();
    private static final long OTP_VALIDITY_MS = 5 * 60 * 1000; // 5 minutes

    // ═══ Rate limiting — Économiser les crédits Twilio ═══
    private static final Map<String, Long> lastSmsSentTime = new ConcurrentHashMap<>();
    private static final Map<String, Integer> dailySmsCount = new ConcurrentHashMap<>();
    private static long dailyResetTimestamp = System.currentTimeMillis();
    private static final long SMS_COOLDOWN_MS = 60 * 1000; // 60 secondes entre 2 SMS
    private static final int MAX_SMS_PER_DAY = 5; // Max 5 SMS par numéro par jour

    // Dernier OTP généré (pour affichage en popup si envoi échoue)
    private static String lastGeneratedOtp = null;
    private static boolean lastSendSucceeded = false;

    // ═══════════════════════════════════════════════════════════
    // MÉTHODES PUBLIQUES
    // ═══════════════════════════════════════════════════════════

    /**
     * Génère et envoie un code OTP par SMS via Twilio.
     *
     * @param phoneNumber Numéro du destinataire (format international : +216XXXXXXXX)
     * @return Le code OTP généré
     */
    public static String sendOtp(String phoneNumber) {
        // ═══ Réutiliser un OTP existant non expiré (économie de SMS) ═══
        OtpEntry existing = otpCache.get(phoneNumber);
        if (existing != null && (System.currentTimeMillis() - existing.timestamp) < OTP_VALIDITY_MS) {
            long ageSeconds = (System.currentTimeMillis() - existing.timestamp) / 1000;
            LOGGER.info("♻️ OTP existant encore valide (" + ageSeconds + "s) pour " + phoneNumber + " — pas de nouvel envoi");
            System.out.println("♻️ OTP réutilisé (encore valide " + (300 - ageSeconds) + "s) — SMS économisé !");
            lastSendSucceeded = true;
            lastGeneratedOtp = existing.code;
            return existing.code;
        }

        // ═══ Cooldown : 60 secondes entre 2 SMS au même numéro ═══
        Long lastSent = lastSmsSentTime.get(phoneNumber);
        if (lastSent != null && (System.currentTimeMillis() - lastSent) < SMS_COOLDOWN_MS) {
            long waitSec = (SMS_COOLDOWN_MS - (System.currentTimeMillis() - lastSent)) / 1000;
            LOGGER.warning("⏳ Cooldown actif — attendez " + waitSec + "s avant de renvoyer un SMS à " + phoneNumber);
            System.out.println("⏳ Cooldown SMS: attendez " + waitSec + " secondes");
            // Return the existing OTP from cache if available
            if (existing != null) {
                lastGeneratedOtp = existing.code;
                lastSendSucceeded = false;
                return existing.code;
            }
        }

        // ═══ Limite quotidienne : max 5 SMS par numéro par jour ═══
        resetDailyCounterIfNeeded();
        int count = dailySmsCount.getOrDefault(phoneNumber, 0);
        if (count >= MAX_SMS_PER_DAY) {
            LOGGER.warning("🚫 Limite quotidienne atteinte (" + MAX_SMS_PER_DAY + " SMS/jour) pour " + phoneNumber);
            System.out.println("🚫 Limite atteinte: " + MAX_SMS_PER_DAY + " SMS/jour max pour " + phoneNumber);
            String otp = generateOtp();
            lastGeneratedOtp = otp;
            otpCache.put(phoneNumber, new OtpEntry(otp, System.currentTimeMillis()));
            lastSendSucceeded = false;
            return otp;
        }

        // ═══ Générer et envoyer le nouveau OTP ═══
        String otp = generateOtp();
        lastGeneratedOtp = otp;
        otpCache.put(phoneNumber, new OtpEntry(otp, System.currentTimeMillis()));

        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║  📱 OTP CODE: " + otp + "                 ║");
        System.out.println("║  📞 TO: " + phoneNumber + "           ║");
        System.out.println("║  📊 SMS aujourd'hui: " + (count + 1) + "/" + MAX_SMS_PER_DAY + "            ║");
        System.out.println("╚══════════════════════════════════════╝");

        String body = "BoostUp - Votre code de verification : " + otp +
                " - Ce code expire dans 5 minutes.";

        lastSendSucceeded = sendViaTwilio(phoneNumber, body);

        if (lastSendSucceeded) {
            lastSmsSentTime.put(phoneNumber, System.currentTimeMillis());
            dailySmsCount.put(phoneNumber, count + 1);
            LOGGER.info("✅ SMS OTP envoyé avec succès à " + phoneNumber + " (" + (count + 1) + "/" + MAX_SMS_PER_DAY + " aujourd'hui)");
        } else {
            LOGGER.warning("⚠️ SMS non livré à " + phoneNumber + " — Code OTP: " + otp +
                    " — Le code sera affiché dans une popup.");
        }
        return otp;
    }

    private static void resetDailyCounterIfNeeded() {
        long now = System.currentTimeMillis();
        // Reset every 24h
        if (now - dailyResetTimestamp > 24 * 60 * 60 * 1000) {
            dailySmsCount.clear();
            dailyResetTimestamp = now;
            LOGGER.info("📊 Compteurs SMS quotidiens réinitialisés");
        }
    }

    /**
     * Vérifie si un code OTP est valide.
     */
    public static boolean verifyOtp(String phoneNumber, String code) {
        OtpEntry entry = otpCache.get(phoneNumber);

        if (entry == null) {
            LOGGER.warning("Aucun code OTP trouvé pour " + phoneNumber);
            return false;
        }

        if (System.currentTimeMillis() - entry.timestamp > OTP_VALIDITY_MS) {
            otpCache.remove(phoneNumber);
            LOGGER.warning("Code OTP expiré pour " + phoneNumber);
            return false;
        }

        boolean valid = entry.code.equals(code);
        if (valid) {
            otpCache.remove(phoneNumber);
            LOGGER.info("Code OTP vérifié avec succès pour " + phoneNumber);
        } else {
            LOGGER.warning("Code OTP incorrect pour " + phoneNumber);
        }
        return valid;
    }

    /**
     * Envoie un SMS de bienvenue.
     */
    public static boolean sendWelcomeSms(String phoneNumber, String userName) {
        return sendViaTwilio(phoneNumber,
                "Bienvenue sur BoostUp, " + userName + " ! Votre compte a ete cree. - L'equipe BoostUp");
    }

    /**
     * Envoie une notification SMS.
     */
    public static boolean sendNotification(String phoneNumber, String subject, String body) {
        return sendViaTwilio(phoneNumber, subject + " - " + body + " - BoostUp");
    }

    /**
     * Envoie une alerte de sécurité.
     */
    public static boolean sendSecurityAlert(String phoneNumber) {
        return sendViaTwilio(phoneNumber,
                "ALERTE SECURITE BoostUp : Tentative de connexion suspecte detectee. " +
                        "Si ce n'est pas vous, changez votre mot de passe.");
    }

    /**
     * Formate un numéro au format international.
     * Exemples : "23919944" → "+21623919944", "0655123456" → "+216655123456"
     */
    public static String formatPhoneNumber(String phone, String defaultCountryCode) {
        if (phone == null || phone.trim().isEmpty()) return null;
        String cleaned = phone.replaceAll("[\\s.-]", "");
        if (cleaned.startsWith("+")) return cleaned;
        if (cleaned.startsWith("00")) return "+" + cleaned.substring(2);
        if (cleaned.startsWith("0")) return defaultCountryCode + cleaned.substring(1);
        return defaultCountryCode + cleaned;
    }

    /**
     * Retourne le dernier OTP généré (pour affichage en popup si SMS non livré).
     */
    public static String getLastGeneratedOtp() {
        return lastGeneratedOtp;
    }

    /**
     * Vérifie si le dernier envoi SMS a réellement réussi.
     */
    public static boolean didLastSendSucceed() {
        return lastSendSucceeded;
    }

    /**
     * Vérifie si Twilio est correctement configuré.
     */
    public static boolean isConfigured() {
        return !TWILIO_FROM.startsWith("REPLACE");
    }

    // ═══════════════════════════════════════════════════════════
    // ENVOI VIA TWILIO REST API
    // ═══════════════════════════════════════════════════════════

    private static boolean sendViaTwilio(String to, String body) {
        System.out.println("══════════════════════════════════════");
        System.out.println("📱 TWILIO SMS — Sending to: " + to);
        System.out.println("   FROM: " + TWILIO_FROM);
        System.out.println("   SID: " + TWILIO_SID.substring(0, 8) + "...");
        System.out.println("══════════════════════════════════════");

        // Vérifier la configuration
        if (!isConfigured()) {
            LOGGER.warning("⚠️ Twilio non configuré ! TWILIO_FROM doit être votre numéro Twilio trial.\n" +
                    "   → Allez sur https://console.twilio.com/us1/develop/phone-numbers/manage/incoming\n" +
                    "   → Copiez votre numéro trial et collez-le dans SmsService.TWILIO_FROM");
            return false;
        }

        try {
            String urlStr = "https://api.twilio.com/2010-04-01/Accounts/" + TWILIO_SID + "/Messages.json";
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            // Authentification Basic
            String auth = TWILIO_SID + ":" + TWILIO_TOKEN;
            String encoded = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            conn.setRequestProperty("Authorization", "Basic " + encoded);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            // Paramètres
            String data = "To=" + encode(to) +
                    "&From=" + encode(TWILIO_FROM) +
                    "&Body=" + encode(body);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(data.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            String response = readResponse(conn);

            if (responseCode == 201 || responseCode == 200) {
                // Check if Twilio accepted but message will fail (e.g. limit exceeded)
                if (response.contains("30454") || response.contains("messages limit")
                        || response.contains("exceeded")) {
                    LOGGER.warning("⚠️ SMS accepté mais limite Twilio atteinte ! [HTTP " + responseCode + "]");
                    System.out.println("⚠️ TWILIO: Limite de messages atteinte (erreur 30454)");
                    System.out.println("   → Créez un nouveau compte trial ou rechargez votre compte");
                    System.out.println("   → https://www.twilio.com/try-twilio");
                    return false;
                }
                LOGGER.info("✅ SMS Twilio envoyé à " + to + " [HTTP " + responseCode + "]");
                System.out.println("✅ SMS envoyé avec succès à " + to);
                return true;
            } else {
                LOGGER.warning("❌ Twilio erreur [HTTP " + responseCode + "]:\n" + response);
                System.out.println("❌ TWILIO ERREUR HTTP " + responseCode);
                System.out.println("   TO: " + to);
                System.out.println("   FROM: " + TWILIO_FROM);
                System.out.println("   RESPONSE: " + response);
                logTwilioHelp(responseCode, response);
                return false;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ Exception Twilio", e);
            return false;
        }
    }

    /**
     * Affiche des messages d'aide selon l'erreur Twilio rencontrée.
     */
    private static void logTwilioHelp(int httpCode, String response) {
        if (response.contains("21608") || response.contains("unverified")) {
            LOGGER.warning(
                    "═══════════════════════════════════════════════════════\n" +
                    "  ERREUR TWILIO 21608 : Numéro de destination non vérifié\n" +
                    "  \n" +
                    "  Avec un compte TRIAL, vous devez vérifier chaque\n" +
                    "  numéro de destination :\n" +
                    "  1) Allez sur : https://console.twilio.com/us1/develop/phone-numbers/manage/verified\n" +
                    "  2) Cliquez 'Add a new Caller ID'\n" +
                    "  3) Entrez le numéro du destinataire (ex: +21623919944)\n" +
                    "  4) Vous recevrez un appel/SMS de vérification\n" +
                    "═══════════════════════════════════════════════════════"
            );
        } else if (response.contains("21212") || response.contains("is not valid")) {
            LOGGER.warning(
                    "═══════════════════════════════════════════════════════\n" +
                    "  ERREUR TWILIO 21212 : Numéro FROM invalide\n" +
                    "  \n" +
                    "  Le numéro TWILIO_FROM='" + TWILIO_FROM + "' n'est pas valide.\n" +
                    "  Vous devez utiliser votre numéro Twilio trial :\n" +
                    "  1) Allez sur : https://console.twilio.com/us1/develop/phone-numbers/manage/incoming\n" +
                    "  2) Copiez le numéro affiché (ex: +18551234567)\n" +
                    "  3) Collez-le dans SmsService.TWILIO_FROM\n" +
                    "═══════════════════════════════════════════════════════"
            );
        } else if (response.contains("21610")) {
            LOGGER.warning(
                    "═══════════════════════════════════════════════════════\n" +
                    "  ERREUR TWILIO 21610 : Message bloqué (opt-out)\n" +
                    "  Le destinataire a bloqué les messages de ce numéro.\n" +
                    "═══════════════════════════════════════════════════════"
            );
        } else if (httpCode == 401) {
            LOGGER.warning(
                    "═══════════════════════════════════════════════════════\n" +
                    "  ERREUR TWILIO 401 : Authentification échouée\n" +
                    "  Vérifiez TWILIO_SID et TWILIO_TOKEN sur :\n" +
                    "  https://console.twilio.com\n" +
                    "═══════════════════════════════════════════════════════"
            );
        }
    }

    // ═══════════════════════════════════════════════════════════
    // UTILITAIRES
    // ═══════════════════════════════════════════════════════════

    private static String generateOtp() {
        return String.valueOf(100000 + new Random().nextInt(900000));
    }

    private static String encode(String value) {
        try {
            return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return value;
        }
    }

    private static String readResponse(HttpURLConnection conn) {
        try {
            InputStream is;
            try {
                is = conn.getInputStream();
            } catch (IOException e) {
                is = conn.getErrorStream();
            }
            if (is == null) return "(no response body)";
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            return sb.toString().trim();
        } catch (Exception e) {
            return "(error reading response: " + e.getMessage() + ")";
        }
    }

    private static class OtpEntry {
        final String code;
        final long timestamp;

        OtpEntry(String code, long timestamp) {
            this.code = code;
            this.timestamp = timestamp;
        }
    }
}







