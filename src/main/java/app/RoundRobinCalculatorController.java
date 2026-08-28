package app;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.util.List;
//
public class RoundRobinCalculatorController {
    @FXML
    private Label indicatorField;
    @FXML
    private TextField displayValue;
    @FXML
    private Button clearButton;
    @FXML
    private Button addButton;
    @FXML
    private Button subtractButton;
    @FXML
    private Button enterButton;
    @FXML
    private Button zeroButton;
    @FXML
    private Button oneButton;
    @FXML
    private Button twoButton;
    @FXML
    private Button threeButton;
    @FXML
    private Button fourButton;
    @FXML
    private Button fiveButton;
    @FXML
    private Button sixButton;
    @FXML
    private Button sevenButton;
    @FXML
    private Button eightButton;
    @FXML
    private Button nineButton;
    @FXML
    private Button deciButton;
    @FXML
    private BorderPane calculatorRoot;
    @FXML
    private VBox resultContainer;
    @FXML
    private Label resultTitle;
    @FXML
    private ScrollPane chartScroll;
    @FXML
    private Label tableHeading;
    @FXML
    private TableView<GanttChartResultStage.ProcessRow> resultTable;
    @FXML
    private Label averagesLabel;
    @FXML
    private Button newCalculationButton;

    private final RoundRobinInputFlow inputFlow = new RoundRobinInputFlow();
    private boolean transitioningToResult;

    @FXML
    private void initialize() {
        bindNumberButtons();
        clearButton.setOnAction(event -> displayValue.setText("0"));
        addButton.setOnAction(event -> submitDisplayValue());
        subtractButton.setOnAction(event -> removeLastCharacter());
        deciButton.setOnAction(event -> appendDecimalPoint());
        enterButton.setOnAction(event -> submitDisplayValue());
        newCalculationButton.setOnAction(event -> showInputModeInstant());
        configureResultTable();
        showInputModeInstant();
        resetDisplay();
    }

    private void bindNumberButtons() {
        Button[] numberButtons = {
                zeroButton, oneButton, twoButton, threeButton, fourButton,
                fiveButton, sixButton, sevenButton, eightButton, nineButton
        };
        for (int i = 0; i < numberButtons.length; i++) {
            final String digit = String.valueOf(i);
            numberButtons[i].setOnAction(event -> appendDigit(digit));
        }
    }

    private void appendDigit(String digit) {
        if ("0".equals(displayValue.getText())) {
            displayValue.setText(digit);
            return;
        }
        displayValue.setText(displayValue.getText() + digit);
    }

    private void submitDisplayValue() {
        if (transitioningToResult) {
            return;
        }
        int value;
        try {
            value = Integer.parseInt(displayValue.getText().trim());
        } catch (NumberFormatException ex) {
            indicatorField.setText("Please enter a whole number.");
            displayValue.setText("0");
            return;
        }

        FlowResult result = inputFlow.submit(value);
        if (result.error()) {
            indicatorField.setText(result.message());
            displayValue.setText("0");
            return;
        }

        indicatorField.setText(result.nextPrompt());
        displayValue.setText("0");

        if (result.completed()) {
            transitioningToResult = true;
            playFadeToResult(
                    inputFlow.getLastNames(),
                    inputFlow.getLastArrivalTimes(),
                    inputFlow.getLastBurstTimes(),
                    inputFlow.getLastQuantum());
        }
    }

    private void appendDecimalPoint() {
        if (displayValue.getText().contains(".")) {
            return;
        }
        displayValue.setText(displayValue.getText() + ".");
    }

    private void removeLastCharacter() {
        String current = displayValue.getText();
        if (current.length() <= 1) {
            displayValue.setText("0");
            return;
        }
        displayValue.setText(current.substring(0, current.length() - 1));
    }

    private void resetDisplay() {
        inputFlow.reset();
        indicatorField.setText(inputFlow.currentPrompt());
        displayValue.setText("0");
    }

    private void playFadeToResult(String[] names, int[] arrival, int[] burst, int quantum) {
        indicatorField.setText("Starting animation...");
        FadeTransition fade = new FadeTransition(Duration.millis(450), calculatorRoot);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.setOnFinished(event -> {
            calculatorRoot.setVisible(false);
            calculatorRoot.setManaged(false);
            showResultMode(names, arrival, burst, quantum);
            transitioningToResult = false;
        });
        fade.play();
    }

    private void showResultMode(String[] names, int[] arrival, int[] burst, int quantum) {
        List<ganttChart.GanttSlice> slices = ganttChart.computeSlices(names, arrival, burst, quantum);
        int[] completionTime = new int[names.length];
        for (ganttChart.GanttSlice slice : slices) {
            if (slice.processIndex() >= 0) {
                completionTime[slice.processIndex()] = slice.end();
            }
        }

        resultTitle.setText("Round Robin Scheduling  —  Quantum = " + quantum);
        fillResultTable(names, arrival, burst, completionTime);
        fillAveragesLabel(names, arrival, burst, completionTime);
        hideTableAndAverages();

        GanttChartAnimationView animationView = new GanttChartAnimationView(slices);
        animationView.setOnAnimationFinished(this::showTableAndAverages);
        chartScroll.setContent(animationView);

        resultContainer.setOpacity(1.0);
        resultContainer.setVisible(true);
        resultContainer.setManaged(true);
        animationView.play();
    }

    private void showInputModeInstant() {
        resultContainer.setVisible(false);
        resultContainer.setManaged(false);
        calculatorRoot.setOpacity(1.0);
        calculatorRoot.setVisible(true);
        calculatorRoot.setManaged(true);
        resetDisplay();
    }

    private void configureResultTable() {
        resultTable.getColumns().clear();

        TableColumn<GanttChartResultStage.ProcessRow, String> processCol = new TableColumn<>("Process");
        processCol.setCellValueFactory(new PropertyValueFactory<>("process"));

        TableColumn<GanttChartResultStage.ProcessRow, Number> arrivalCol = new TableColumn<>("AT");
        arrivalCol.setCellValueFactory(new PropertyValueFactory<>("arrival"));

        TableColumn<GanttChartResultStage.ProcessRow, Number> burstCol = new TableColumn<>("BT");
        burstCol.setCellValueFactory(new PropertyValueFactory<>("burst"));

        TableColumn<GanttChartResultStage.ProcessRow, Number> completionCol = new TableColumn<>("CT");
        completionCol.setCellValueFactory(new PropertyValueFactory<>("completion"));

        TableColumn<GanttChartResultStage.ProcessRow, Number> turnaroundCol = new TableColumn<>("TAT");
        turnaroundCol.setCellValueFactory(new PropertyValueFactory<>("turnaround"));

        TableColumn<GanttChartResultStage.ProcessRow, Number> waitingCol = new TableColumn<>("WT");
        waitingCol.setCellValueFactory(new PropertyValueFactory<>("waiting"));

        resultTable.getColumns().addAll(processCol, arrivalCol, burstCol, completionCol, turnaroundCol, waitingCol);
        resultTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        tableHeading.setFont(Font.font("System", FontWeight.BOLD, 14));
    }

    private void fillResultTable(String[] names, int[] arrival, int[] burst, int[] completionTime) {
        resultTable.getItems().clear();
        for (int i = 0; i < names.length; i++) {
            int turnaround = completionTime[i] - arrival[i];
            int waiting = turnaround - burst[i];
            resultTable.getItems().add(new GanttChartResultStage.ProcessRow(
                    names[i], arrival[i], burst[i], completionTime[i], turnaround, waiting));
        }
    }

    private void fillAveragesLabel(String[] names, int[] arrival, int[] burst, int[] completionTime) {
        double totalTat = 0;
        double totalWt = 0;
        for (int i = 0; i < names.length; i++) {
            int tat = completionTime[i] - arrival[i];
            int wt = tat - burst[i];
            totalTat += tat;
            totalWt += wt;
        }
        averagesLabel.setText(String.format(
                "Average Waiting Time: %.2f      Average Turnaround Time: %.2f",
                totalWt / names.length, totalTat / names.length));
    }

    private void hideTableAndAverages() {
        tableHeading.setVisible(false);
        tableHeading.setManaged(false);
        resultTable.setVisible(false);
        resultTable.setManaged(false);
        averagesLabel.setVisible(false);
        averagesLabel.setManaged(false);
    }

    private void showTableAndAverages() {
        tableHeading.setVisible(true);
        tableHeading.setManaged(true);
        resultTable.setVisible(true);
        resultTable.setManaged(true);
        averagesLabel.setVisible(true);
        averagesLabel.setManaged(true);
    }
}