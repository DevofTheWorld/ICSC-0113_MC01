package app;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Region;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.List;

/**
 * Standalone window that shows the animated Gantt chart for a completed
 * Round Robin run, alongside the process table and average metrics.
 */
public final class GanttChartResultStage {

    private GanttChartResultStage() {
    }

    public static void show(String[] names, int[] arrival, int[] burst, int quantum) {
        List<ganttChart.GanttSlice> slices = ganttChart.computeSlices(names, arrival, burst, quantum);

        int[] completionTime = new int[names.length];
        for (ganttChart.GanttSlice slice : slices) {
            if (slice.processIndex() >= 0) {
                completionTime[slice.processIndex()] = slice.end();
            }
        }

        Label title = new Label("Round Robin Scheduling  \u2014  Quantum = " + quantum);
        title.setFont(Font.font("System", FontWeight.BOLD, 18));
        title.setStyle("-fx-text-fill: white;");

        GanttChartAnimationView animationView = new GanttChartAnimationView(slices);

        ScrollPane chartScroll = new ScrollPane(animationView);
        chartScroll.setFitToHeight(true);
        chartScroll.setPrefHeight(200);
        chartScroll.setStyle("-fx-background: #202124; -fx-background-color: #202124; -fx-border-color: #303134;");

        Label tableHeading = new Label("Process Table");
        tableHeading.setFont(Font.font("System", FontWeight.BOLD, 14));
        tableHeading.setStyle("-fx-text-fill: #a5a5a5;");

        TableView<ProcessRow> table = buildTable(names, arrival, burst, completionTime);

        double totalTat = 0;
        double totalWt = 0;
        for (int i = 0; i < names.length; i++) {
            int tat = completionTime[i] - arrival[i];
            int wt = tat - burst[i];
            totalTat += tat;
            totalWt += wt;
        }

        Label averages = new Label(String.format(
                "Average Waiting Time: %.2f      Average Turnaround Time: %.2f",
                totalWt / names.length, totalTat / names.length));
        averages.setFont(Font.font("System", FontWeight.BOLD, 14));
        averages.setStyle("-fx-text-fill: #FF9500;");

        VBox root = new VBox(12, title, chartScroll, tableHeading, table, averages);
        root.setPadding(new Insets(18));
        root.setStyle("-fx-background-color: #202124;");

        Stage stage = new Stage();
        stage.setTitle("Round Robin Result");
        stage.setScene(new Scene(root, 760, 560));
        stage.show();

        animationView.play();
    }

    private static TableView<ProcessRow> buildTable(String[] names, int[] arrival, int[] burst, int[] completionTime) {
        TableView<ProcessRow> table = new TableView<>();
        table.setFixedCellSize(28);
        table.setMinHeight(Region.USE_PREF_SIZE);
        table.setPrefHeight(Region.USE_COMPUTED_SIZE);
        table.setMaxHeight(Region.USE_PREF_SIZE);
        table.setStyle("-fx-background-color: #202124; -fx-control-inner-background: #202124; "
                + "-fx-text-fill: white; -fx-table-cell-border-color: #303134;");

        TableColumn<ProcessRow, String> processCol = new TableColumn<>("Process");
        processCol.setCellValueFactory(new PropertyValueFactory<>("process"));

        TableColumn<ProcessRow, Number> arrivalCol = new TableColumn<>("AT");
        arrivalCol.setCellValueFactory(new PropertyValueFactory<>("arrival"));

        TableColumn<ProcessRow, Number> burstCol = new TableColumn<>("BT");
        burstCol.setCellValueFactory(new PropertyValueFactory<>("burst"));

        TableColumn<ProcessRow, Number> completionCol = new TableColumn<>("CT");
        completionCol.setCellValueFactory(new PropertyValueFactory<>("completion"));

        TableColumn<ProcessRow, Number> turnaroundCol = new TableColumn<>("TAT");
        turnaroundCol.setCellValueFactory(new PropertyValueFactory<>("turnaround"));

        TableColumn<ProcessRow, Number> waitingCol = new TableColumn<>("WT");
        waitingCol.setCellValueFactory(new PropertyValueFactory<>("waiting"));

        table.getColumns().addAll(List.of(processCol, arrivalCol, burstCol, completionCol, turnaroundCol, waitingCol));
        table.setPrefHeight(table.getFixedCellSize() * (names.length + 1) + 2);

        for (int i = 0; i < names.length; i++) {
            int turnaround = completionTime[i] - arrival[i];
            int waiting = turnaround - burst[i];
            table.getItems().add(new ProcessRow(names[i], arrival[i], burst[i], completionTime[i], turnaround, waiting));
        }
        return table;
    }

    /** Row model backing the process TableView. */
    public static class ProcessRow {
        private final String process;
        private final int arrival;
        private final int burst;
        private final int completion;
        private final int turnaround;
        private final int waiting;

        public ProcessRow(String process, int arrival, int burst, int completion, int turnaround, int waiting) {
            this.process = process;
            this.arrival = arrival;
            this.burst = burst;
            this.completion = completion;
            this.turnaround = turnaround;
            this.waiting = waiting;
        }

        public String getProcess() {
            return process;
        }

        public int getArrival() {
            return arrival;
        }

        public int getBurst() {
            return burst;
        }

        public int getCompletion() {
            return completion;
        }

        public int getTurnaround() {
            return turnaround;
        }

        public int getWaiting() {
            return waiting;
        }
    }
}
