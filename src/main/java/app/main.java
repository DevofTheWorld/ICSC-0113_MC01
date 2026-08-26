package app;

import java.util.Scanner;

public class main {

    static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int p; // process
        int tq; // time quantum

        System.out.print("Number of process(es)? (<= 6)" );
        p = sc.nextInt();
        int at[] = new int[p]; // arrival time
        int bt[] = new int[p]; // burst time

        for (int i = 0; i < p; i++) {
            System.out.print("Enter Arrival time for P" + (i + 1) + ": ");
            at[i] = sc.nextInt();

            System.out.print("Enter Burst time for P" + (i + 1) + ": ");
            bt[i] = sc.nextInt();
        }
        System.out.print("Enter time quantum: ");
        tq = sc.nextInt();

        // gantt chart generator
        String rrGanttChart = ganttChart.generateRoundRobin(at, bt, tq);
        System.out.println(rrGanttChart);


//        for(String name: pName){
//            System.out.printf(name + " ");
//        }

    }
}
