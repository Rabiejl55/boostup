package services;

import entities.GEvenement.Evenement;
import javafx.application.Platform;
import javafx.scene.control.Alert;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

/**
 * 📧 SERVICE EMAIL - Envoi automatique d'emails professionnels
 * Envoie des emails HTML stylés pour les inscriptions, rappels, annulations
 */
public class EmailService {

    // ════════════════════════════════════════════════════════
    // 🔐 CONFIGURATION GMAIL SMTP
    // ════════════════════════════════════════════════════════

    // ⚠️ IMPORTANT : Utilise un compte Gmail dédié pour l'app
    // Tu devras activer "Mots de passe d'application" dans ton compte Google

    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";

    // 📧 Email expéditeur (l'app BoostUp)
    // ⚠️ DOIT ÊTRE UN EMAIL @gmail.com !
    private static final String FROM_EMAIL = "rayenuchiha22@gmail.com";
    private static final String FROM_PASSWORD = "fatmklnqjjhdcysc"; // ⬅️ À REMPLACER par le code Google

    // 📧 Email destinataire par défaut (toi)
    private static final String DEFAULT_RECIPIENT = "rayen.amri@esprit.tn";

    // ════════════════════════════════════════════════════════
    // 📤 ENVOI EMAIL DE BIENVENUE (INSCRIPTION)
    // ════════════════════════════════════════════════════════

    /**
     * Envoie un email de bienvenue quand un utilisateur s'inscrit à un événement
     * @param evenement L'événement auquel l'utilisateur s'est inscrit
     * @param nomUtilisateur Nom de l'utilisateur
     * @param emailUtilisateur Email de l'utilisateur (optionnel, sinon par défaut)
     */
    public static void envoyerEmailBienvenue(Evenement evenement, String nomUtilisateur, String emailUtilisateur) {
        // Si pas d'email fourni, utiliser l'email par défaut (le tien)
        String destinataire = (emailUtilisateur != null && !emailUtilisateur.isEmpty())
                              ? emailUtilisateur
                              : DEFAULT_RECIPIENT;

        System.out.println("\n═══════════════════════════════════════════════");
        System.out.println("📧 ENVOI EMAIL DE BIENVENUE");
        System.out.println("═══════════════════════════════════════════════");
        System.out.println("📞 De: " + FROM_EMAIL);
        System.out.println("📞 Vers: " + destinataire);
        System.out.println("📅 Événement: " + evenement.getTitre());
        System.out.println("👤 Utilisateur: " + nomUtilisateur);
        System.out.println("═══════════════════════════════════════════════\n");

        // Exécution asynchrone pour ne pas bloquer l'interface
        new Thread(() -> {
            try {
                System.out.println("🔄 Configuration SMTP...");

                // 🔐 Configuration SMTP Gmail
                Properties props = new Properties();
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.host", SMTP_HOST);
                props.put("mail.smtp.port", SMTP_PORT);
                props.put("mail.smtp.ssl.protocols", "TLSv1.2");

                // 🔑 Authentification
                Session session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(FROM_EMAIL, FROM_PASSWORD);
                    }
                });

                System.out.println("✅ Session SMTP créée");

                // 📝 Construction du message
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(FROM_EMAIL, "BoostUp Events"));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinataire));
                message.setSubject("🎉 Bienvenue à " + evenement.getTitre() + " - BoostUp");

                // 🎨 Contenu HTML stylé
                String htmlContent = construireEmailBienvenue(evenement, nomUtilisateur);
                message.setContent(htmlContent, "text/html; charset=utf-8");

                System.out.println("🚀 Envoi de l'email...");

                // 📤 Envoi
                Transport.send(message);

                System.out.println("\n✅✅✅ EMAIL ENVOYÉ AVEC SUCCÈS ! ✅✅✅");
                System.out.println("   📧 Destinataire: " + destinataire);
                System.out.println("   📋 Sujet: Bienvenue à " + evenement.getTitre());

                // Notification à l'utilisateur
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Email envoyé ✅");
                    alert.setHeaderText("Email de bienvenue envoyé");
                    alert.setContentText("Un email de confirmation a été envoyé à:\n" + destinataire);
                    alert.showAndWait();
                });

            } catch (Exception e) {
                System.err.println("\n❌❌❌ ERREUR LORS DE L'ENVOI EMAIL ❌❌❌");
                System.err.println("Type: " + e.getClass().getName());
                System.err.println("Message: " + e.getMessage());
                e.printStackTrace();

                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Email non envoyé");
                    alert.setHeaderText("Impossible d'envoyer l'email");
                    alert.setContentText("L'inscription est confirmée mais l'email n'a pas pu être envoyé.\n\n" +
                                       "Erreur : " + e.getMessage() + "\n\n" +
                                       "💡 Vérifie la configuration SMTP dans EmailService.java");
                    alert.showAndWait();
                });
            }
        }).start();
    }

    // ════════════════════════════════════════════════════════
    // 🎨 CONSTRUCTION DU TEMPLATE HTML
    // ════════════════════════════════════════════════════════

    /**
     * Construit le contenu HTML de l'email de bienvenue
     */
    private static String construireEmailBienvenue(Evenement evenement, String nomUtilisateur) {
        String dateFormatee = evenement.getDateEvenement() != null
            ? evenement.getDateEvenement().toString()
            : "Date à confirmer";

        return "<!DOCTYPE html>" +
            "<html lang='fr'>" +
            "<head>" +
            "  <meta charset='UTF-8'>" +
            "  <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
            "  <title>Bienvenue - BoostUp</title>" +
            "</head>" +
            "<body style='margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #f0f2f5;'>" +
            "  <div style='max-width: 600px; margin: 40px auto; background: white; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 20px rgba(0,0,0,0.1);'>" +
            "    " +
            "    <!-- Header avec gradient violet/bleu -->" +
            "    <div style='background: linear-gradient(135deg, #1b2a4a 0%, #2d1b4e 100%); padding: 40px 30px; text-align: center;'>" +
            "      <h1 style='color: white; margin: 0; font-size: 32px; font-weight: 800;'>⚡ BoostUp</h1>" +
            "      <p style='color: rgba(255,255,255,0.9); margin: 10px 0 0 0; font-size: 14px;'>Votre plateforme événementielle</p>" +
            "    </div>" +
            "    " +
            "    <!-- Contenu principal -->" +
            "    <div style='padding: 40px 30px;'>" +
            "      <h2 style='color: #1b2a4a; margin: 0 0 20px 0; font-size: 24px;'>🎉 Inscription confirmée !</h2>" +
            "      " +
            "      <p style='color: #495057; font-size: 16px; line-height: 1.6; margin: 0 0 15px 0;'>" +
            "        Bonjour <strong>" + nomUtilisateur + "</strong>,</p>" +
            "      " +
            "      <p style='color: #495057; font-size: 16px; line-height: 1.6; margin: 0 0 25px 0;'>" +
            "        Nous sommes ravis de vous compter parmi les participants à cet événement exceptionnel !</p>" +
            "      " +
            "      <!-- Détails de l'événement -->" +
            "      <div style='background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); border-radius: 12px; padding: 25px; margin: 25px 0;'>" +
            "        <h3 style='color: white; margin: 0 0 20px 0; font-size: 20px; font-weight: 700;'>📌 Détails de l'événement</h3>" +
            "        " +
            "        <div style='background: rgba(255,255,255,0.1); border-radius: 8px; padding: 15px; margin-bottom: 12px;'>" +
            "          <p style='color: rgba(255,255,255,0.8); margin: 0 0 5px 0; font-size: 12px; text-transform: uppercase;'>Titre</p>" +
            "          <p style='color: white; margin: 0; font-size: 18px; font-weight: 700;'>" + evenement.getTitre() + "</p>" +
            "        </div>" +
            "        " +
            "        <div style='background: rgba(255,255,255,0.1); border-radius: 8px; padding: 15px; margin-bottom: 12px;'>" +
            "          <p style='color: rgba(255,255,255,0.8); margin: 0 0 5px 0; font-size: 12px; text-transform: uppercase;'>📅 Date</p>" +
            "          <p style='color: white; margin: 0; font-size: 16px; font-weight: 600;'>" + dateFormatee + "</p>" +
            "        </div>" +
            "        " +
            "        <div style='background: rgba(255,255,255,0.1); border-radius: 8px; padding: 15px; margin-bottom: 12px;'>" +
            "          <p style='color: rgba(255,255,255,0.8); margin: 0 0 5px 0; font-size: 12px; text-transform: uppercase;'>📍 Lieu</p>" +
            "          <p style='color: white; margin: 0; font-size: 16px; font-weight: 600;'>" + evenement.getLieu() + "</p>" +
            "        </div>" +
            "        " +
            "        <div style='background: rgba(255,255,255,0.1); border-radius: 8px; padding: 15px;'>" +
            "          <p style='color: rgba(255,255,255,0.8); margin: 0 0 5px 0; font-size: 12px; text-transform: uppercase;'>Type</p>" +
            "          <p style='color: white; margin: 0; font-size: 16px; font-weight: 600;'>" + evenement.getType() + "</p>" +
            "        </div>" +
            "      </div>" +
            "      " +
            "      <!-- Description -->" +
            "      <div style='background: #f8f9fa; border-left: 4px solid #667eea; padding: 15px 20px; border-radius: 8px; margin: 25px 0;'>" +
            "        <p style='color: #6c757d; margin: 0; font-size: 14px; line-height: 1.6;'>" +
            (evenement.getDescription() != null ? evenement.getDescription() : "Un événement exceptionnel vous attend !") +
            "</p>" +
            "      </div>" +
            "      " +
            "      <!-- Conseils pratiques -->" +
            "      <div style='background: #fff8e1; border-radius: 8px; padding: 20px; margin: 25px 0;'>" +
            "        <h4 style='color: #f57c00; margin: 0 0 15px 0; font-size: 16px;'>💡 Conseils pratiques</h4>" +
            "        <ul style='color: #6c757d; margin: 0; padding-left: 20px; font-size: 14px; line-height: 1.8;'>" +
            "          <li>Arrivez 15 minutes avant le début</li>" +
            "          <li>Pensez à apporter une pièce d'identité</li>" +
            "          <li>Consultez régulièrement vos emails pour les mises à jour</li>" +
            "        </ul>" +
            "      </div>" +
            "      " +
            "      <p style='color: #495057; font-size: 16px; line-height: 1.6; margin: 25px 0;'>" +
            "        À très bientôt ! 🚀</p>" +
            "      " +
            "      <p style='color: #6c757d; font-size: 14px; margin: 0;'>L'équipe BoostUp</p>" +
            "    </div>" +
            "    " +
            "    <!-- Footer -->" +
            "    <div style='background: #f8f9fa; padding: 30px; text-align: center; border-top: 1px solid #e9ecef;'>" +
            "      <p style='color: #adb5bd; font-size: 12px; margin: 0 0 10px 0;'>" +
            "        © 2026 BoostUp - Votre succès commence ici</p>" +
            "      <p style='color: #adb5bd; font-size: 11px; margin: 0;'>" +
            "        Cet email a été envoyé automatiquement, merci de ne pas y répondre.</p>" +
            "    </div>" +
            "  </div>" +
            "</body>" +
            "</html>";
    }

    // ════════════════════════════════════════════════════════
    // 📤 ENVOI EMAIL D'ANNULATION D'ÉVÉNEMENT
    // ════════════════════════════════════════════════════════

    /**
     * Envoie un email quand un événement est annulé/supprimé
     * @param evenement L'événement supprimé
     */
    public static void envoyerEmailAnnulation(Evenement evenement) {
        String destinataire = DEFAULT_RECIPIENT; // Ton email

        System.out.println("\n═══════════════════════════════════════════════");
        System.out.println("📧 ENVOI EMAIL D'ANNULATION");
        System.out.println("═══════════════════════════════════════════════");
        System.out.println("📞 De: " + FROM_EMAIL);
        System.out.println("📞 Vers: " + destinataire);
        System.out.println("📅 Événement: " + evenement.getTitre());
        System.out.println("═══════════════════════════════════════════════\n");

        // Exécution asynchrone
        new Thread(() -> {
            try {
                System.out.println("🔄 Configuration SMTP...");

                // 🔐 Configuration SMTP Gmail
                Properties props = new Properties();
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.host", SMTP_HOST);
                props.put("mail.smtp.port", SMTP_PORT);
                props.put("mail.smtp.ssl.protocols", "TLSv1.2");

                // 🔑 Authentification
                Session session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(FROM_EMAIL, FROM_PASSWORD);
                    }
                });

                System.out.println("✅ Session SMTP créée");

                // 📝 Construction du message
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(FROM_EMAIL, "BoostUp Events"));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinataire));
                message.setSubject("🚫 Événement annulé : " + evenement.getTitre() + " - BoostUp");

                // 🎨 Contenu HTML stylé
                String htmlContent = construireEmailAnnulation(evenement);
                message.setContent(htmlContent, "text/html; charset=utf-8");

                System.out.println("🚀 Envoi de l'email...");

                // 📤 Envoi
                Transport.send(message);

                System.out.println("\n✅✅✅ EMAIL D'ANNULATION ENVOYÉ ! ✅✅✅");
                System.out.println("   📧 Destinataire: " + destinataire);

                // Notification à l'utilisateur
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Email d'annulation envoyé ✅");
                    alert.setHeaderText("Notification envoyée");
                    alert.setContentText("Un email d'annulation a été envoyé à:\n" + destinataire);
                    alert.showAndWait();
                });

            } catch (Exception e) {
                System.err.println("\n❌ ERREUR ENVOI EMAIL ANNULATION");
                e.printStackTrace();

                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Email non envoyé");
                    alert.setHeaderText("Erreur d'envoi");
                    alert.setContentText("Erreur : " + e.getMessage());
                    alert.showAndWait();
                });
            }
        }).start();
    }

    /**
     * 🎨 Construit le template HTML pour l'email d'annulation
     */
    private static String construireEmailAnnulation(Evenement evenement) {
        String dateFormatee = evenement.getDateEvenement() != null
            ? evenement.getDateEvenement().toString()
            : "Date non définie";

        return "<!DOCTYPE html>" +
            "<html lang='fr'>" +
            "<head>" +
            "  <meta charset='UTF-8'>" +
            "  <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
            "  <title>Événement annulé - BoostUp</title>" +
            "</head>" +
            "<body style='margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #f0f2f5;'>" +
            "  <div style='max-width: 600px; margin: 40px auto; background: white; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 20px rgba(0,0,0,0.1);'>" +
            "    " +
            "    <!-- Header avec gradient rouge/orange (annulation) -->" +
            "    <div style='background: linear-gradient(135deg, #e63956 0%, #f57c00 100%); padding: 40px 30px; text-align: center;'>" +
            "      <h1 style='color: white; margin: 0; font-size: 48px;'>🚫</h1>" +
            "      <h2 style='color: white; margin: 15px 0 0 0; font-size: 28px; font-weight: 800;'>Événement annulé</h2>" +
            "      <p style='color: rgba(255,255,255,0.9); margin: 10px 0 0 0; font-size: 14px;'>BoostUp - Notification importante</p>" +
            "    </div>" +
            "    " +
            "    <!-- Contenu principal -->" +
            "    <div style='padding: 40px 30px;'>" +
            "      <p style='color: #495057; font-size: 16px; line-height: 1.6; margin: 0 0 20px 0;'>" +
            "        Bonjour,</p>" +
            "      " +
            "      <p style='color: #495057; font-size: 16px; line-height: 1.6; margin: 0 0 25px 0;'>" +
            "        Nous sommes désolés de vous informer que l'événement suivant est <strong style='color: #e63956;'>annulé</strong> :</p>" +
            "      " +
            "      <!-- Détails de l'événement annulé -->" +
            "      <div style='background: linear-gradient(135deg, #e63956 0%, #f57c00 100%); border-radius: 12px; padding: 25px; margin: 25px 0;'>" +
            "        <h3 style='color: white; margin: 0 0 20px 0; font-size: 20px; font-weight: 700;'>📌 Événement annulé</h3>" +
            "        " +
            "        <div style='background: rgba(255,255,255,0.15); border-radius: 8px; padding: 15px; margin-bottom: 12px;'>" +
            "          <p style='color: rgba(255,255,255,0.8); margin: 0 0 5px 0; font-size: 12px; text-transform: uppercase;'>Titre</p>" +
            "          <p style='color: white; margin: 0; font-size: 18px; font-weight: 700;'>" + evenement.getTitre() + "</p>" +
            "        </div>" +
            "        " +
            "        <div style='background: rgba(255,255,255,0.15); border-radius: 8px; padding: 15px; margin-bottom: 12px;'>" +
            "          <p style='color: rgba(255,255,255,0.8); margin: 0 0 5px 0; font-size: 12px; text-transform: uppercase;'>📅 Date prévue</p>" +
            "          <p style='color: white; margin: 0; font-size: 16px; font-weight: 600;'>" + dateFormatee + "</p>" +
            "        </div>" +
            "        " +
            "        <div style='background: rgba(255,255,255,0.15); border-radius: 8px; padding: 15px;'>" +
            "          <p style='color: rgba(255,255,255,0.8); margin: 0 0 5px 0; font-size: 12px; text-transform: uppercase;'>📍 Lieu prévu</p>" +
            "          <p style='color: white; margin: 0; font-size: 16px; font-weight: 600;'>" + evenement.getLieu() + "</p>" +
            "        </div>" +
            "      </div>" +
            "      " +
            "      <!-- Message d'excuses -->" +
            "      <div style='background: #fff3cd; border-left: 4px solid #f57c00; padding: 20px; border-radius: 8px; margin: 25px 0;'>" +
            "        <p style='color: #856404; margin: 0; font-size: 15px; line-height: 1.6;'>" +
            "          <strong>Nous nous excusons sincèrement pour ce désagrément.</strong><br><br>" +
            "          Cet événement a dû être annulé pour des raisons indépendantes de notre volonté. " +
            "          Nous ferons tout notre possible pour organiser un événement similaire prochainement.</p>" +
            "      </div>" +
            "      " +
            "      <!-- Prochaines étapes -->" +
            "      <div style='background: #e7f3ff; border-radius: 8px; padding: 20px; margin: 25px 0;'>" +
            "        <h4 style='color: #0c5da5; margin: 0 0 15px 0; font-size: 16px;'>💡 Que faire maintenant ?</h4>" +
            "        <ul style='color: #495057; margin: 0; padding-left: 20px; font-size: 14px; line-height: 1.8;'>" +
            "          <li>Consultez notre plateforme pour découvrir d'autres événements</li>" +
            "          <li>Vous serez informé en priorité de nos prochains événements</li>" +
            "          <li>N'hésitez pas à nous contacter pour toute question</li>" +
            "        </ul>" +
            "      </div>" +
            "      " +
            "      <p style='color: #495057; font-size: 16px; line-height: 1.6; margin: 25px 0;'>" +
            "        <strong>À la prochaine inchallah ! 🙏</strong></p>" +
            "      " +
            "      <p style='color: #6c757d; font-size: 14px; margin: 0;'>Cordialement,<br>L'équipe BoostUp</p>" +
            "    </div>" +
            "    " +
            "    <!-- Footer -->" +
            "    <div style='background: #f8f9fa; padding: 30px; text-align: center; border-top: 1px solid #e9ecef;'>" +
            "      <p style='color: #adb5bd; font-size: 12px; margin: 0 0 10px 0;'>" +
            "        © 2026 BoostUp - Votre succès commence ici</p>" +
            "      <p style='color: #adb5bd; font-size: 11px; margin: 0;'>" +
            "        Cet email a été envoyé automatiquement, merci de ne pas y répondre.</p>" +
            "    </div>" +
            "  </div>" +
            "</body>" +
            "</html>";
    }

    // ════════════════════════════════════════════════════════
    // 📤 ENVOI EMAIL DE BIENVENUE (INSCRIPTION UTILISATEUR)
    // ════════════════════════════════════════════════════════

    /**
     * Envoie un email de bienvenue lors de l'inscription d'un nouvel utilisateur
     * @param email Email de l'utilisateur
     * @param nom Nom de l'utilisateur
     */
    public static void sendWelcomeEmail(String email, String nom) {
        System.out.println("\n═══════════════════════════════════════════════");
        System.out.println("📧 ENVOI EMAIL BIENVENUE UTILISATEUR");
        System.out.println("═══════════════════════════════════════════════");
        System.out.println("📞 De: " + FROM_EMAIL);
        System.out.println("📞 Vers: " + email);
        System.out.println("👤 Utilisateur: " + nom);
        System.out.println("═══════════════════════════════════════════════\n");

        // Exécution asynchrone
        new Thread(() -> {
            try {
                // Configuration SMTP
                Properties props = new Properties();
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.host", SMTP_HOST);
                props.put("mail.smtp.port", SMTP_PORT);
                props.put("mail.smtp.ssl.protocols", "TLSv1.2");

                // Authentification
                Session session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(FROM_EMAIL, FROM_PASSWORD);
                    }
                });

                // Construction du message
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(FROM_EMAIL, "BoostUp Platform"));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
                message.setSubject("🎉 Bienvenue sur BoostUp - Votre compte est créé !");

                // Contenu HTML
                String htmlContent = construireEmailBienvenueUtilisateur(nom);
                message.setContent(htmlContent, "text/html; charset=utf-8");

                // Envoi
                Transport.send(message);

                System.out.println("\n✅✅✅ EMAIL BIENVENUE UTILISATEUR ENVOYÉ ! ✅✅✅");
                System.out.println("   📧 Destinataire: " + email);

            } catch (Exception e) {
                System.err.println("\n❌ ERREUR ENVOI EMAIL BIENVENUE UTILISATEUR");
                System.err.println("Message: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Construit le template HTML pour l'email de bienvenue utilisateur
     */
    private static String construireEmailBienvenueUtilisateur(String nom) {
        return "<!DOCTYPE html>" +
            "<html lang='fr'>" +
            "<head>" +
            "  <meta charset='UTF-8'>" +
            "  <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
            "</head>" +
            "<body style='margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #f0f2f5;'>" +
            "  <div style='max-width: 600px; margin: 40px auto; background: white; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 20px rgba(0,0,0,0.1);'>" +
            "    <div style='background: linear-gradient(135deg, #1b2a4a 0%, #2d1b4e 100%); padding: 40px 30px; text-align: center;'>" +
            "      <h1 style='color: white; margin: 0; font-size: 32px; font-weight: 800;'>⚡ BoostUp</h1>" +
            "      <p style='color: rgba(255,255,255,0.9); margin: 10px 0 0 0; font-size: 14px;'>Votre plateforme startup</p>" +
            "    </div>" +
            "    <div style='padding: 40px 30px;'>" +
            "      <h2 style='color: #1b2a4a; margin: 0 0 20px 0; font-size: 24px;'>🎉 Bienvenue sur BoostUp !</h2>" +
            "      <p style='color: #495057; font-size: 16px; line-height: 1.6; margin: 0 0 15px 0;'>" +
            "        Bonjour <strong>" + nom + "</strong>,</p>" +
            "      <p style='color: #495057; font-size: 16px; line-height: 1.6; margin: 0 0 25px 0;'>" +
            "        Votre compte BoostUp a été créé avec succès ! Vous pouvez maintenant accéder à toutes nos fonctionnalités :</p>" +
            "      <ul style='color: #495057; font-size: 15px; line-height: 1.8; margin: 25px 0;'>" +
            "        <li>🚀 Explorer les startups innovantes</li>" +
            "        <li>📅 S'inscrire aux événements exclusifs</li>" +
            "        <li>💰 Découvrir les opportunités d'investissement</li>" +
            "        <li>🤝 Participer aux accompagnements</li>" +
            "      </ul>" +
            "      <div style='text-align: center; margin: 30px 0;'>" +
            "        <a href='#' style='display: inline-block; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 15px 40px; text-decoration: none; border-radius: 8px; font-weight: 700; font-size: 16px;'>Connectez-vous maintenant</a>" +
            "      </div>" +
            "      <div style='background: #f8f9fa; border-left: 4px solid #667eea; padding: 15px 20px; border-radius: 8px; margin: 25px 0;'>" +
            "        <p style='color: #6c757d; margin: 0; font-size: 14px;'>💡 <strong>Astuce :</strong> Complétez votre profil pour profiter pleinement de toutes nos fonctionnalités.</p>" +
            "      </div>" +
            "    </div>" +
            "    <div style='background: #f8f9fa; padding: 25px 30px; text-align: center; border-top: 1px solid #e9ecef;'>" +
            "      <p style='color: #6c757d; margin: 0 0 10px 0; font-size: 13px;'>Besoin d'aide ? Contactez-nous :</p>" +
            "      <p style='color: #1b2a4a; margin: 0; font-size: 14px; font-weight: 600;'>📧 support@boostup.com</p>" +
            "      <p style='color: #adb5bd; margin: 15px 0 0 0; font-size: 12px;'>© 2026 BoostUp. Tous droits réservés.</p>" +
            "    </div>" +
            "  </div>" +
            "</body>" +
            "</html>";
    }

    // ════════════════════════════════════════════════════════
    // 🔐 ENVOI EMAIL RÉCUPÉRATION MOT DE PASSE
    // ════════════════════════════════════════════════════════

    /**
     * 📧 Envoie un code de vérification pour la récupération du mot de passe
     * @param emailDemandeur Email qui a demandé la récupération
     * @param codeVerification Code à 6 chiffres
     */
    public static void envoyerEmailRecuperationMotDePasse(String emailDemandeur, String codeVerification) {
        new Thread(() -> {
            try {
                // Configuration SMTP
                Properties props = new Properties();
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.host", SMTP_HOST);
                props.put("mail.smtp.port", SMTP_PORT);

                // Session email
                Session session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(FROM_EMAIL, FROM_PASSWORD);
                    }
                });

                // Construction du message
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(FROM_EMAIL, "BoostUp - Récupération"));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(DEFAULT_RECIPIENT));
                message.setSubject("🔐 Code de récupération - BoostUp");

                // Corps HTML stylé
                String htmlContent = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta charset="UTF-8">
                        <style>
                            body { font-family: 'Segoe UI', Arial, sans-serif; background: #f0f2f5; margin: 0; padding: 20px; }
                            .container { max-width: 600px; margin: 0 auto; background: white; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 20px rgba(0,0,0,0.1); }
                            .header { background: linear-gradient(135deg, #1b2a4a, #2d1b4e); padding: 30px; text-align: center; }
                            .header h1 { color: white; margin: 0; font-size: 28px; }
                            .content { padding: 40px 30px; }
                            .code-box { background: linear-gradient(135deg, #e63956, #ff6b8a); color: white; padding: 30px; border-radius: 12px; text-align: center; margin: 30px 0; }
                            .code { font-size: 48px; font-weight: 800; letter-spacing: 8px; margin: 10px 0; }
                            .warning { background: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0; border-radius: 8px; }
                            .footer { background: #f8f9fa; padding: 20px; text-align: center; color: #6c757d; font-size: 12px; }
                            .btn { display: inline-block; background: #1b2a4a; color: white; padding: 12px 30px; text-decoration: none; border-radius: 8px; margin: 20px 0; }
                        </style>
                    </head>
                    <body>
                        <div class="container">
                            <div class="header">
                                <h1>🔐 Récupération de mot de passe</h1>
                            </div>
                            <div class="content">
                                <p style="font-size: 16px; color: #495057;">Bonjour,</p>
                                <p style="color: #6c757d;">Une demande de récupération de mot de passe a été effectuée pour l'adresse : <strong>""" + emailDemandeur + """
                                </strong></p>
                                
                                <div class="code-box">
                                    <p style="margin: 0; font-size: 14px; opacity: 0.9;">Votre code de vérification :</p>
                                    <div class="code">""" + codeVerification + """
                                    </div>
                                    <p style="margin: 0; font-size: 13px; opacity: 0.8;">⏱ Valide pendant 15 minutes</p>
                                </div>
                                
                                <p style="color: #495057;">Pour continuer, entrez ce code dans l'application BoostUp.</p>
                                
                                <div class="warning">
                                    <strong>⚠️ Sécurité :</strong>
                                    <ul style="margin: 10px 0; padding-left: 20px;">
                                        <li>Ne partagez jamais ce code</li>
                                        <li>BoostUp ne vous demandera jamais votre mot de passe par email</li>
                                        <li>Si vous n'avez pas demandé cette récupération, ignorez cet email</li>
                                    </ul>
                                </div>
                            </div>
                            <div class="footer">
                                <p>© 2026 BoostUp - Plateforme de gestion d'événements</p>
                                <p>Cet email a été envoyé automatiquement, merci de ne pas y répondre.</p>
                            </div>
                        </div>
                    </body>
                    </html>
                    """;

                message.setContent(htmlContent, "text/html; charset=UTF-8");

                // Envoi
                Transport.send(message);

                System.out.println("✅ Email de récupération envoyé avec succès à " + DEFAULT_RECIPIENT);
                System.out.println("📧 Pour: " + emailDemandeur + " | Code: " + codeVerification);

            } catch (Exception e) {
                System.err.println("❌ Erreur envoi email récupération: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }
}

