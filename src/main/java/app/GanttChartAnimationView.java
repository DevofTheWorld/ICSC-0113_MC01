package app;

import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.List;

/**
 * Draws the Round Robin Gantt chart on a Canvas and animates it playing out
 * left-to-right, like a timeline scrubbing through the schedule.
 */
public class GanttChartAnimationView extends VBox {

    private static final double unitWidth = 56.0;
    private static final double barHeight = 70.0;
    private static final double topMargin = 24.0;
    private static final double leftMargin = 6.0;
    private static final double rightLabelGutter = 24.0;
    private static final double baseSecondsPerUnit = 1.0 / 3.0;

    private static final Color[] palette = {
            Color.web("#FF9500"), Color.web("#4FC3F7"), Color.web("#81C784"),
            Color.web("#BA68C8"), Color.web("#FFD54F"), Color.web("#F06292")
    };
    private static final Color idleColor = Color.web("#3a3a3d");

    private final List<ganttChart.GanttSlice> slices;
    private final int totalTime;
    private final Canvas canvas;
    private final Slider speedSlider = new Slider(0.25, 3.0, 1.0);

    private AnimationTimer timer;
    private long animationStartNanos;
    private double elapsedUnits;
    private double visibleUnits;
    private Runnable onAnimationFinished;

    public GanttChartAnimationView(List<ganttChart.GanttSlice> slices) {
        this.slices = slices;
        this.totalTime = slices.isEmpty() ? 0 : slices.get(slices.size() - 1).end();

        this.canvas = new Canvas(1, topMargin + barHeight + 28);

        Button playButton = new Button("Play Animation");
        playButton.setStyle(buttonStyle("#FF9500", "black"));
        playButton.setOnAction(e -> play());
        playButton.setMinWidth(Region.USE_PREF_SIZE);

        Button skipButton = new Button("Skip to End");
        skipButton.setStyle(buttonStyle("#303134", "white"));
        skipButton.setOnAction(e -> showFullChart());
        skipButton.setMinWidth(Region.USE_PREF_SIZE);

        speedSlider.setPrefWidth(120);
        Text speedLabel = new Text("Speed");
        speedLabel.setFill(Color.web("#a5a5a5"));

        HBox actionRow = new HBox(10, playButton, skipButton);
        HBox speedRow = new HBox(10, speedLabel, speedSlider);
        HBox.setHgrow(speedSlider, Priority.ALWAYS);

        VBox controls = new VBox(8, actionRow, speedRow);
        controls.setPadding(new Insets(10, 0, 0, 0));

        setFillWidth(true);
        setSpacing(6);
        setPadding(new Insets(4));
        getChildren().addAll(canvas, controls);

        widthProperty().addListener((obs, oldWidth, newWidth) -> redraw());
        heightProperty().addListener((obs, oldHeight, newHeight) -> redraw());
        redraw();
    }

    /** Starts (or restarts) the left-to-right reveal animation. */
    public void play() {
        if (timer != null) {
            timer.stop();
        }
        if (totalTime <= 0) {
            return;
        }
        elapsedUnits = 0;
        animationStartNanos = -1;
        visibleUnits = 0;
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (animationStartNanos < 0) {
                    animationStartNanos = now;
                }
                double secondsElapsed = (now - animationStartNanos) / 1_000_000_000.0;
                double unitsPerSecond = 1.0 / (baseSecondsPerUnit / speedSlider.getValue());
                elapsedUnits = secondsElapsed * unitsPerSecond;
                if (elapsedUnits >= totalTime) {
                    elapsedUnits = totalTime;
                    visibleUnits = elapsedUnits;
                    redraw();
                    stop();
                    if (onAnimationFinished != null) {
                        onAnimationFinished.run();
                    }
                    return;
                }
                visibleUnits = elapsedUnits;
                redraw();
            }
        };
        timer.start();
    }

    /** Stops any running animation and renders the chart fully drawn. */
    public void showFullChart() {
        if (timer != null) {
            timer.stop();
        }
        visibleUnits = totalTime;
        redraw();
    }

    private void redraw() {
        double usableWidth = Math.max(1, getWidth() - snappedLeftInset() - snappedRightInset());
        canvas.setWidth(usableWidth);
        canvas.setHeight(topMargin + barHeight + 28);
        drawUpTo(visibleUnits);
    }

    private void drawUpTo(double revealUpToUnits) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.web("#202124"));
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        if (slices.isEmpty()) {
            return;
        }

        double closingLabelWidth = measureTextWidth(String.valueOf(totalTime), Font.font("System", 11));
        double labelGutter = Math.max(rightLabelGutter, closingLabelWidth + 8.0);
        double effectiveUnitWidth = computeUnitWidth(labelGutter);
        double chartWidth = totalTime * effectiveUnitWidth;
        double renderedWidth = chartWidth + labelGutter;
        double x = Math.max(leftMargin, (canvas.getWidth() - renderedWidth) / 2.0);
        double barTop = topMargin;

        for (ganttChart.GanttSlice slice : slices) {
            double sliceWidth = (slice.end() - slice.start()) * effectiveUnitWidth;
            double visibleWidth = clampedVisibleWidth(slice, revealUpToUnits, sliceWidth, effectiveUnitWidth);
            boolean isIdle = "idle".equals(slice.name());
            Color color = isIdle ? idleColor : palette[Math.max(slice.processIndex(), 0) % palette.length];

            // Faint outline for the full slice so the eventual shape is visible before it fills in.
            gc.setFill(color.deriveColor(0, 1, 1, 0.18));
            gc.fillRoundRect(x, barTop, sliceWidth, barHeight, 8, 8);

            if (visibleWidth > 0) {
                gc.setFill(color);
                gc.fillRoundRect(x, barTop, visibleWidth, barHeight, 8, 8);
            }

            gc.setStroke(Color.web("#000000", 0.5));
            gc.setLineWidth(1);
            gc.strokeRoundRect(x, barTop, sliceWidth, barHeight, 8, 8);

            if (!isIdle && (visibleWidth >= sliceWidth - 0.5 || revealUpToUnits >= slice.end())) {
                gc.setFill(Color.BLACK);
                gc.setFont(Font.font("System", FontWeight.BOLD, 15));
                gc.fillText(slice.name(), x + sliceWidth / 2.0 - textHalfWidth(slice.name()), barTop + barHeight / 2.0 + 5);
            }

            gc.setFill(Color.web("#a5a5a5"));
            gc.setFont(Font.font("System", 11));
            gc.fillText(String.valueOf(slice.start()), x - 3, barTop + barHeight + 16);

            x += sliceWidth;
        }

        // Final closing time label.
        gc.setFill(Color.web("#a5a5a5"));
        gc.setFont(Font.font("System", 11));
        gc.fillText(String.valueOf(totalTime), x - 3, barTop + barHeight + 16);

        // Playhead line showing current animation position.
        if (revealUpToUnits > 0 && revealUpToUnits < totalTime) {
            double playheadX = x - chartWidth + revealUpToUnits * effectiveUnitWidth;
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(2);
            gc.strokeLine(playheadX, barTop - 8, playheadX, barTop + barHeight + 4);
        }
    }

    private double computeUnitWidth(double rightLabelGutter) {
        if (totalTime <= 0) {
            return unitWidth;
        }
        double availableWidth = Math.max(1, canvas.getWidth() - leftMargin * 2 - rightLabelGutter);
        double fitWidth = availableWidth / totalTime;
        return Math.min(unitWidth, fitWidth);
    }

    private double clampedVisibleWidth(ganttChart.GanttSlice slice, double revealUpToUnits, double sliceWidth, double effectiveUnitWidth) {
        if (revealUpToUnits <= slice.start()) {
            return 0;
        }
        if (revealUpToUnits >= slice.end()) {
            return sliceWidth;
        }
        return (revealUpToUnits - slice.start()) * effectiveUnitWidth;
    }

    private double textHalfWidth(String text) {
        return measureTextWidth(text, Font.font("System", FontWeight.BOLD, 15)) / 2.0;
    }

    private double measureTextWidth(String text, Font font) {
        Text measure = new Text(text);
        measure.setFont(font);
        return measure.getLayoutBounds().getWidth();
    }

    private String buttonStyle(String bg, String fg) {
        return "-fx-background-color: " + bg + "; -fx-text-fill: " + fg
                + "; -fx-background-radius: 16; -fx-font-weight: bold;";
    }

    public void setOnAnimationFinished(Runnable onAnimationFinished) {
        this.onAnimationFinished = onAnimationFinished;
    }
}
