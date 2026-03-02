package services.UtilisateurService;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PasswordResetCache {
    private static final Map<String, ResetTokenInfo> resetTokens = new ConcurrentHashMap<>();
    private static final long TOKEN_EXPIRY_MINUTES = 15;

    public static class ResetTokenInfo {
        public final String email;
        public final long timestamp;
        public boolean used;
        public String resetCode;
        public boolean codeVerified = false;

        public ResetTokenInfo(String email, String resetCode) {
            this.email = email;
            this.timestamp = System.currentTimeMillis();
            this.used = false;
            this.resetCode = resetCode;
        }

        public boolean isExpired() {
            long diffMinutes = (System.currentTimeMillis() - timestamp) / (1000 * 60);
            return diffMinutes > TOKEN_EXPIRY_MINUTES;
        }
    }

    public static String generateToken(String email, String resetCode) {
        String token = UUID.randomUUID().toString();
        resetTokens.put(token, new ResetTokenInfo(email, resetCode));
        cleanupExpiredTokens();
        return token;
    }

    public static boolean verifyCode(String token, String enteredCode) {
        ResetTokenInfo info = resetTokens.get(token);
        if (info == null || info.used || info.isExpired()) {
            return false;
        }

        boolean valid = info.resetCode.equals(enteredCode);
        if (valid) {
            info.codeVerified = true;
        }
        return valid;
    }

    public static boolean isCodeVerified(String token) {
        ResetTokenInfo info = resetTokens.get(token);
        return info != null && info.codeVerified && !info.used && !info.isExpired();
    }

    public static void markTokenAsUsed(String token) {
        ResetTokenInfo info = resetTokens.get(token);
        if (info != null) {
            info.used = true;
        }
    }

    public static String getEmailFromToken(String token) {
        ResetTokenInfo info = resetTokens.get(token);
        return info != null ? info.email : null;
    }

    private static void cleanupExpiredTokens() {
        resetTokens.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
}