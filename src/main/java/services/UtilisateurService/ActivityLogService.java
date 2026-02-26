package services.UtilisateurService;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service de journalisation des activités utilisateur
 * Enregistre les actions importantes (connexion, déconnexion, modifications, etc.)
 */
public class ActivityLogService {

    private static final Logger LOGGER = Logger.getLogger(ActivityLogService.class.getName());

    // Actions possibles
    public static final String ACTION_LOGIN = "LOGIN";
    public static final String ACTION_LOGOUT = "LOGOUT";
    public static final String ACTION_FACE_ENROLL = "FACE_ENROLL";
    public static final String ACTION_FACE_LOGIN = "FACE_LOGIN";
    public static final String ACTION_EXPORT_PDF = "EXPORT_PDF";
    public static final String ACTION_PROFILE_UPDATE = "PROFILE_UPDATE";
    public static final String ACTION_PASSWORD_CHANGE = "PASSWORD_CHANGE";
    public static final String ACTION_DELETE = "DELETE";

    private static final String LOG_DIR = System.getProperty("user.home") + File.separator +
                                          ".boostup" + File.separator + "logs";
    private static final String LOG_FILE = "activity.log";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    static {
        try {
            File dir = new File(LOG_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Impossible de créer le répertoire de logs", e);
        }
    }

    /**
     * Enregistre une activité utilisateur
     * @param userId ID de l'utilisateur
     * @param email Email de l'utilisateur
     * @param action Type d'action (LOGIN, LOGOUT, etc.)
     * @param details Détails supplémentaires
     */
    public static void log(int userId, String email, String action, String details) {
        try {
            String timestamp = LocalDateTime.now().format(DATE_FORMAT);
            String logEntry = String.format("[%s] USER_ID=%d | EMAIL=%s | ACTION=%s | DETAILS=%s%n",
                    timestamp, userId, email, action, details);

            File logFile = new File(LOG_DIR + File.separator + LOG_FILE);

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
                writer.write(logEntry);
            }

            LOGGER.info("Activity logged: " + action + " for user " + email);

        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Erreur lors de l'enregistrement du log", e);
        }
    }

    /**
     * Enregistre une activité sans détails
     */
    public static void log(int userId, String email, String action) {
        log(userId, email, action, "");
    }
}

