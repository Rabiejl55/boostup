package utils;

import javafx.animation.AnimationTimer;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 🎮 MINI-JEU SNAKE - Challenge pour événements complets
 *
 * Un jeu Snake élégant et fun pour donner une chance aux utilisateurs
 * de s'inscrire à un événement complet s'ils sont doués !
 */
public class SnakeGameChallenge {

    private static final int TILE_SIZE = 20;
    private static final int WIDTH = 20;
    private static final int HEIGHT = 20;
    private static final int CANVAS_WIDTH = WIDTH * TILE_SIZE;
    private static final int CANVAS_HEIGHT = HEIGHT * TILE_SIZE;

    private boolean gameWon = false;
    private boolean gameOver = false;

    /**
     * Lance le défi Snake et retourne true si l'utilisateur gagne
     */
    public boolean showChallenge(Stage owner) {
        // Message de défi
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.initOwner(owner);
        confirmAlert.setTitle("🎯 Événement Complet - Défi Spécial");
        confirmAlert.setHeaderText("Cet événement est COMPLET (250/250) !");
        confirmAlert.setContentText(
            "💡 Mais si tu es doué, tu peux encore t'inscrire !\n\n" +
            "🎮 Relève le défi Snake :\n" +
            "   • Mange 5 pommes pour gagner\n" +
            "   • Utilise les flèches pour te déplacer\n" +
            "   • Ne te mords pas la queue !\n\n" +
            "Es-tu prêt à relever le défi ?"
        );

        ButtonType btnEssayer = new ButtonType("🎮 Essayer");
        ButtonType btnAnnuler = new ButtonType("❌ Annuler");

        confirmAlert.getButtonTypes().setAll(btnEssayer, btnAnnuler);

        java.util.Optional<ButtonType> result = confirmAlert.showAndWait();

        if (result.isPresent() && result.get() == btnEssayer) {
            // Lancer le jeu
            return playSnakeGame(owner);
        }

        return false; // L'utilisateur a annulé
    }

    /**
     * Lance le jeu Snake et retourne true si victoire
     */
    private boolean playSnakeGame(Stage owner) {
        Stage gameStage = new Stage();
        gameStage.initModality(Modality.APPLICATION_MODAL);
        gameStage.initOwner(owner);
        gameStage.setTitle("🐍 Snake Challenge - Mange 5 pommes pour gagner !");
        gameStage.setResizable(false);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #1b2a4a, #2d1b4e);");

        // Header
        VBox header = new VBox(10);
        header.setAlignment(Pos.CENTER);
        header.setStyle("-fx-padding: 15; -fx-background-color: rgba(255,255,255,0.1);");

        Label title = new Label("🐍 SNAKE CHALLENGE");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");

        Label scoreLabel = new Label("🍎 Pommes: 0/5");
        scoreLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 18px; -fx-font-weight: bold;");

        Label instructionsLabel = new Label("Utilise les FLÈCHES pour te déplacer");
        instructionsLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.7); -fx-font-size: 12px;");

        header.getChildren().addAll(title, scoreLabel, instructionsLabel);

        // Canvas pour le jeu
        Canvas canvas = new Canvas(CANVAS_WIDTH, CANVAS_HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        VBox canvasContainer = new VBox();
        canvasContainer.setAlignment(Pos.CENTER);
        canvasContainer.setStyle("-fx-padding: 20;");
        canvasContainer.getChildren().add(canvas);

        root.setTop(header);
        root.setCenter(canvasContainer);

        Scene scene = new Scene(root);
        gameStage.setScene(scene);

        // Logique du jeu
        SnakeGame game = new SnakeGame(gc, scoreLabel, gameStage);

        scene.setOnKeyPressed(e -> {
            if (!game.isGameOver()) {
                game.handleKeyPress(e.getCode());
            }
        });

        game.start();

        gameStage.showAndWait();

        return game.isWon();
    }

    /**
     * Classe interne pour la logique du jeu Snake
     */
    private static class SnakeGame extends AnimationTimer {
        private final GraphicsContext gc;
        private final Label scoreLabel;
        private final Stage stage;

        private List<Point> snake = new ArrayList<>();
        private Point food;
        private Direction direction = Direction.RIGHT;
        private Direction nextDirection = Direction.RIGHT;

        private int score = 0;
        private final int TARGET_SCORE = 5;

        private boolean gameOver = false;
        private boolean won = false;

        private long lastUpdate = 0;
        private final long SPEED = 150_000_000; // 150ms

        private Random random = new Random();

        public SnakeGame(GraphicsContext gc, Label scoreLabel, Stage stage) {
            this.gc = gc;
            this.scoreLabel = scoreLabel;
            this.stage = stage;

            // Initialiser le serpent au centre
            snake.add(new Point(WIDTH / 2, HEIGHT / 2));
            snake.add(new Point(WIDTH / 2 - 1, HEIGHT / 2));
            snake.add(new Point(WIDTH / 2 - 2, HEIGHT / 2));

            // Placer la première pomme
            spawnFood();
        }

        @Override
        public void handle(long now) {
            if (gameOver) {
                stop();
                return;
            }

            if (now - lastUpdate < SPEED) {
                return;
            }

            lastUpdate = now;

            // Mettre à jour la direction
            direction = nextDirection;

            // Calculer la nouvelle tête
            Point head = snake.get(0);
            Point newHead = new Point(head.x, head.y);

            switch (direction) {
                case UP: newHead.y--; break;
                case DOWN: newHead.y++; break;
                case LEFT: newHead.x--; break;
                case RIGHT: newHead.x++; break;
            }

            // Vérifier collision avec les murs
            if (newHead.x < 0 || newHead.x >= WIDTH || newHead.y < 0 || newHead.y >= HEIGHT) {
                endGame(false);
                return;
            }

            // Vérifier collision avec le corps
            for (Point p : snake) {
                if (p.equals(newHead)) {
                    endGame(false);
                    return;
                }
            }

            // Ajouter la nouvelle tête
            snake.add(0, newHead);

            // Vérifier si le serpent mange la nourriture
            if (newHead.equals(food)) {
                score++;
                scoreLabel.setText("🍎 Pommes: " + score + "/" + TARGET_SCORE);

                if (score >= TARGET_SCORE) {
                    endGame(true);
                    return;
                }

                spawnFood();
            } else {
                // Retirer la queue si pas de nourriture mangée
                snake.remove(snake.size() - 1);
            }

            // Dessiner
            draw();
        }

        private void draw() {
            // Fond
            gc.setFill(Color.rgb(26, 42, 74));
            gc.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);

            // Grille
            gc.setStroke(Color.rgb(40, 60, 100));
            gc.setLineWidth(0.5);
            for (int i = 0; i <= WIDTH; i++) {
                gc.strokeLine(i * TILE_SIZE, 0, i * TILE_SIZE, CANVAS_HEIGHT);
            }
            for (int i = 0; i <= HEIGHT; i++) {
                gc.strokeLine(0, i * TILE_SIZE, CANVAS_WIDTH, i * TILE_SIZE);
            }

            // Nourriture (pomme rouge brillante)
            gc.setFill(Color.rgb(231, 76, 60));
            gc.fillOval(food.x * TILE_SIZE + 2, food.y * TILE_SIZE + 2, TILE_SIZE - 4, TILE_SIZE - 4);
            gc.setFill(Color.rgb(192, 57, 43));
            gc.fillOval(food.x * TILE_SIZE + 6, food.y * TILE_SIZE + 4, 4, 4);

            // Serpent (gradient vert)
            for (int i = 0; i < snake.size(); i++) {
                Point p = snake.get(i);

                if (i == 0) {
                    // Tête (vert clair)
                    gc.setFill(Color.rgb(46, 204, 113));
                } else {
                    // Corps (gradient)
                    double alpha = 1.0 - (i / (double) snake.size()) * 0.5;
                    gc.setFill(Color.rgb(39, 174, 96, alpha));
                }

                gc.fillRoundRect(p.x * TILE_SIZE + 1, p.y * TILE_SIZE + 1,
                               TILE_SIZE - 2, TILE_SIZE - 2, 5, 5);
            }
        }

        private void spawnFood() {
            do {
                food = new Point(random.nextInt(WIDTH), random.nextInt(HEIGHT));
            } while (snake.contains(food));
        }

        public void handleKeyPress(KeyCode keyCode) {
            switch (keyCode) {
                case UP:
                case Z:
                    if (direction != Direction.DOWN) nextDirection = Direction.UP;
                    break;
                case DOWN:
                case S:
                    if (direction != Direction.UP) nextDirection = Direction.DOWN;
                    break;
                case LEFT:
                case Q:
                    if (direction != Direction.RIGHT) nextDirection = Direction.LEFT;
                    break;
                case RIGHT:
                case D:
                    if (direction != Direction.LEFT) nextDirection = Direction.RIGHT;
                    break;
            }
        }

        private void endGame(boolean victory) {
            gameOver = true;
            won = victory;
            stop();

            javafx.application.Platform.runLater(() -> {
                stage.close();

                Alert alert = new Alert(victory ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING);

                if (victory) {
                    alert.setTitle("🏆 VICTOIRE !");
                    alert.setHeaderText("FÉLICITATIONS ! 🎉");
                    alert.setContentText(
                        "Tu as réussi le défi Snake !\n\n" +
                        "🎯 Score : " + score + "/" + TARGET_SCORE + "\n" +
                        "✅ Tu peux maintenant t'inscrire à l'événement complet !\n\n" +
                        "Bravo champion ! 🏆"
                    );
                } else {
                    alert.setTitle("😢 Défaite");
                    alert.setHeaderText("Hard Luck!");
                    alert.setContentText(
                        "Dommage ! Tu t'es fait mordre la queue...\n\n" +
                        "🎯 Score final : " + score + "/" + TARGET_SCORE + "\n" +
                        "❌ Tu ne peux pas t'inscrire pour le moment.\n\n" +
                        "Retente ta chance plus tard ! 💪"
                    );
                }

                alert.showAndWait();
            });
        }

        public boolean isGameOver() {
            return gameOver;
        }

        public boolean isWon() {
            return won;
        }
    }

    /**
     * Classe Point pour les coordonnées
     */
    private static class Point {
        int x, y;

        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Point) {
                Point p = (Point) obj;
                return p.x == x && p.y == y;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return x * 1000 + y;
        }
    }

    /**
     * Énumération des directions
     */
    private enum Direction {
        UP, DOWN, LEFT, RIGHT
    }
}



