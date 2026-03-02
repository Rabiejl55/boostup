package services.CandidatureService;

import entities.GCandidature.Candidature;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import java.util.Properties;

/**
 * EmailService — Envoi automatique via Gmail SMTP (JavaMail API).
 *
 * ── CONFIG GMAIL (une seule fois) ──────────────────────────────
 * 1. Connectez-vous à votre compte Gmail
 * 2. Allez sur : https://myaccount.google.com/apppasswords
 * 3. Créez un "Mot de passe d'application" → copiez les 16 caractères
 * 4. Remplacez SENDER_EMAIL et SENDER_APP_PASSWORD ci-dessous
 * ────────────────────────────────────────────────────────────────
 */
public class EmailService {

    // ── ⚠️ REMPLACEZ CES VALEURS ─────────────────────────────────
    private static final String SENDER_EMAIL        = "arijbensalem901@gmail.com";
    private static final String SENDER_APP_PASSWORD = "alnf byxe hiqx knxt"; // mot de passe d'application Gmail
    private static final String APP_NAME            = "BOOSTUP";
    // ──────────────────────────────────────────────────────────────

    /**
     * Envoie un email de notification lors d'un changement de statut.
     * Appelé automatiquement depuis CandidatureService.updateStatut()
     *
     * @param candidature la candidature mise à jour (avec le nouveau statut)
     * @param emailDestinataire l'email de la startup
     */
    public void envoyerNotificationStatut(Candidature candidature, String emailDestinataire) {
        if (emailDestinataire == null || emailDestinataire.isBlank()) return;

        // Lancement dans un thread séparé pour ne pas bloquer l'UI
        Thread t = new Thread(() -> {
            try {
                Session session = creerSession();
                Message message = new MimeMessage(session);

                message.setFrom(new InternetAddress(SENDER_EMAIL, APP_NAME));
                message.setRecipients(Message.RecipientType.TO,
                        InternetAddress.parse(emailDestinataire));
                message.setSubject(buildSubject(candidature));
                message.setContent(buildHtmlBody(candidature), "text/html; charset=UTF-8");

                Transport.send(message);
                System.out.println("✅ Email envoyé à : " + emailDestinataire
                        + " [" + candidature.getStatut() + "]");

            } catch (Exception e) {
                System.err.println("❌ Erreur envoi email : " + e.getMessage());
                e.printStackTrace();
            }
        });
        t.setDaemon(true);
        t.start();
    }

    // ─── Session SMTP Gmail ───────────────────────────────────────

    private Session creerSession() {
        Properties props = new Properties();
        props.put("mail.smtp.host",            "smtp.gmail.com");
        props.put("mail.smtp.port",            "587");
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.ssl.trust",       "smtp.gmail.com");

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SENDER_EMAIL, SENDER_APP_PASSWORD);
            }
        });
    }

    // ─── Sujet de l'email ─────────────────────────────────────────

    private String buildSubject(Candidature c) {
        return switch (nvl(c.getStatut())) {
            case "VALIDEE" -> "✅ [BOOSTUP] Candidature acceptée — " + nvl(c.getNomCandidature());
            case "REFUSEE" -> "❌ [BOOSTUP] Candidature refusée — "  + nvl(c.getNomCandidature());
            default        -> "⏳ [BOOSTUP] Mise à jour de votre candidature — " + nvl(c.getNomCandidature());
        };
    }

    // ─── Template HTML ────────────────────────────────────────────

    private String buildHtmlBody(Candidature c) {
        String statut      = nvl(c.getStatut());
        String nomCand     = nvl(c.getNomCandidature());
        String nomStartup  = nvl(c.getNomStartup());
        String score       = c.getScore() != null ? String.format("%.2f / 10", c.getScore()) : "—";
        String commentaire = c.getCommentaire() != null && !c.getCommentaire().isBlank()
                ? c.getCommentaire() : "Aucun commentaire";
        String dateDepot   = c.getDateDepot() != null ? c.getDateDepot().toString() : "—";

        // Couleurs selon statut
        String couleur, emoji, titreStatut, messageIntro;
        switch (statut) {
            case "VALIDEE" -> {
                couleur     = "#0d7a57";
                emoji       = "✅";
                titreStatut = "Candidature acceptée !";
                messageIntro = "Félicitations ! Votre candidature a été examinée par notre jury et a été <strong>acceptée</strong>.";
            }
            case "REFUSEE" -> {
                couleur     = "#b91c1c";
                emoji       = "❌";
                titreStatut = "Candidature non retenue";
                messageIntro = "Après examen par notre jury, nous avons le regret de vous informer que votre candidature n'a pas été retenue cette fois-ci.";
            }
            default -> {
                couleur     = "#d97706";
                emoji       = "⏳";
                titreStatut = "Candidature en cours d'examen";
                messageIntro = "Votre candidature est en cours d'examen par notre jury. Vous serez notifié dès qu'une décision sera prise.";
            }
        }

        // Bloc score + commentaire — affiché seulement si VALIDEE ou REFUSEE
        String blocEvaluation = "";
        if ("VALIDEE".equals(statut) || "REFUSEE".equals(statut)) {
            blocEvaluation = """
                <div style="background:#f8fafc; border-radius:10px; padding:20px 24px;
                            margin:20px 0; border-left:4px solid %s;">
                    <p style="margin:0 0 14px 0; font-size:14px; color:#374151; font-weight:600;">
                        📊 Détails de l'évaluation
                    </p>
                    <table width="100%%" cellpadding="0" cellspacing="0">
                        <tr>
                            <td style="padding:6px 0; color:#6b7280; font-size:13px; width:140px;">Score obtenu</td>
                            <td style="padding:6px 0; color:#111827; font-size:14px; font-weight:700;">%s</td>
                        </tr>
                        <tr>
                            <td style="padding:6px 0; color:#6b7280; font-size:13px;">Date de dépôt</td>
                            <td style="padding:6px 0; color:#111827; font-size:13px;">%s</td>
                        </tr>
                        <tr>
                            <td style="padding:6px 0; color:#6b7280; font-size:13px; vertical-align:top;">Commentaire</td>
                            <td style="padding:6px 0; color:#374151; font-size:13px; font-style:italic;">"%s"</td>
                        </tr>
                    </table>
                </div>
                """.formatted(couleur, score, dateDepot, commentaire);
        }

        return """
            <!DOCTYPE html>
            <html lang="fr">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="margin:0; padding:0; background:#f3f4f6; font-family:'Segoe UI', Arial, sans-serif;">

            <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f3f4f6; padding:40px 20px;">
                <tr><td align="center">
                <table width="600" cellpadding="0" cellspacing="0"
                       style="background:white; border-radius:16px; overflow:hidden;
                              box-shadow:0 4px 24px rgba(0,0,0,0.08); max-width:600px;">

                    <!-- HEADER -->
                    <tr>
                        <td style="background:linear-gradient(135deg, #1e2a6e, #4f62e8);
                                   padding:36px 40px; text-align:center;">
                            <p style="margin:0 0 6px 0; font-size:28px; font-weight:900;
                                      color:white; letter-spacing:2px;">🚀 BOOSTUP</p>
                            <p style="margin:0; font-size:13px; color:rgba(255,255,255,0.65);">
                                Plateforme de gestion des candidatures startup
                            </p>
                        </td>
                    </tr>

                    <!-- BADGE STATUT -->
                    <tr>
                        <td style="padding:0; text-align:center;">
                            <div style="display:inline-block; margin:28px auto 0;
                                        background:%s; color:white;
                                        border-radius:30px; padding:10px 28px;
                                        font-size:15px; font-weight:700;">
                                %s &nbsp; %s
                            </div>
                        </td>
                    </tr>

                    <!-- BODY -->
                    <tr>
                        <td style="padding:24px 40px 36px;">

                            <p style="margin:0 0 8px 0; font-size:20px; font-weight:800; color:#111827;">
                                Bonjour, <span style="color:#4f62e8;">%s</span> 👋
                            </p>
                            <p style="margin:0 0 20px 0; font-size:14px; color:#6b7280; line-height:1.6;">
                                %s
                            </p>

                            <!-- Infos candidature -->
                            <div style="background:#f8fafc; border-radius:10px;
                                        padding:18px 24px; margin-bottom:6px;
                                        border:1px solid #e5e7eb;">
                                <p style="margin:0 0 10px 0; font-size:13px;
                                          font-weight:700; color:#374151;">📋 Votre candidature</p>
                                <p style="margin:0; font-size:18px; font-weight:800; color:#111827;">
                                    %s
                                </p>
                            </div>

                            %s

                            <p style="margin:28px 0 12px; font-size:13px; color:#9ca3af;
                                      border-top:1px solid #f3f4f6; padding-top:20px;">
                                Pour toute question, contactez l'équipe BOOSTUP.
                            </p>

                        </td>
                    </tr>

                    <!-- FOOTER -->
                    <tr>
                        <td style="background:#f9fafb; padding:18px 40px; text-align:center;
                                   border-top:1px solid #e5e7eb;">
                            <p style="margin:0; font-size:12px; color:#9ca3af;">
                                © 2026 BOOSTUP · Cet email a été envoyé automatiquement, merci de ne pas y répondre.
                            </p>
                        </td>
                    </tr>

                </table>
                </td></tr>
            </table>
            </body>
            </html>
            """.formatted(
                couleur, emoji, titreStatut,  // badge
                nomStartup,                   // bonjour
                messageIntro,                 // intro
                nomCand,                      // nom candidature
                blocEvaluation                // évaluation
        );
    }

    private String nvl(String s) { return s == null ? "" : s; }
}