package app;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
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

    private final RoundRobinInputFlow inputFlow = new RoundRobinInputFlow();

    @FXML
    private void initialize() {
        bindNumberButtons();
        clearButton.setOnAction(event -> displayValue.setText("0"));
        addButton.setOnAction(event -> submitDisplayValue());
        subtractButton.setOnAction(event -> removeLastCharacter());
        deciButton.setOnAction(event -> appendDecimalPoint());
        enterButton.setOnAction(event -> submitDisplayValue());
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
            indicatorField.setText("Solved. Enter new values (1-6).");
            GanttChartResultStage.show(
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
}