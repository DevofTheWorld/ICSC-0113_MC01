package app;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;

public class ganttChart {

    public static int num(int[] arrival, int processCount) {
        if (arrival == null || processCount <= 0) {
            return 0;
        }
        return Math.min(arrival.length, processCount);
    }

    public static void printRoundRobin(int[] arrival, int[] burst, int quantum) { // creates a name for each process
        String report = solveRoundRobin(arrival, burst, quantum);
        System.out.print(report);
    }

    public static void printRoundRobin(String[] names, int[] arrival, int[] burst, int quantum) { // stores the data in List
        String report = solveRoundRobin(names, arrival, burst, quantum);
        System.out.print(report);
    }

    public static String solveRoundRobin(int[] arrival, int[] burst, int quantum) {
        String[] names = new String[burst.length];
        for (int i = 0; i < burst.length; i++) {
            names[i] = "P" + (i + 1);
        }
        return solveRoundRobin(names, arrival, burst, quantum);
    }

    public static String solveRoundRobin(String[] names, int[] arrival, int[] burst, int quantum) {
        validateInputs(names, arrival, burst, quantum);
        List<Slice> slices = buildSlices(names, arrival, burst, quantum);
        return buildRoundRobinReport(slices, names, arrival, burst);
    }

    // Starts to slice burst time of each process based on the given time quantum

    private static List<Slice> buildSlices(String[] names, int[] arrival, int[] burst, int quantum) {
        int n = burst.length;
        int[] remaining = Arrays.copyOf(burst, n);
        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) {
            order[i] = i;
        }
        Arrays.sort(order, Comparator.comparingInt((Integer i) -> arrival[i]).thenComparingInt(i -> i));

        List<Slice> slices = new ArrayList<>();
        Queue<Integer> readyQueue = new ArrayDeque<>();
        boolean[] inQueue = new boolean[n];
        int completed = 0;
        int time = 0;
        int nextArrival = 0;

        while (completed < n) {
            while (nextArrival < n && arrival[order[nextArrival]] <= time) {
                int idx = order[nextArrival++];
                if (!inQueue[idx] && remaining[idx] > 0) {
                    readyQueue.offer(idx);
                    inQueue[idx] = true;
                }
            }

            if (readyQueue.isEmpty()) {
                int idx = order[nextArrival];
                int nextTime = arrival[idx];
                if (time < nextTime) {
                    slices.add(new Slice("idle", time, nextTime, -1));
                    time = nextTime;
                }
                continue;
            }

            int current = readyQueue.poll();
            inQueue[current] = false;
            int start = time;
            int run = Math.min(quantum, remaining[current]);
            time += run;
            remaining[current] -= run;
            slices.add(new Slice(names[current], start, time, current));

            while (nextArrival < n && arrival[order[nextArrival]] <= time) {
                int idx = order[nextArrival++];
                if (!inQueue[idx] && remaining[idx] > 0) {
                    readyQueue.offer(idx);
                    inQueue[idx] = true;
                }
            }

            if (remaining[current] > 0) {
                readyQueue.offer(current);
                inQueue[current] = true;
            } else {
                completed++;
            }
        }

        return slices;
    }

    private static String buildRoundRobinReport(List<Slice> slices, String[] names, int[] arrival, int[] burst) {
        StringBuilder topLine = new StringBuilder();
        StringBuilder middleLine = new StringBuilder();
        StringBuilder bottomLine = new StringBuilder();
        StringBuilder timeLine = new StringBuilder();
        StringBuilder report = new StringBuilder();
        List<Integer> boundaryPositions = new ArrayList<>();
        List<Integer> boundaryTimes = new ArrayList<>();
        int cursor = 0;

        topLine.append("+");
        middleLine.append("|");
        bottomLine.append("+");
        boundaryPositions.add(0);
        boundaryTimes.add(slices.get(0).start);

        for (Slice slice : slices) {
            int duration = slice.end - slice.start;
            int width = Math.max(slice.name.length() + 2, duration * 2);
            topLine.append(repeat("-", width)).append("+");
            middleLine.append(center(slice.name, width)).append("|");
            bottomLine.append(repeat("-", width)).append("+");
            cursor += width + 1;
            boundaryPositions.add(cursor);
            boundaryTimes.add(slice.end);
        }

        for (int i = 0; i < boundaryPositions.size(); i++) {
            placeText(timeLine, boundaryPositions.get(i), String.valueOf(boundaryTimes.get(i)));
        }

        int[] completionTime = computeCompletionTimes(slices, burst.length);

        report.append(buildLinearGanttLine(slices)).append(System.lineSeparator());
        report.append(System.lineSeparator());
        report.append(buildExecutionSegmentsReport(slices, burst)).append(System.lineSeparator());
        report.append(System.lineSeparator());
        report.append("Gantt Chart (ASCII):").append(System.lineSeparator());
        report.append(topLine).append(System.lineSeparator());
        report.append(middleLine).append(System.lineSeparator());
        report.append(bottomLine).append(System.lineSeparator());
        report.append(timeLine.toString().stripTrailing()).append(System.lineSeparator());
        report.append(System.lineSeparator());
        report.append(buildMetricsReport(names, arrival, burst, completionTime));
        return report.toString();
    }

    private static int[] computeCompletionTimes(List<Slice> slices, int n) {
        int[] completionTime = new int[n];
        Arrays.fill(completionTime, -1);
        for (Slice slice : slices) {
            if (slice.processIndex >= 0) {
                completionTime[slice.processIndex] = slice.end;
            }
        }
        return completionTime;
    }

    private static String buildLinearGanttLine(List<Slice> slices) {
        StringBuilder line = new StringBuilder();
        if (slices.isEmpty()) {
            return line.toString();
        }
        line.append(slices.get(0).start);
        for (Slice slice : slices) {
            line.append(" | ").append(slice.name).append(" | ").append(slice.end);
        }
        return line.toString();
    }

    private static String buildExecutionSegmentsReport(List<Slice> slices, int[] burst) {
        int[] remaining = Arrays.copyOf(burst, burst.length);
        StringBuilder report = new StringBuilder();
        report.append("Execution Segments:").append(System.lineSeparator());
        report.append(String.format("%-12s%-10s%-10s%-16s%n", "Process", "Start", "End", "Remaining BT"));
        for (Slice slice : slices) {
            int remainingAfterSegment = -1;
            if (slice.processIndex >= 0) {
                remaining[slice.processIndex] -= (slice.end - slice.start);
                remainingAfterSegment = remaining[slice.processIndex];
            }
            String remainingText = remainingAfterSegment >= 0 ? String.valueOf(remainingAfterSegment) : "-";
            report.append(String.format("%-12s%-10d%-10d%-16s%n", slice.name, slice.start, slice.end, remainingText));
        }
        return report.toString().stripTrailing();
    }

    private static String buildMetricsReport(String[] names, int[] arrival, int[] burst, int[] completionTime) {
        double totalTat = 0;
        double totalWt = 0;
        StringBuilder report = new StringBuilder();
        report.append("Process Analysis:").append(System.lineSeparator());
        report.append(String.format("%-8s%-8s%-8s%-8s%-12s%-12s%n", "Process", "AT", "BT", "CT", "TAT", "WT"));
        for (int i = 0; i < names.length; i++) {
            int tat = completionTime[i] - arrival[i];
            int wt = tat - burst[i];
            totalTat += tat;
            totalWt += wt;
            report.append(String.format("%-8s%-8d%-8d%-8d%-12d%-12d%n",
                    names[i], arrival[i], burst[i], completionTime[i], tat, wt));
        }
        report.append(String.format("Average Waiting Time: %.2f%n", totalWt / names.length));
        report.append(String.format("Average Turnaround Time: %.2f%n", totalTat / names.length));
        return report.toString();
    }

    private static String center(String text, int width) {
        int pad = width - text.length();
        int left = pad / 2;
        int right = pad - left;
        return repeat(" ", left) + text + repeat(" ", right);
    }

    private static String repeat(String text, int count) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < count; i++) {
            result.append(text);
        }
        return result.toString();
    }

    private static void placeText(StringBuilder line, int position, String text) {
        int needed = position + text.length();
        while (line.length() < needed) {
            line.append(" ");
        }
        for (int i = 0; i < text.length(); i++) {
            line.setCharAt(position + i, text.charAt(i));
        }
    }

    private static void validateInputs(String[] names, int[] arrival, int[] burst, int quantum) {
        if (names == null || arrival == null || burst == null) {
            throw new IllegalArgumentException("Inputs cannot be null.");
        }
        if (names.length != arrival.length || arrival.length != burst.length) {
            throw new IllegalArgumentException("Process names, arrival times, and burst times must have equal length.");
        }
        if (names.length == 0) {
            throw new IllegalArgumentException("At least one process is required.");
        }
        if (quantum <= 0) {
            throw new IllegalArgumentException("Time quantum must be greater than 0.");
        }
        for (int i = 0; i < arrival.length; i++) {
            if (arrival[i] < 0 || burst[i] <= 0) {
                throw new IllegalArgumentException("Arrival times must be >= 0 and burst times must be > 0.");
            }
        }
    }

    private static class Slice {
        private final String name;
        private final int start;
        private final int end;
        private final int processIndex;

        private Slice(String name, int start, int end, int processIndex) {
            this.name = name;
            this.start = start;
            this.end = end;
            this.processIndex = processIndex;
        }
    }
}
