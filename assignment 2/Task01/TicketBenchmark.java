package task01;

import support.Bench;
import java.util.*;

public class TicketBenchmark {
    public static String build(String[] fields, int mode) {
        if (mode == 0) {
            String ticket = "";
            for (String f : fields) ticket = ticket + f + '\n';
            return ticket;
        }
        if (mode == 1) {
            StringBuilder b = new StringBuilder();
            for (String f : fields) b.append(f).append('\n');
            return b.toString();
        }
        StringBuffer b = new StringBuffer();
        for (String f : fields) b.append(f).append('\n');
        return b.toString();
    }
    public static String join(String[] fields) { return String.join("\n", fields) + "\n"; }
    public static String stringJoiner(String[] fields) {
        StringJoiner j = new StringJoiner("\n", "", "\n");
        for (String f : fields) j.add(f);
        return fields.length == 0 ? "" : j.toString();
    }
    public static void main(String[] args) throws Exception {
        String[] sample = {"PNR: 1234567890", "Passenger: Asha", "Coach: B2",
            "Berth: 21", "Fare: 850", "Journey: Chennai to Bengaluru"};
        for (int mode = 0; mode < 3; mode++)
            Bench.require(build(sample, mode).equals(build(sample, 1)), "Ticket mismatch");
        Bench.require(join(sample).equals(build(sample, 1)), "join mismatch");
        Bench.require(stringJoiner(sample).equals(build(sample, 1)), "joiner mismatch");
        System.out.print(build(sample, 1));
        System.out.println("Correctness: all ticket implementations agree.");
        int[][] workloads = {{1,6}, {10000,50}, {100,500}};
        String[] names = {"StringPlus", "StringBuilder", "StringBuffer"};
        try (Bench b = new Bench("Task01")) {
            for (int[] w : workloads) {
                int tickets = w[0], count = w[1];
                String[] fields = new String[count];
                for (int i = 0; i < count; i++) fields[i] = String.format("Field%04d: value", i);
                for (int mode = 0; mode < 3; mode++) {
                    final int m = mode;
                    b.measure(names[m]+"_tickets"+tickets, count, tickets, () -> () -> {
                        long checksum = 0;
                        for (int t = 0; t < tickets; t++) {
                            String ticket = build(fields, m);
                            // Consume contents as well as length, discouraging eliminated work.
                            checksum += ticket.hashCode();
                        }
                        return checksum;
                    });
                }
                long prefixChars = 0, length = 0;
                for (String field : fields) { length += field.length()+1; prefixChars += length; }
                System.out.printf("MODEL fields=%d tickets=%d plus_prefix_chars=%d final_chars=%d "
                    + "plus_intermediate_strings_estimate=%d builder_final_strings=%d%n",
                    count, tickets, prefixChars*tickets, length*tickets,
                    (long)(count-1)*tickets, tickets);
            }
        }
    }
}
