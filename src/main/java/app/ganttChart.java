package app;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;

public class ganttChart {

    public static int num(int[] at, int p) {
        if (at == null || p <= 0) {
            return 0;
        }
        return Math.min(at.length, p);
    }

    public static String generateRoundRobin(int[] at, int[] bt, int tq) {
        String[] pName = new String[bt.length];
        for (int i = 0; i < bt.length; i++) {
            pName[i] = "P" + (i + 1);
        }
        return generateRoundRobin(pName, at, bt, tq);
    }

    public static String generateRoundRobin(String[] pName, int[] at, int[] bt, int tq) {
        validateInputs(pName, at, bt, tq);
        List<Slice> slices = buildSlices(pName, at, bt, tq);
        return formatGanttChart(slices);
    }

    private static List<Slice> buildSlices(String[] pName, int[] at, int[] bt, int tq) {
        int n = bt.length;
        int[] remaining = Arrays.copyOf(bt, n);
        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) {
            order[i] = i;
        }
        Arrays.sort(order, Comparator.comparingInt((Integer i) -> at[i]).thenComparingInt(i -> i));

        List<Slice> slices = new ArrayList<>();
        Queue<Integer> readyQueue = new ArrayDeque<>();
        boolean[] inQueue = new boolean[n];
        int completed = 0;
        int time = 0;
        int nextArrival = 0;

        while (completed < n) {
            while (nextArrival < n && at[order[nextArrival]] <= time) {
                int idx = order[nextArrival++];
                if (!inQueue[idx] && remaining[idx] > 0) {
                    readyQueue.offer(idx);
                    inQueue[idx] = true;
                }
            }

            if (readyQueue.isEmpty()) {
                int idx = order[nextArrival];
                int nextTime = at[idx];
                if (time < nextTime) {
                    slices.add(new Slice("idle", time, nextTime));
                    time = nextTime;
                }
                continue;
            }

            int current = readyQueue.poll();
            inQueue[current] = false;
            int start = time;
            int run = Math.min(tq, remaining[current]);
            time += run;
            remaining[current] -= run;
            slices.add(new Slice(pName[current], start, time));

            while (nextArrival < n && at[order[nextArrival]] <= time) {
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

    private static String formatGanttChart(List<Slice> slices) {
        StringBuilder out = new StringBuilder("Gantt Chart:\n");
        for (Slice slice : slices) {
            out.append("[").append(slice.start).append("-").append(slice.end).append("] ")
                    .append(slice.name).append(" ");
        }
        return out.toString().trim();
    }

    private static void validateInputs(String[] pName, int[] at, int[] bt, int tq) {
        if (pName == null || at == null || bt == null) {
            throw new IllegalArgumentException("Inputs cannot be null.");
        }
        if (pName.length != at.length || at.length != bt.length) {
            throw new IllegalArgumentException("Process names, arrival times, and burst times must have equal length.");
        }
        if (tq <= 0) {
            throw new IllegalArgumentException("Time quantum must be greater than 0.");
        }
        for (int i = 0; i < at.length; i++) {
            if (at[i] < 0 || bt[i] <= 0) {
                throw new IllegalArgumentException("Arrival times must be >= 0 and burst times must be > 0.");
            }
        }
    }

    private static class Slice {
        private final String name;
        private final int start;
        private final int end;

        private Slice(String name, int start, int end) {
            this.name = name;
            this.start = start;
            this.end = end;
        }
    }
}
