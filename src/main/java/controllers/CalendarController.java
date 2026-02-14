package controllers;

import entities.GEvenement.EvenementFX;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.net.URL;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;

/**
 * Calendrier premium: vue mensuelle avec pills événements.
 * Il s'affiche en overlay depuis FrontEvenements.
 */
public class CalendarController implements Initializable {

    @FXML private Label lblMonth;
    @FXML private GridPane gridDays;

    private Runnable onClose;

    private YearMonth currentMonth = YearMonth.now();

    private final List<String> palette = List.of(
            "#3b82f6", // bleu
            "#8b5cf6", // violet
            "#fb7185", // rose doux
            "#06b6d4", // cyan
            "#fb923c"  // orange pastel
    );

    /** événements par date */
    private final Map<LocalDate, List<EvenementFX>> eventsByDate = new HashMap<>();

    /** callback: ouvrir détail */
    private java.util.function.Consumer<EvenementFX> onOpenDetail;

    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    public void setOnOpenDetail(java.util.function.Consumer<EvenementFX> onOpenDetail) {
        this.onOpenDetail = onOpenDetail;
    }

    /** Passe les événements du front */
    public void setEvents(List<EvenementFX> events) {
        eventsByDate.clear();
        if (events == null) {
            renderMonth(false);
            return;
        }

        for (EvenementFX e : events) {
            if (e == null || e.getDateEvenement() == null) continue;
            LocalDate d = e.getDateEvenement().toLocalDate();
            eventsByDate.computeIfAbsent(d, k -> new ArrayList<>()).add(e);
        }

        // tri stable par titre
        for (var list : eventsByDate.values()) {
            list.sort(Comparator.comparing(EvenementFX::getTitre, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        }

        renderMonth(false);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        renderMonth(false);
    }

    @FXML
    private void handleClose() {
        if (onClose != null) onClose.run();
    }

    @FXML
    private void handlePrevMonth() {
        animateMonthChange(currentMonth.minusMonths(1), -1);
    }

    @FXML
    private void handleNextMonth() {
        animateMonthChange(currentMonth.plusMonths(1), +1);
    }

    private void animateMonthChange(YearMonth target, int direction) {
        if (gridDays == null) return;

        TranslateTransition out = new TranslateTransition(Duration.millis(220), gridDays);
        out.setToX(-direction * 80);
        FadeTransition fout = new FadeTransition(Duration.millis(220), gridDays);
        fout.setToValue(0);

        ParallelTransition ptOut = new ParallelTransition(out, fout);
        ptOut.setInterpolator(Interpolator.EASE_BOTH);
        ptOut.setOnFinished(e -> {
            currentMonth = target;
            renderMonth(true);

            gridDays.setTranslateX(direction * 80);
            gridDays.setOpacity(0);

            TranslateTransition in = new TranslateTransition(Duration.millis(260), gridDays);
            in.setToX(0);
            FadeTransition fin = new FadeTransition(Duration.millis(260), gridDays);
            fin.setToValue(1);

            ParallelTransition ptIn = new ParallelTransition(in, fin);
            ptIn.setInterpolator(Interpolator.EASE_BOTH);
            ptIn.play();
        });

        ptOut.play();
    }

    private void renderMonth(boolean animatedTitle) {
        if (gridDays == null || lblMonth == null) return;

        gridDays.getChildren().clear();

        String monthName = currentMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH);
        lblMonth.setText(cap(monthName) + " " + currentMonth.getYear());

        if (animatedTitle) {
            ScaleTransition st = new ScaleTransition(Duration.millis(180), lblMonth);
            st.setFromX(0.98);
            st.setFromY(0.98);
            st.setToX(1.0);
            st.setToY(1.0);
            st.setInterpolator(Interpolator.EASE_OUT);
            st.play();
        }

        LocalDate first = currentMonth.atDay(1);
        int firstDayIndex = dayIndexMondayFirst(first.getDayOfWeek()); // 0..6
        LocalDate start = first.minusDays(firstDayIndex);

        LocalDate today = LocalDate.now();

        int rows = 6;
        int cols = 7;

        int paletteIndex = 0;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                LocalDate date = start.plusDays(r * 7L + c);

                VBox cell = new VBox();
                cell.getStyleClass().add("day-cell");

                Label dayNum = new Label(String.valueOf(date.getDayOfMonth()));
                dayNum.getStyleClass().add("day-number");

                if (date.equals(today) && date.getMonth() == currentMonth.getMonth()) {
                    cell.getStyleClass().add("day-today");
                }

                if (date.getMonth() != currentMonth.getMonth()) {
                    cell.setOpacity(0.45);
                }

                VBox pillsBox = new VBox();
                pillsBox.setSpacing(6);

                List<EvenementFX> list = eventsByDate.getOrDefault(date, List.of());
                for (int i = 0; i < list.size(); i++) {
                    EvenementFX ev = list.get(i);

                    Label pill = new Label(shortTitle(ev.getTitre()));
                    pill.getStyleClass().add("event-pill");

                    String color = palette.get((paletteIndex + i) % palette.size());
                    pill.setStyle("-fx-background-color: " + color + ";");

                    String lieu = ev.getLieu() == null ? "-" : ev.getLieu();

                    Tooltip.install(pill, new Tooltip(
                            (ev.getTitre() == null ? "Événement" : ev.getTitre()) + "\n" +
                                    "Date: " + date + "\n" +
                                    "Lieu: " + lieu
                    ));

                    pill.setOnMouseEntered(e -> playPillHover(pill, true));
                    pill.setOnMouseExited(e -> playPillHover(pill, false));

                    pill.setOnMouseClicked(e -> {
                        playPillClick(pill);
                        if (onOpenDetail != null) {
                            onOpenDetail.accept(ev);
                        }
                    });

                    pillsBox.getChildren().add(pill);
                }

                cell.getChildren().addAll(dayNum, pillsBox);
                gridDays.add(cell, c, r);

                paletteIndex++;
            }
        }
    }

    private void playPillHover(Node pill, boolean enter) {
        double targetScale = enter ? 1.05 : 1.0;
        double targetY = enter ? -1.0 : 0.0;

        ScaleTransition st = new ScaleTransition(Duration.millis(160), pill);
        st.setToX(targetScale);
        st.setToY(targetScale);
        st.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition tt = new TranslateTransition(Duration.millis(160), pill);
        tt.setToY(targetY);
        tt.setInterpolator(Interpolator.EASE_OUT);

        new ParallelTransition(st, tt).play();
    }

    private void playPillClick(Node pill) {
        ScaleTransition st1 = new ScaleTransition(Duration.millis(110), pill);
        st1.setToX(1.08);
        st1.setToY(1.08);
        st1.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition st2 = new ScaleTransition(Duration.millis(150), pill);
        st2.setToX(1.0);
        st2.setToY(1.0);
        st2.setInterpolator(Interpolator.EASE_BOTH);

        new SequentialTransition(st1, st2).play();
    }

    private int dayIndexMondayFirst(DayOfWeek d) {
        return d.getValue() - 1; // 1=Mon..7=Sun
    }

    private String shortTitle(String s) {
        if (s == null) return "";
        String v = s.trim();
        return v.length() <= 14 ? v : v.substring(0, 13) + "…";
    }

    private String cap(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase(Locale.FRENCH) + s.substring(1);
    }
}
