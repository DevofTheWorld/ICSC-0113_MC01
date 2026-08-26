package app;

import java.util.Scanner;

public class main {

    static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        int n; // process count
        int q; // time quantum

        System.out.print("Number of process(es)? < 6: " );
        n = in.nextInt();
        int[] arrival = new int[n];
        int[] burst = new int[n];

        for (int i = 0; i < n; i++) {
            System.out.print("Enter Arrival time for P" + (i + 1) + ": ");
            arrival[i] = in.nextInt();

            System.out.print("Enter Burst time for P" + (i + 1) + ": ");
            burst[i] = in.nextInt();
        }
        System.out.print("Enter time quantum: ");
        q = in.nextInt();

        ganttChart.printRoundRobin(arrival, burst, q);
    }
}
