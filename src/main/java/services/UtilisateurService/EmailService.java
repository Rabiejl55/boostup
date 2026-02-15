package services.UtilisateurService;

import entities.GUtilisateurs.User;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EmailService {

    private static final Logger LOGGER = Logger.getLogger(EmailService.class.getName());

    // Configuration Gmail SMTP - À MODIFIER AVEC VOS IDENTIFIANTS
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";
    private static final String USERNAME = "jlassirabie9@gmail.com"; // REMPLACEZ PAR VOTRE EMAIL
    private static final String PASSWORD = "eyfe pjyk dkqh wvup"; // REMPLACEZ PAR VOTRE MOT DE PASSE D'APPLICATION

    /**
     * Envoie un email avec un code de vérification (version HTML)
     */
    public static boolean sendVerificationCode(String toEmail, String verificationCode) {
        Properties properties = getMailProperties();

        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(USERNAME, PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(USERNAME));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("🔐 Code de vérification - BoostUp");

            String htmlContent = buildVerificationEmailHTML(verificationCode);

            Multipart multipart = new MimeMultipart();

            // Partie texte (fallback)
            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setText("Votre code de vérification est : " + verificationCode);

            // Partie HTML
            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(htmlContent, "text/html; charset=utf-8");

            multipart.addBodyPart(textPart);
            multipart.addBodyPart(htmlPart);

            message.setContent(multipart);

            Transport.send(message);
            LOGGER.info("✅ Email envoyé avec succès à " + toEmail + " - Code: " + verificationCode);
            return true;

        } catch (MessagingException e) {
            LOGGER.log(Level.SEVERE, "❌ Erreur envoi email: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Envoie un email avec un code de vérification (version texte simple)
     */
    public static boolean sendVerificationCodeSimple(String toEmail, String verificationCode) {
        Properties properties = getMailProperties();

        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(USERNAME, PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(USERNAME));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("🔐 Code de vérification - BoostUp");

            String content = "Bonjour,\n\n" +
                    "Votre code de vérification est : " + verificationCode + "\n\n" +
                    "Ce code expirera dans 15 minutes.\n\n" +
                    "Cordialement,\nL'équipe BoostUp";

            message.setText(content);

            Transport.send(message);
            LOGGER.info("✅ Email simple envoyé à " + toEmail + " - Code: " + verificationCode);
            return true;

        } catch (MessagingException e) {
            LOGGER.log(Level.SEVERE, "❌ Erreur envoi email: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Envoie un email de confirmation d'inscription
     */
    public static boolean sendWelcomeEmail(User user) {
        Properties properties = getMailProperties();

        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(USERNAME, PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(USERNAME));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(user.getEmail()));
            message.setSubject("🎉 Bienvenue sur BoostUp !");

            String displayName = user.getDisplayName();
            String htmlContent = buildWelcomeEmailHTML(displayName);

            Multipart multipart = new MimeMultipart();

            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setText("Bienvenue sur BoostUp !");

            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(htmlContent, "text/html; charset=utf-8");

            multipart.addBodyPart(textPart);
            multipart.addBodyPart(htmlPart);

            message.setContent(multipart);

            Transport.send(message);
            LOGGER.info("✅ Email de bienvenue envoyé à " + user.getEmail());
            return true;

        } catch (MessagingException e) {
            LOGGER.log(Level.SEVERE, "❌ Erreur envoi email bienvenue: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Envoie un email de notification de blocage
     */
    public static boolean sendBlockNotification(String toEmail, int minutes) {
        Properties properties = getMailProperties();

        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(USERNAME, PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(USERNAME));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("⚠️ Compte temporairement bloqué - BoostUp");

            String htmlContent = buildBlockEmailHTML(minutes);

            Multipart multipart = new MimeMultipart();

            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setText("Votre compte est bloqué pour " + minutes + " minutes.");

            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(htmlContent, "text/html; charset=utf-8");

            multipart.addBodyPart(textPart);
            multipart.addBodyPart(htmlPart);

            message.setContent(multipart);

            Transport.send(message);
            LOGGER.info("✅ Notification de blocage envoyée à " + toEmail);
            return true;

        } catch (MessagingException e) {
            LOGGER.log(Level.SEVERE, "❌ Erreur envoi notification blocage: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Construit les propriétés de connexion SMTP
     */
    private static Properties getMailProperties() {
        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", SMTP_HOST);
        properties.put("mail.smtp.port", SMTP_PORT);
        properties.put("mail.smtp.ssl.trust", SMTP_HOST);
        properties.put("mail.debug", "false"); // Mettre à "true" pour debug
        return properties;
    }

    /**
     * Construit le HTML pour l'email de vérification
     */
    private static String buildVerificationEmailHTML(String code) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<style>" +
                "body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 0; }" +
                ".container { max-width: 600px; margin: 20px auto; background: white; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }" +
                ".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }" +
                ".content { padding: 40px; text-align: center; }" +
                ".code { font-size: 48px; font-weight: bold; color: #667eea; letter-spacing: 10px; background: #f8f9fa; padding: 20px; border-radius: 10px; margin: 20px 0; font-family: monospace; }" +
                ".footer { padding: 20px; text-align: center; color: #666; font-size: 12px; border-top: 1px solid #eee; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='header'>" +
                "<h1 style='margin:0;'>🚀 BoostUp</h1>" +
                "<p style='opacity:0.9; margin-top:10px;'>Code de vérification</p>" +
                "</div>" +
                "<div class='content'>" +
                "<p>Bonjour,</p>" +
                "<p>Voici votre code de vérification :</p>" +
                "<div class='code'>" + code + "</div>" +
                "<p>Ce code expirera dans <strong>15 minutes</strong>.</p>" +
                "<p>Si vous n'avez pas demandé cette vérification, ignorez cet email.</p>" +
                "<p>Cordialement,<br>L'équipe BoostUp</p>" +
                "</div>" +
                "<div class='footer'>" +
                "<p>© 2024 BoostUp. Tous droits réservés.</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    /**
     * Construit le HTML pour l'email de bienvenue
     */
    private static String buildWelcomeEmailHTML(String displayName) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<style>" +
                "body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f4f4f4; }" +
                ".container { max-width: 600px; margin: 20px auto; background: white; border-radius: 10px; }" +
                ".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }" +
                ".content { padding: 40px; }" +
                ".footer { padding: 20px; text-align: center; color: #666; font-size: 12px; border-top: 1px solid #eee; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='header'>" +
                "<h1>🚀 Bienvenue sur BoostUp !</h1>" +
                "</div>" +
                "<div class='content'>" +
                "<h2>Bonjour " + displayName + ",</h2>" +
                "<p>Nous sommes ravis de vous accueillir sur BoostUp, la plateforme de networking professionnel.</p>" +
                "<p>Vous pouvez maintenant :</p>" +
                "<ul>" +
                "<li>Découvrir des startups innovantes</li>" +
                "<li>Participer à des événements exclusifs</li>" +
                "<li>Connecter avec des investisseurs</li>" +
                "<li>Développer votre réseau professionnel</li>" +
                "</ul>" +
                "<p>À très bientôt sur BoostUp !</p>" +
                "<p>L'équipe BoostUp</p>" +
                "</div>" +
                "<div class='footer'>" +
                "<p>© 2024 BoostUp. Tous droits réservés.</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    /**
     * Construit le HTML pour l'email de blocage
     */
    private static String buildBlockEmailHTML(int minutes) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<style>" +
                "body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f4f4f4; }" +
                ".container { max-width: 600px; margin: 20px auto; background: white; border-radius: 10px; }" +
                ".header { background: #dc3545; color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }" +
                ".content { padding: 40px; }" +
                ".footer { padding: 20px; text-align: center; color: #666; font-size: 12px; border-top: 1px solid #eee; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='header'>" +
                "<h1>🔒 Compte temporairement bloqué</h1>" +
                "</div>" +
                "<div class='content'>" +
                "<p>Bonjour,</p>" +
                "<p>Nous vous informons que votre compte a été temporairement bloqué pour une durée de <strong>" + minutes + " minutes</strong>.</p>" +
                "<p>Cette mesure a été prise pour des raisons de sécurité. Passé ce délai, vous pourrez à nouveau vous connecter.</p>" +
                "<p>Si vous pensez qu'il s'agit d'une erreur, veuillez contacter notre support.</p>" +
                "<p>Cordialement,<br>L'équipe BoostUp</p>" +
                "</div>" +
                "<div class='footer'>" +
                "<p>© 2024 BoostUp. Tous droits réservés.</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    /**
     * Méthode utilitaire pour vérifier si l'email est valide
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        return email.matches(emailRegex);
    }
}