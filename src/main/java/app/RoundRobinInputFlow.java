package app;
//
public class RoundRobinInputFlow {
    private static final int MAX_PROCESSES = 6;

    private int processCount;
    private int currentProcessIndex;
    private boolean expectingArrival;
    private int[] arrivalTimes;
    private int[] burstTimes;

    public RoundRobinInputFlow() {
        reset();
    }

    public void reset() {
        processCount = 0;
        currentProcessIndex = 0;
        expectingArrival = true;
        arrivalTimes = null;
        burstTimes = null;
    }

    public String currentPrompt() {
        if (arrivalTimes == null) {
            return "Number of processes (1-6)";
        }
        if (currentProcessIndex < processCount) {
            if (expectingArrival) {
                return "Arrival time for P" + (currentProcessIndex + 1);
            }
            return "Burst time for P" + (currentProcessIndex + 1);
        }
        return "Time quantum";
    }

    public FlowResult submit(int value) {
        if (arrivalTimes == null) {
            if (value <= 0) {
                return FlowResult.error("Process count must be > 0.");
            }
            if (value > MAX_PROCESSES) {
                return FlowResult.error("Maximum process count is 6.");
            }
            processCount = value;
            arrivalTimes = new int[processCount];
            burstTimes = new int[processCount];
            currentProcessIndex = 0;
            expectingArrival = true;
            return FlowResult.next(currentPrompt());
        }

        if (currentProcessIndex < processCount && expectingArrival) {
            if (value < 0) {
                return FlowResult.error("Arrival time must be >= 0.");
            }
            arrivalTimes[currentProcessIndex] = value;
            expectingArrival = false;
            return FlowResult.next(currentPrompt());
        }

        if (currentProcessIndex < processCount) {
            if (value <= 0) {
                return FlowResult.error("Burst time must be > 0.");
            }
            burstTimes[currentProcessIndex] = value;
            currentProcessIndex++;
            expectingArrival = true;
            return FlowResult.next(currentPrompt());
        }

        if (value <= 0) {
            return FlowResult.error("Time quantum must be > 0.");
        }

        try {
            ganttChart.printRoundRobin(arrivalTimes, burstTimes, value);
            reset();
            return FlowResult.completed(currentPrompt());
        } catch (IllegalArgumentException ex) {
            return FlowResult.error(ex.getMessage());
        }
    }
}
