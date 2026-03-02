package controllers;

import entities.GUtilisateurs.User;
import entities.Role_enum;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import services.UtilisateurService.*;

import java.io.File;
import java.sql.SQLException;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;
    @FXML private Button loginButton;
    @FXML private ImageView logoImageView;

    // OTP SMS elements
    @FXML private VBox otpBox;
    @FXML private TextField otpField;
    @FXML private Button verifyOtpButton;
    @FXML private Hyperlink resendOtpLink;
    @FXML private Button faceIdButton;

    // Theme toggle
    @FXML private ToggleButton themeToggle;

    private final UserService userService = new UserService();

    // State for OTP verification
    private User pendingOtpUser = null;
    private ActionEvent pendingEvent = null;
    private String currentOtp = null;

    @FXML
    public void initialize() {
        loadLogo();

        // Appliquer le thème
        Platform.runLater(() -> {
            if (themeToggle != null && themeToggle.getScene() != null) {
                ThemeHelper.applyTheme(themeToggle.getScene(), themeToggle);
            }
        });
    }

    // ═══════════════════════════════════════════════════════
    // LOGIN CLASSIQUE (email + mot de passe) — PAS DE SMS
    // ═══════════════════════════════════════════════════════

    @FXML
    private void handleLogin(ActionEvent event) {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showMessage("Email et mot de passe requis", "error");
            return;
        }

        loginButton.setDisable(true);
        loginButton.setText("Connexion...");

        new Thread(() -> {
            try {
                User user = userService.findByEmail(email);

                if (user == null) {
                    ActivityLogService.logAnonymous(email, ActivityLogService.ACTION_LOGIN_FAILED,
                            "Email non trouvé: " + email);
                    showMessage("Email ou mot de passe incorrect", "error");
                } else if (!user.isActive()) {
                    ActivityLogService.log(user.getId(), email, ActivityLogService.ACTION_LOGIN_FAILED,
                            "Compte bloqué - tentative de connexion");
                    showMessage("❌ Compte bloqué. Veuillez contacter l'administrateur.", "error");
                } else {
                    User connectedUser = userService.seConnecter(email, password);

                    if (connectedUser != null) {
                        // Login classique réussi → connexion directe (pas de SMS)
                        completeLogin(event, connectedUser);
                    } else {
                        ActivityLogService.logAnonymous(email, ActivityLogService.ACTION_LOGIN_FAILED,
                                "Mot de passe incorrect pour: " + email);
                        showMessage("Email ou mot de passe incorrect", "error");
                    }
                }
            } catch (SQLException | InterruptedException e) {
                showMessage("Erreur : " + e.getMessage(), "error");
                e.printStackTrace();
            } finally {
                Platform.runLater(() -> {
                    loginButton.setText("Se connecter");
                    loginButton.setDisable(false);
                });
            }
        }).start();
    }

    // ═══════════════════════════════════════════════════════
    // VÉRIFICATION OTP SMS
    // ═══════════════════════════════════════════════════════

    @FXML
    private void handleVerifyOtp(ActionEvent event) {
        String code = otpField.getText().trim();

        if (code.isEmpty() || code.length() != 6) {
            showMessage("⚠️ Veuillez entrer le code à 6 chiffres", "error");
            return;
        }

        if (pendingOtpUser == null) {
            showMessage("⚠️ Session expirée. Veuillez vous reconnecter.", "error");
            return;
        }

        String formattedPhone = SmsService.formatPhoneNumber(pendingOtpUser.getPhone(), "+216");

        if (SmsService.verifyOtp(formattedPhone, code)) {
            ActivityLogService.log(pendingOtpUser.getId(), pendingOtpUser.getEmail(),
                    ActivityLogService.ACTION_OTP_VERIFIED,
                    "Code OTP vérifié avec succès");
            try {
                completeLogin(pendingEvent != null ? pendingEvent : event, pendingOtpUser);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            ActivityLogService.log(pendingOtpUser.getId(), pendingOtpUser.getEmail(),
                    ActivityLogService.ACTION_OTP_FAILED,
                    "Code OTP incorrect saisi");
            showMessage("❌ Code incorrect ou expiré. Réessayez.", "error");
            otpField.clear();
            otpField.requestFocus();
        }
    }

    @FXML
    private void handleResendOtp(ActionEvent event) {
        if (pendingOtpUser == null) {
            showMessage("⚠️ Session expirée. Veuillez vous reconnecter.", "error");
            return;
        }

        String formattedPhone = SmsService.formatPhoneNumber(pendingOtpUser.getPhone(), "+216");
        currentOtp = SmsService.sendOtp(formattedPhone);

        if (SmsService.didLastSendSucceed()) {
            ActivityLogService.log(pendingOtpUser.getId(), pendingOtpUser.getEmail(),
                    ActivityLogService.ACTION_SMS_SENT,
                    "Code OTP renvoyé à " + maskPhone(formattedPhone));
            showMessage("📱 Code envoyé à " + maskPhone(formattedPhone), "success");
        } else {
            final String otpToShow = currentOtp;
            Platform.runLater(() -> showOtpUi(formattedPhone, otpToShow));
        }
        otpField.clear();
        otpField.requestFocus();
    }

    // ═══════════════════════════════════════════════════════
    // LOGIN PAR FACE ID (Reconnaissance Faciale)
    // ═══════════════════════════════════════════════════════

    @FXML
    private void handleFaceLogin(ActionEvent event) {
        String email = emailField.getText().trim();

        if (email.isEmpty()) {
            showMessage("⚠️ Veuillez d'abord entrer votre email pour Face ID", "error");
            emailField.requestFocus();
            return;
        }

        showMessage("🔐 Initialisation Face ID...", "success");

        new Thread(() -> {
            try {
                User user = userService.findByEmail(email);

                if (user == null) {
                    showMessage("❌ Aucun compte trouvé avec cet email", "error");
                    return;
                }
                if (!user.isActive()) {
                    showMessage("❌ Compte bloqué. Contactez l'administrateur.", "error");
                    return;
                }
                if (!FaceRecognitionService.hasFaceEnrolled(user.getId())) {
                    showMessage("⚠️ Aucun visage enregistré. Activez Face ID dans votre profil.", "error");
                    return;
                }

                Platform.runLater(() -> openFaceIdWindow(event, user));

            } catch (SQLException e) {
                showMessage("❌ Erreur : " + e.getMessage(), "error");
            }
        }).start();
    }

    private void openFaceIdWindow(ActionEvent event, User user) {
        Stage stage = new Stage();
        stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        stage.setTitle("🔐 BoostUp — Face ID");
        stage.setResizable(false);

        // ── Camera preview ──
        ImageView cameraView = new ImageView();
        cameraView.setFitWidth(420);
        cameraView.setFitHeight(315);
        cameraView.setPreserveRatio(false);
        cameraView.setSmooth(true);

        // Scan line overlay
        javafx.scene.shape.Line scanLine = new javafx.scene.shape.Line(0, 0, 420, 0);
        scanLine.setStroke(javafx.scene.paint.Color.web("#2ecc71"));
        scanLine.setStrokeWidth(2);
        scanLine.setOpacity(0.8);
        scanLine.setEffect(new javafx.scene.effect.Glow(0.8));

        // Face guide oval
        javafx.scene.shape.Ellipse faceGuide = new javafx.scene.shape.Ellipse(110, 130);
        faceGuide.setFill(javafx.scene.paint.Color.TRANSPARENT);
        faceGuide.setStroke(javafx.scene.paint.Color.web("#2ecc71"));
        faceGuide.setStrokeWidth(2.5);
        faceGuide.getStrokeDashArray().addAll(12.0, 6.0);
        faceGuide.setOpacity(0);

        // Corner brackets
        javafx.scene.Group cornerBrackets = createCornerBrackets(420, 315);
        cornerBrackets.setOpacity(0);

        StackPane cameraStack = new StackPane(cameraView, scanLine, faceGuide, cornerBrackets);
        cameraStack.setStyle("-fx-background-color: #0a0a0a; -fx-background-radius: 16;");
        cameraStack.setPrefSize(420, 315);
        cameraStack.setMaxSize(420, 315);
        javafx.scene.shape.Rectangle cameraClip = new javafx.scene.shape.Rectangle(420, 315);
        cameraClip.setArcWidth(16);
        cameraClip.setArcHeight(16);
        cameraStack.setClip(cameraClip);

        // ── Status labels ──
        Label titleLabel = new Label("🔐 Face ID — Reconnaissance Faciale");
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: #1a1a2e;");

        Label subtitleLabel = new Label("Positionnez votre visage dans l'ovale");
        subtitleLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #adb5bd;");

        Label statusLabel = new Label("Initialisation de la caméra...");
        statusLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #6c757d;");

        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(420);
        progressBar.setPrefHeight(5);
        progressBar.setStyle("-fx-accent: #2ecc71;");
        progressBar.setVisible(false);

        Label resultLabel = new Label("");
        resultLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 800;");
        resultLabel.setVisible(false);

        Label scoreLabel = new Label("");
        scoreLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #adb5bd;");
        scoreLabel.setVisible(false);

        // ── Cancel button ──
        Button cancelBtn = new Button("✕ Annuler");
        cancelBtn.setStyle("-fx-background-color: rgba(239,68,68,0.1); -fx-text-fill: #ef4444; " +
                "-fx-font-weight: 600; -fx-background-radius: 10; -fx-padding: 10 28; -fx-cursor: hand; " +
                "-fx-border-color: rgba(239,68,68,0.3); -fx-border-radius: 10; -fx-font-size: 12px;");

        // ── Layout ──
        VBox centerBox = new VBox(10, titleLabel, subtitleLabel, cameraStack,
                progressBar, statusLabel, resultLabel, scoreLabel, cancelBtn);
        centerBox.setAlignment(javafx.geometry.Pos.CENTER);
        centerBox.setStyle("-fx-padding: 25 35; -fx-background-color: white;");

        javafx.scene.Scene scene = new javafx.scene.Scene(centerBox, 500, 530);
        stage.setScene(scene);

        // ── Webcam ──
        final java.util.concurrent.atomic.AtomicBoolean running = new java.util.concurrent.atomic.AtomicBoolean(true);
        final java.util.concurrent.atomic.AtomicReference<com.github.sarxos.webcam.Webcam> webcamRef =
                new java.util.concurrent.atomic.AtomicReference<>(null);

        Thread webcamThread = new Thread(() -> {
            com.github.sarxos.webcam.Webcam webcam = null;
            try {
                Platform.runLater(() -> statusLabel.setText("📸 Recherche de caméra..."));

                webcam = com.github.sarxos.webcam.Webcam.getDefault();
                if (webcam == null) {
                    Platform.runLater(() -> {
                        statusLabel.setText("❌ Aucune caméra détectée");
                        statusLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #ef4444;");
                    });
                    return;
                }
                webcamRef.set(webcam);

                webcam.setViewSize(new java.awt.Dimension(640, 480));

                Platform.runLater(() -> statusLabel.setText("📸 Ouverture de la caméra..."));

                webcam.open();

                // Warm up — first frames are often black
                for (int i = 0; i < 10 && running.get(); i++) {
                    java.awt.image.BufferedImage warmup = webcam.getImage();
                    if (warmup != null) break;
                    Thread.sleep(100);
                }

                Platform.runLater(() -> {
                    statusLabel.setText("📸 Positionnez votre visage dans l'ovale");
                    faceGuide.setOpacity(0.8);
                    cornerBrackets.setOpacity(0.7);
                });

                Platform.runLater(() -> startScanAnimation(scanLine, 315));

                long startTime = System.currentTimeMillis();
                final long SCAN_DELAY = 4500;

                while (running.get()) {
                    if (!webcam.isOpen()) break;
                    java.awt.image.BufferedImage frame = webcam.getImage();
                    if (frame == null) { Thread.sleep(50); continue; }

                    // Convert BufferedImage → JavaFX Image for display only
                    java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                    javax.imageio.ImageIO.write(frame, "png", baos);
                    byte[] bytes = baos.toByteArray();
                    Image fxImage = new Image(new java.io.ByteArrayInputStream(bytes));
                    Platform.runLater(() -> cameraView.setImage(fxImage));

                    long elapsed = System.currentTimeMillis() - startTime;
                    double progress = Math.min(1.0, (double) elapsed / SCAN_DELAY);

                    Platform.runLater(() -> {
                        progressBar.setVisible(true);
                        progressBar.setProgress(progress);

                        if (progress < 0.25) {
                            statusLabel.setText("📸 Détection du visage...");
                            faceGuide.setStroke(javafx.scene.paint.Color.web("#2ecc71"));
                        } else if (progress < 0.55) {
                            statusLabel.setText("🔍 Analyse des traits du visage...");
                            faceGuide.setStroke(javafx.scene.paint.Color.web("#f59e0b"));
                        } else if (progress < 0.85) {
                            statusLabel.setText("🧠 Comparaison AI multi-algorithme...");
                            faceGuide.setStroke(javafx.scene.paint.Color.web("#0d6efd"));
                        } else {
                            statusLabel.setText("⚙️ Calcul du score de confiance...");
                            faceGuide.setStroke(javafx.scene.paint.Color.web("#6f42c1"));
                        }
                    });

                    if (elapsed >= SCAN_DELAY) {
                        running.set(false);

                        // Save captured frame as PNG
                        String tempPath = System.getProperty("java.io.tmpdir") +
                                File.separator + "boostup_face_capture.png";
                        File capturedFile = new File(tempPath);
                        javax.imageio.ImageIO.write(frame, "png", capturedFile);

                        Platform.runLater(() -> {
                            statusLabel.setText("🧠 Envoi au serveur Face++ AI...");
                            statusLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #6f42c1;");
                            progressBar.setProgress(-1);
                        });

                        // Compare directly with file — skip JavaFX conversion
                        double score = FaceRecognitionService.compareFacesFromFile(user.getId(), capturedFile);

                        System.out.println("Face++ score: " + score);

                        boolean noFace = (score == -1);
                        boolean apiError = (score == -2);
                        boolean match = (score >= 62.0);
                        int pct = (score >= 0) ? (int) Math.round(score) : 0;

                        Platform.runLater(() -> {
                            progressBar.setVisible(false);
                            resultLabel.setVisible(true);
                            scoreLabel.setVisible(true);

                            if (noFace || apiError) {
                                faceGuide.setStroke(javafx.scene.paint.Color.web("#ef4444"));
                                faceGuide.getStrokeDashArray().clear();
                                faceGuide.setStrokeWidth(3);

                                if (noFace) {
                                    statusLabel.setText("❌ Aucun visage détecté");
                                    resultLabel.setText("❌ AUCUN VISAGE DÉTECTÉ");
                                    scoreLabel.setText("Assurez-vous que votre visage est bien visible et éclairé");
                                } else {
                                    statusLabel.setText("❌ Erreur de vérification");
                                    resultLabel.setText("❌ ERREUR DE CONNEXION AI");
                                    scoreLabel.setText("Vérifiez votre connexion internet et réessayez");
                                }
                                statusLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #ef4444;");
                                resultLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: #ef4444;");
                                scoreLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #ef4444;");

                                javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                                        javafx.util.Duration.millis(2500));
                                pause.setOnFinished(ev -> {
                                    stage.close();
                                    handleFaceResult(event, user, false);
                                });
                                pause.play();

                            } else if (match) {
                                faceGuide.setStroke(javafx.scene.paint.Color.web("#2ecc71"));
                                faceGuide.getStrokeDashArray().clear();
                                faceGuide.setStrokeWidth(3);
                                statusLabel.setText("✅ Identité confirmée par Face++ AI");
                                statusLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #2ecc71;");
                                resultLabel.setText("✅ AUTHENTIFICATION RÉUSSIE");
                                resultLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: #2ecc71;");
                                scoreLabel.setText("Confiance AI : " + pct + "%");
                                scoreLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #2ecc71;");

                                javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                                        javafx.util.Duration.millis(1500));
                                pause.setOnFinished(ev -> {
                                    stage.close();
                                    handleFaceResult(event, user, true);
                                });
                                pause.play();

                            } else {
                                faceGuide.setStroke(javafx.scene.paint.Color.web("#ef4444"));
                                faceGuide.getStrokeDashArray().clear();
                                faceGuide.setStrokeWidth(3);
                                statusLabel.setText("❌ Visage non reconnu par Face++ AI");
                                statusLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #ef4444;");
                                resultLabel.setText("❌ ÉCHEC — Visage différent");
                                resultLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: #ef4444;");
                                scoreLabel.setText("Confiance AI : " + pct + "% — Seuil requis : 62%");
                                scoreLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #ef4444;");

                                cancelBtn.setText("Fermer");

                                javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                                        javafx.util.Duration.millis(2500));
                                pause.setOnFinished(ev -> {
                                    stage.close();
                                    handleFaceResult(event, user, false);
                                });
                                pause.play();
                            }
                        });
                        break;
                    }
                    Thread.sleep(70);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    statusLabel.setText("❌ Erreur caméra: " + e.getMessage());
                    statusLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #ef4444;");
                });
            } finally {
                if (webcam != null && webcam.isOpen()) {
                    webcam.close();
                }
            }
        });
        webcamThread.setDaemon(true);

        cancelBtn.setOnAction(e -> {
            running.set(false);
            com.github.sarxos.webcam.Webcam wc = webcamRef.get();
            if (wc != null && wc.isOpen()) wc.close();
            stage.close();
            showMessage("⚠️ Authentification faciale annulée", "error");
        });

        stage.setOnCloseRequest(e -> {
            running.set(false);
            com.github.sarxos.webcam.Webcam wc = webcamRef.get();
            if (wc != null && wc.isOpen()) wc.close();
        });

        webcamThread.start();
        stage.showAndWait();
    }

    private void startScanAnimation(javafx.scene.shape.Line scanLine, double height) {
        javafx.animation.TranslateTransition scanAnim = new javafx.animation.TranslateTransition(
                javafx.util.Duration.millis(1500), scanLine);
        scanAnim.setFromY(0);
        scanAnim.setToY(height);
        scanAnim.setCycleCount(javafx.animation.Animation.INDEFINITE);
        scanAnim.setAutoReverse(true);
        scanAnim.play();
    }

    private javafx.scene.Group createCornerBrackets(double w, double h) {
        double size = 30, offset = 35;
        javafx.scene.paint.Color c = javafx.scene.paint.Color.web("#2ecc71");

        javafx.scene.shape.Line tl1 = new javafx.scene.shape.Line(offset, offset, offset + size, offset);
        javafx.scene.shape.Line tl2 = new javafx.scene.shape.Line(offset, offset, offset, offset + size);
        javafx.scene.shape.Line tr1 = new javafx.scene.shape.Line(w - offset, offset, w - offset - size, offset);
        javafx.scene.shape.Line tr2 = new javafx.scene.shape.Line(w - offset, offset, w - offset, offset + size);
        javafx.scene.shape.Line bl1 = new javafx.scene.shape.Line(offset, h - offset, offset + size, h - offset);
        javafx.scene.shape.Line bl2 = new javafx.scene.shape.Line(offset, h - offset, offset, h - offset - size);
        javafx.scene.shape.Line br1 = new javafx.scene.shape.Line(w - offset, h - offset, w - offset - size, h - offset);
        javafx.scene.shape.Line br2 = new javafx.scene.shape.Line(w - offset, h - offset, w - offset, h - offset - size);

        for (javafx.scene.shape.Line l : new javafx.scene.shape.Line[]{tl1, tl2, tr1, tr2, bl1, bl2, br1, br2}) {
            l.setStroke(c);
            l.setStrokeWidth(3);
            l.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        }

        return new javafx.scene.Group(tl1, tl2, tr1, tr2, bl1, bl2, br1, br2);
    }

    private void handleFaceResult(ActionEvent event, User user, boolean match) {
        if (match) {
            ActivityLogService.log(user.getId(), user.getEmail(),
                    ActivityLogService.ACTION_FACE_AUTH, "Authentification faciale réussie");
            showMessage("✅ Visage reconnu ! Connexion...", "success");

            new Thread(() -> {
                try { completeLogin(event, user); }
                catch (InterruptedException e) { e.printStackTrace(); }
            }).start();
        } else {
            ActivityLogService.log(user.getId(), user.getEmail(),
                    ActivityLogService.ACTION_FACE_AUTH_FAILED, "Authentification faciale échouée");

            // SMS alerte sécurité désactivé pour économiser les crédits Twilio
            // Décommenter pour activer lors de la présentation :
            if (user.getPhone() != null && !user.getPhone().isEmpty()) {
                String phone = SmsService.formatPhoneNumber(user.getPhone(), "+216");
               SmsService.sendSecurityAlert(phone);
             }
            showMessage("❌ Visage non reconnu. Veuillez réessayer.", "error");
        }
    }

    // ═══════════════════════════════════════════════════════
    // LOGIN PAR SMS OTP (sans mot de passe)//
    // ═══════════════════════════════════════════════════════

    @FXML
    private void handleSmsLogin(ActionEvent event) {
        String email = emailField.getText().trim();

        if (email.isEmpty()) {
            showMessage("⚠️ Veuillez entrer votre email pour le login SMS", "error");
            emailField.requestFocus();
            return;
        }

        loginButton.setDisable(true);
        showMessage("🔄 Vérification en cours...", "success");

        new Thread(() -> {
            try {
                User user = userService.findByEmail(email);

                if (user == null) {
                    showMessage("❌ Aucun compte trouvé avec cet email", "error");
                    return;
                }
                if (!user.isActive()) {
                    showMessage("❌ Compte bloqué. Contactez l'administrateur.", "error");
                    return;
                }
                if (user.getPhone() == null || user.getPhone().isEmpty()) {
                    showMessage("⚠️ Aucun téléphone associé. Ajoutez un numéro dans votre profil.", "error");
                    return;
                }

                pendingOtpUser = user;
                pendingEvent = event;
                String formattedPhone = SmsService.formatPhoneNumber(user.getPhone(), "+216");
                currentOtp = SmsService.sendOtp(formattedPhone);

                ActivityLogService.log(user.getId(), email, ActivityLogService.ACTION_SMS_SENT,
                        "Code OTP envoyé par login SMS à " + maskPhone(formattedPhone));

                final String otpToShow = currentOtp;
                Platform.runLater(() -> showOtpUi(formattedPhone, otpToShow));

            } catch (SQLException e) {
                showMessage("❌ Erreur : " + e.getMessage(), "error");
            } finally {
                Platform.runLater(() -> loginButton.setDisable(false));
            }
        }).start();
    }

    // ═══════════════════════════════════════════════════════
    // MÉTHODES COMMUNES
    // ═══════════════════════════════════════════════════════

    private void completeLogin(ActionEvent event, User user) throws InterruptedException {
        SessionManager.setCurrentUser(user);

        ActivityLogService.log(user.getId(), user.getEmail(),
                ActivityLogService.ACTION_LOGIN,
                "Connexion réussie - Rôle: " + user.getRole().name());

        showMessage("✅ Connexion réussie ! Redirection...", "success");
        Thread.sleep(1000);
        Platform.runLater(() -> navigateBasedOnRole(event, user));
    }

    private void navigateBasedOnRole(ActionEvent event, User user) {
        String fxmlPath = user.getRole() == Role_enum.ADMIN ? "/fxml/admin-dashboard.fxml" : "/fxml/homepage.fxml";
        String title = user.getRole() == Role_enum.ADMIN ? "Tableau de bord Admin" : "Accueil";

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        NavigationHelper.navigateTo(stage, fxmlPath, title);
    }

    @FXML
    private void goToSignup(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        NavigationHelper.navigateTo(stage, "/fxml/signup.fxml", "Inscription");
    }

    @FXML
    private void goToForgotPassword(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        NavigationHelper.navigateTo(stage, "/fxml/forgot-password.fxml", "Mot de passe oublié");
    }

    @FXML
    private void toggleTheme(ActionEvent event) {
        ThemeHelper.toggleTheme(themeToggle);
    }

    // ═══════════════════════════════════════════════════════
    // SOCIAL LOGIN — Google OAuth2
    // ═══════════════════════════════════════════════════════

    @FXML
    private void handleGoogleLogin(ActionEvent event) {
        if (!GoogleOAuthService.isConfigured()) {
            // Show setup instructions
            showMessage("Configuration Google OAuth requise", "error");
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Configuration Google OAuth");
                alert.setHeaderText("Google OAuth n'est pas encore configure");
                alert.setContentText(
                    "Pour activer le login Google :\n\n" +
                    "1. Allez sur https://console.cloud.google.com/\n" +
                    "2. Creez un projet -> APIs & Services -> Credentials\n" +
                    "3. Creez un OAuth 2.0 Client ID (Web application)\n" +
                    "4. Ajoutez http://localhost:8888/callback comme redirect URI\n" +
                    "5. Copiez le Client ID et Client Secret\n" +
                    "6. Collez-les dans GoogleOAuthService.java\n\n" +
                    "Fichier: services/UtilisateurService/GoogleOAuthService.java"
                );
                alert.showAndWait();
            });
            return;
        }

        showMessage("Ouverture de Google Login...", "success");
        loginButton.setDisable(true);

        GoogleOAuthService.login().thenAccept(googleUser -> {
            Platform.runLater(() -> {
                try {
                    processGoogleUser(event, googleUser);
                } catch (Exception e) {
                    showMessage("Erreur: " + e.getMessage(), "error");
                    loginButton.setDisable(false);
                }
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                showMessage("Connexion Google echouee: " + ex.getMessage(), "error");
                loginButton.setDisable(false);
            });
            return null;
        });
    }

    /**
     * Process the Google user info: find or create the user, then login.
     */
    private void processGoogleUser(ActionEvent event, GoogleOAuthService.GoogleUserInfo googleUser) throws Exception {
        if (googleUser == null || googleUser.email == null) {
            showMessage("Impossible de recuperer l'email Google", "error");
            loginButton.setDisable(false);
            return;
        }

        new Thread(() -> {
            try {
                User user = userService.findByEmail(googleUser.email);

                // Extract clean username from email (before @, remove numbers)
                String emailPrefix = googleUser.email.split("@")[0];
                String cleanName = emailPrefix.replaceAll("[0-9_.]", " ").trim();
                if (cleanName.isEmpty()) cleanName = emailPrefix;

                if (user == null) {
                    // Create new user from Google account
                    String password = org.mindrot.jbcrypt.BCrypt.hashpw(
                            "google_" + googleUser.id + "_" + System.currentTimeMillis(),
                            org.mindrot.jbcrypt.BCrypt.gensalt());

                    user = new User();
                    user.setNom(cleanName);
                    user.setEmail(googleUser.email);
                    user.setMDP(password);
                    user.setRole(Role_enum.INVESTISSEUR);
                    user.setActive(true);
                    user.setFullname(cleanName);
                    user.setAvatar(googleUser.picture);

                    userService.ajouter(user);
                    user = userService.findByEmail(googleUser.email);

                    ActivityLogService.log(user.getId(), user.getEmail(),
                            ActivityLogService.ACTION_LOGIN,
                            "Nouveau compte cree via Google OAuth");

                    System.out.println("New user created from Google: " + googleUser.email + " -> name: " + cleanName);
                } else if (!user.isActive()) {
                    showMessage("Compte bloque. Contactez l'administrateur.", "error");
                    Platform.runLater(() -> loginButton.setDisable(false));
                    return;
                } else {
                    // Existing user — update nom + fullname + avatar
                    boolean updated = false;
                    if (!cleanName.equals(user.getNom())) {
                        user.setNom(cleanName);
                        updated = true;
                    }
                    if (user.getFullname() == null || !cleanName.equals(user.getFullname())) {
                        user.setFullname(cleanName);
                        updated = true;
                    }
                    if (googleUser.picture != null && !googleUser.picture.equals(user.getAvatar())) {
                        user.setAvatar(googleUser.picture);
                        updated = true;
                    }
                    if (updated) {
                        userService.update(user);
                    }
                }

                // Complete login
                completeLogin(event, user);

            } catch (Exception e) {
                e.printStackTrace();
                showMessage("Erreur: " + e.getMessage(), "error");
                Platform.runLater(() -> loginButton.setDisable(false));
            }
        }).start();
    }

    // ═══════════════════════════════════════════════════════
    // SOCIAL LOGIN — GitHub OAuth2
    // ═══════════════════════════════════════════════════════

    @FXML
    private void handleGitHubLogin(ActionEvent event) {
        if (!GitHubOAuthService.isConfigured()) {
            showMessage("Configuration GitHub OAuth requise", "error");
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Configuration GitHub OAuth");
                alert.setHeaderText("GitHub OAuth n'est pas encore configure");
                alert.setContentText(
                    "Pour activer le login GitHub :\n\n" +
                    "1. Allez sur https://github.com/settings/developers\n" +
                    "2. Cliquez 'New OAuth App'\n" +
                    "3. Remplissez :\n" +
                    "   - Application name: BoostUp\n" +
                    "   - Homepage URL: http://localhost\n" +
                    "   - Callback URL: http://127.0.0.1/callback\n" +
                    "4. Copiez Client ID et generez Client Secret\n" +
                    "5. Collez-les dans GitHubOAuthService.java\n\n" +
                    "Fichier: services/UtilisateurService/GitHubOAuthService.java"
                );
                alert.showAndWait();
            });
            return;
        }

        showMessage("Ouverture de GitHub Login...", "success");
        loginButton.setDisable(true);

        GitHubOAuthService.login().thenAccept(ghUser -> {
            Platform.runLater(() -> {
                try {
                    processGitHubUser(event, ghUser);
                } catch (Exception e) {
                    showMessage("Erreur: " + e.getMessage(), "error");
                    loginButton.setDisable(false);
                }
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                showMessage("Connexion GitHub echouee: " + ex.getMessage(), "error");
                loginButton.setDisable(false);
            });
            return null;
        });
    }

    /**
     * Process the GitHub user info: find or create the user, then login.
     */
    private void processGitHubUser(ActionEvent event, GitHubOAuthService.GitHubUserInfo ghUser) throws Exception {
        if (ghUser == null || ghUser.email == null) {
            showMessage("Impossible de recuperer l'email GitHub", "error");
            loginButton.setDisable(false);
            return;
        }

        new Thread(() -> {
            try {
                User user = userService.findByEmail(ghUser.email);

                // Use GitHub login (username) as the clean name
                String cleanName = ghUser.login != null ? ghUser.login : ghUser.email.split("@")[0].replaceAll("[0-9_.]", " ").trim();
                if (cleanName.isEmpty()) cleanName = ghUser.email.split("@")[0];

                if (user == null) {
                    String password = org.mindrot.jbcrypt.BCrypt.hashpw(
                            "github_" + ghUser.id + "_" + System.currentTimeMillis(),
                            org.mindrot.jbcrypt.BCrypt.gensalt());

                    user = new User();
                    user.setNom(cleanName);
                    user.setEmail(ghUser.email);
                    user.setMDP(password);
                    user.setRole(Role_enum.INVESTISSEUR);
                    user.setActive(true);
                    user.setFullname(cleanName);
                    user.setAvatar(ghUser.avatarUrl);

                    userService.ajouter(user);
                    user = userService.findByEmail(ghUser.email);

                    ActivityLogService.log(user.getId(), user.getEmail(),
                            ActivityLogService.ACTION_LOGIN,
                            "Nouveau compte cree via GitHub OAuth");

                    System.out.println("New user created from GitHub: " + ghUser.email + " -> name: " + cleanName);
                } else if (!user.isActive()) {
                    showMessage("Compte bloque. Contactez l'administrateur.", "error");
                    Platform.runLater(() -> loginButton.setDisable(false));
                    return;
                } else {
                    boolean updated = false;
                    if (!cleanName.equals(user.getNom())) {
                        user.setNom(cleanName);
                        updated = true;
                    }
                    if (user.getFullname() == null || !cleanName.equals(user.getFullname())) {
                        user.setFullname(cleanName);
                        updated = true;
                    }
                    if (ghUser.avatarUrl != null && !ghUser.avatarUrl.equals(user.getAvatar())) {
                        user.setAvatar(ghUser.avatarUrl);
                        updated = true;
                    }
                    if (updated) {
                        userService.update(user);
                    }
                }

                completeLogin(event, user);

            } catch (Exception e) {
                e.printStackTrace();
                showMessage("Erreur: " + e.getMessage(), "error");
                Platform.runLater(() -> loginButton.setDisable(false));
            }
        }).start();
    }

    /**
     * Affiche la zone OTP et, si le SMS n'a pas été livré, montre le code dans une popup.
     */
    private void showOtpUi(String formattedPhone, String otp) {
        otpBox.setVisible(true);
        otpBox.setManaged(true);
        otpField.requestFocus();

        if (!SmsService.didLastSendSucceed()) {
            showMessage("📱 Code envoyé à " + maskPhone(formattedPhone) + "  (voir popup)", "success");

            String reason;
            if (!SmsService.isConfigured()) {
                reason = "Twilio n'est pas encore configuré.\n" +
                        "Collez votre numéro Twilio trial dans SmsService.TWILIO_FROM\n\n";
            } else {
                reason = "Le SMS n'a pas pu être livré (vérifiez la console).\n\n";
            }

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("📱 Code OTP");
            alert.setHeaderText("Votre code de vérification");
            alert.setContentText(reason +
                    "Votre code OTP est :  " + otp + "\n\n" +
                    "Copiez-le dans le champ de vérification.");
            alert.showAndWait();
        } else {
            showMessage("📱 Code envoyé par SMS à " + maskPhone(formattedPhone), "success");
        }
    }

    private void showMessage(String text, String type) {
        Platform.runLater(() -> {
            messageLabel.setText(text);
            messageLabel.getStyleClass().removeAll("success", "error");
            messageLabel.getStyleClass().add(type);
            messageLabel.setVisible(true);
            messageLabel.setManaged(true);
        });
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 8) return phone;
        return phone.substring(0, 4) + "****" + phone.substring(phone.length() - 4);
    }

    private void loadLogo() {
        if (logoImageView == null) return;

        try {
            java.io.InputStream imageStream = getClass().getResourceAsStream("/images/logo.png");
            if (imageStream == null) {
                imageStream = getClass().getResourceAsStream("boostup-logo.png");
            }

            if (imageStream != null) {
                Image logo = new Image(imageStream);
                if (!logo.isError()) {
                    logoImageView.setImage(logo);
                    logoImageView.setPreserveRatio(true);
                    logoImageView.setFitWidth(120);
                    logoImageView.setFitHeight(120);
                    logoImageView.setSmooth(true);
                    logoImageView.setVisible(true);

                    Platform.runLater(() -> {
                        if (logoImageView.getParent() instanceof StackPane parent) {
                            if (parent.getChildren().size() > 1) {
                                parent.getChildren().get(1).setVisible(false);
                                parent.getChildren().get(1).setManaged(false);
                            }
                        }
                    });
                    return;
                }
            }

            showRocketFallback();
        } catch (Exception e) {
            System.err.println("Could not load logo: " + e.getMessage());
            showRocketFallback();
        }
    }

    private void showRocketFallback() {
        Platform.runLater(() -> {
            if (logoImageView != null) {
                logoImageView.setVisible(false);
                if (logoImageView.getParent() instanceof StackPane parent) {
                    if (parent.getChildren().size() > 1) {
                        parent.getChildren().get(1).setVisible(true);
                        parent.getChildren().get(1).setManaged(true);
                    }
                }
            }
        });
    }
}

