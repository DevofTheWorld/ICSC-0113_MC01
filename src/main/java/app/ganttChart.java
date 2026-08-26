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
        String[] names = new String[burst.length];
        for (int i = 0; i < burst.length; i++) {
            names[i] = "P" + (i + 1);
        }
        printRoundRobin(names, arrival, burst, quantum);
    }

    public static void printRoundRobin(String[] names, int[] arrival, int[] burst, int quantum) { // stores the data in List
        validateInputs(names, arrival, burst, quantum);
        List<Slice> slices = buildSlices(names, arrival, burst, quantum);
        printChartAndMetrics(slices, names, arrival, burst);
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

    private static void printChartAndMetrics(List<Slice> slices, String[] names, int[] arrival, int[] burst) {
        StringBuilder topLine = new StringBuilder();
        StringBuilder middleLine = new StringBuilder();
        StringBuilder bottomLine = new StringBuilder();
        StringBuilder timeLine = new StringBuilder();
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

        System.out.println("Gantt Chart (ASCII):");
        System.out.println(topLine);
        System.out.println(middleLine);
        System.out.println(bottomLine);
        System.out.println(timeLine.toString().stripTrailing());
        System.out.println();
        printMetrics(names, arrival, burst, completionTime);
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

    private static void printMetrics(String[] names, int[] arrival, int[] burst, int[] completionTime) {
        double totalTat = 0;
        double totalWt = 0;
        System.out.println("Process Analysis:");
        System.out.printf("%-8s%-8s%-8s%-8s%-12s%-12s%n", "Process", "AT", "BT", "CT", "TAT", "WT");
        for (int i = 0; i < names.length; i++) {
            int tat = completionTime[i] - arrival[i];
            int wt = tat - burst[i];
            totalTat += tat;
            totalWt += wt;
            System.out.printf("%-8s%-8d%-8d%-8d%-12d%-12d%n",
                    names[i], arrival[i], burst[i], completionTime[i], tat, wt);
        }
        System.out.printf("Average Waiting Time: %.2f%n", totalWt / names.length);
        System.out.printf("Average Turnaround Time: %.2f%n", totalTat / names.length);
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
