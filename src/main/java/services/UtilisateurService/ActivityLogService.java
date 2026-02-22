package services.UtilisateurService;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Service de journalisation des activités utilisateur.
 * Stocke les logs dans un fichier local (pas de table SQL).
 * Format CSV : timestamp|userId|email|actionType|description|device
 */
public class ActivityLogService {

    private static final Logger LOGGER = Logger.getLogger(ActivityLogService.class.getName());

    private static final String LOG_DIR = System.getProperty("user.home") + File.separator +
            ".boostup" + File.separator + "logs";
    private static final String LOG_FILE = LOG_DIR + File.separator + "activity.log";
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    // Types d'actions
    public static final String ACTION_LOGIN = "LOGIN";
    public static final String ACTION_LOGOUT = "LOGOUT";
    public static final String ACTION_LOGIN_FAILED = "LOGIN_FAILED";
    public static final String ACTION_SIGNUP = "SIGNUP";
    public static final String ACTION_PROFILE_UPDATE = "PROFILE_UPDATE";
    public static final String ACTION_PASSWORD_CHANGE = "PASSWORD_CHANGE";
    public static final String ACTION_PASSWORD_RESET = "PASSWORD_RESET";
    public static final String ACTION_USER_BLOCKED = "USER_BLOCKED";
    public static final String ACTION_USER_UNBLOCKED = "USER_UNBLOCKED";
    public static final String ACTION_ROLE_CHANGE = "ROLE_CHANGE";
    public static final String ACTION_USER_DELETED = "USER_DELETED";
    public static final String ACTION_USER_CREATED = "USER_CREATED";
    public static final String ACTION_EXPORT_PDF = "EXPORT_PDF";
    public static final String ACTION_FACE_ENROLL = "FACE_ENROLL";
    public static final String ACTION_FACE_AUTH = "FACE_AUTH";
    public static final String ACTION_FACE_AUTH_FAILED = "FACE_AUTH_FAILED";
    public static final String ACTION_SMS_SENT = "SMS_SENT";
    public static final String ACTION_OTP_VERIFIED = "OTP_VERIFIED";
    public static final String ACTION_OTP_FAILED = "OTP_FAILED";

    static {
        try {
            Files.createDirectories(Paths.get(LOG_DIR));
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Cannot create log directory", e);
        }
    }

    /**
     * Enregistre une activité dans le fichier log.
     */
    public static void log(int userId, String email, String actionType, String description) {
        try {
            String timestamp = SDF.format(new Date());
            String device = getDeviceInfo();
            // Escape pipes in description
            String safeDesc = description != null ? description.replace("|", " ") : "";
            String line = String.join("|", timestamp, String.valueOf(userId),
                    email != null ? email : "", actionType, safeDesc, device);

            // Append to file
            Files.write(Paths.get(LOG_FILE), (line + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);

            LOGGER.info("📝 [" + actionType + "] " + description + " (User: " + email + ")");
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error writing activity log", e);
        }
    }

    public static void logAnonymous(String email, String actionType, String description) {
        log(0, email, actionType, description);
    }

    /**
     * Récupère les N dernières activités depuis le fichier.
     */
    public static List<Map<String, Object>> getRecentActivities(int limit) {
        List<Map<String, Object>> all = readAllLogs();
        // Most recent first (file appends at end, so reverse)
        Collections.reverse(all);
        return all.stream().limit(limit).collect(Collectors.toList());
    }

    public static List<Map<String, Object>> getUserActivities(int userId, int limit) {
        return readAllLogs().stream()
                .sorted((a, b) -> ((String) b.get("created_at")).compareTo((String) a.get("created_at")))
                .filter(m -> Integer.parseInt(m.get("user_id").toString()) == userId)
                .limit(limit)
                .collect(Collectors.toList());
    }

    public static List<Map<String, Object>> getActivitiesByType(String actionType, int limit) {
        return readAllLogs().stream()
                .sorted((a, b) -> ((String) b.get("created_at")).compareTo((String) a.get("created_at")))
                .filter(m -> actionType.equals(m.get("action_type")))
                .limit(limit)
                .collect(Collectors.toList());
    }

    public static Map<String, Integer> getActivityStats() {
        Map<String, Integer> stats = new LinkedHashMap<>();
        for (Map<String, Object> entry : readAllLogs()) {
            String type = (String) entry.get("action_type");
            stats.merge(type, 1, Integer::sum);
        }
        return stats;
    }

    public static int getLoginCountLast24h() {
        long cutoff = System.currentTimeMillis() - 24 * 60 * 60 * 1000;
        return (int) readAllLogs().stream()
                .filter(m -> "LOGIN".equals(m.get("action_type")))
                .filter(m -> parseTimestamp((String) m.get("created_at")) >= cutoff)
                .count();
    }

    public static int getFailedLoginCountLast24h() {
        long cutoff = System.currentTimeMillis() - 24 * 60 * 60 * 1000;
        return (int) readAllLogs().stream()
                .filter(m -> "LOGIN_FAILED".equals(m.get("action_type")))
                .filter(m -> parseTimestamp((String) m.get("created_at")) >= cutoff)
                .count();
    }

    public static Map<String, Integer> getSignupsByDay(int days) {
        // Simplified — return empty for file-based
        return new LinkedHashMap<>();
    }

    public static int purgeOldLogs(int daysToKeep) {
        long cutoff = System.currentTimeMillis() - (long) daysToKeep * 24 * 60 * 60 * 1000;
        List<Map<String, Object>> all = readAllLogs();
        List<Map<String, Object>> kept = all.stream()
                .filter(m -> parseTimestamp((String) m.get("created_at")) >= cutoff)
                .collect(Collectors.toList());
        int deleted = all.size() - kept.size();
        if (deleted > 0) {
            writeAllLogs(kept);
            LOGGER.info("🧹 Purged " + deleted + " old log entries");
        }
        return deleted;
    }

    // ═══════════════════════════════════════════════════════
    // FORMATTING HELPERS
    // ═══════════════════════════════════════════════════════

    public static String getActionEmoji(String actionType) {
        if (actionType == null) return "📝";
        switch (actionType) {
            case ACTION_LOGIN: return "🔓";
            case ACTION_LOGOUT: return "🚪";
            case ACTION_LOGIN_FAILED: return "🚫";
            case ACTION_SIGNUP: return "🎉";
            case ACTION_PROFILE_UPDATE: return "✏️";
            case ACTION_PASSWORD_CHANGE: return "🔑";
            case ACTION_PASSWORD_RESET: return "🔄";
            case ACTION_USER_BLOCKED: return "🔒";
            case ACTION_USER_UNBLOCKED: return "🔓";
            case ACTION_ROLE_CHANGE: return "🎭";
            case ACTION_USER_DELETED: return "🗑️";
            case ACTION_USER_CREATED: return "👤";
            case ACTION_EXPORT_PDF: return "📄";
            case ACTION_FACE_ENROLL: return "📸";
            case ACTION_FACE_AUTH: return "🔐";
            case ACTION_FACE_AUTH_FAILED: return "❌";
            case ACTION_SMS_SENT: return "📱";
            case ACTION_OTP_VERIFIED: return "✅";
            case ACTION_OTP_FAILED: return "⛔";
            default: return "📝";
        }
    }

    public static String formatActionDescription(String actionType) {
        if (actionType == null) return "";
        switch (actionType) {
            case ACTION_LOGIN: return "Connexion réussie";
            case ACTION_LOGOUT: return "Déconnexion";
            case ACTION_LOGIN_FAILED: return "Tentative échouée";
            case ACTION_SIGNUP: return "Inscription";
            case ACTION_PROFILE_UPDATE: return "Mise à jour profil";
            case ACTION_PASSWORD_CHANGE: return "Changement MDP";
            case ACTION_PASSWORD_RESET: return "Réinitialisation MDP";
            case ACTION_USER_BLOCKED: return "Compte bloqué";
            case ACTION_USER_UNBLOCKED: return "Compte débloqué";
            case ACTION_ROLE_CHANGE: return "Changement de rôle";
            case ACTION_USER_DELETED: return "Utilisateur supprimé";
            case ACTION_USER_CREATED: return "Utilisateur créé";
            case ACTION_EXPORT_PDF: return "Export PDF";
            case ACTION_FACE_ENROLL: return "Enregistrement facial";
            case ACTION_FACE_AUTH: return "Auth. faciale";
            case ACTION_FACE_AUTH_FAILED: return "Échec auth. faciale";
            case ACTION_SMS_SENT: return "SMS envoyé";
            case ACTION_OTP_VERIFIED: return "OTP vérifié";
            case ACTION_OTP_FAILED: return "OTP incorrect";
            default: return actionType;
        }
    }

    // ═══════════════════════════════════════════════════════
    // FILE I/O
    // ═══════════════════════════════════════════════════════

    private static List<Map<String, Object>> readAllLogs() {
        List<Map<String, Object>> logs = new ArrayList<>();
        Path path = Paths.get(LOG_FILE);
        if (!Files.exists(path)) return logs;

        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            for (String line : lines) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split("\\|", -1);
                if (parts.length >= 5) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("created_at", parts[0]);
                    map.put("user_id", parts[1]);
                    map.put("user_email", parts.length > 2 ? parts[2] : "");
                    map.put("action_type", parts.length > 3 ? parts[3] : "");
                    map.put("description", parts.length > 4 ? parts[4] : "");
                    map.put("device_info", parts.length > 5 ? parts[5] : "");
                    logs.add(map);
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error reading log file", e);
        }
        return logs;
    }

    private static void writeAllLogs(List<Map<String, Object>> logs) {
        try {
            StringBuilder sb = new StringBuilder();
            for (Map<String, Object> m : logs) {
                sb.append(String.join("|",
                        String.valueOf(m.getOrDefault("created_at", "")),
                        String.valueOf(m.getOrDefault("user_id", "0")),
                        String.valueOf(m.getOrDefault("user_email", "")),
                        String.valueOf(m.getOrDefault("action_type", "")),
                        String.valueOf(m.getOrDefault("description", "")),
                        String.valueOf(m.getOrDefault("device_info", ""))
                )).append(System.lineSeparator());
            }
            Files.write(Paths.get(LOG_FILE), sb.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error writing log file", e);
        }
    }

    private static long parseTimestamp(String ts) {
        try { return SDF.parse(ts).getTime(); }
        catch (Exception e) { return 0; }
    }

    private static String getDeviceInfo() {
        return System.getProperty("os.name") + " " +
                System.getProperty("os.version") + " | Java " +
                System.getProperty("java.version");
    }
}
