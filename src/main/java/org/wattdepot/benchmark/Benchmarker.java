package org.wattdepot.benchmark;

/** Executes all the Benchmarks.
 * Used to run benchmarks from Maven.
 * @author Greg Burgess
**/
public class Benchmarker {
    /**
     * Runs Benchmark classes.
     * @param args Comand line args.
     * @throws Exception If something goes wrong.
     */
    public static void main(final String[] args) throws Exception {
      String[] toSend = {"100"};
      UserBenchmark.main(toSend);
      toSend = new String[] {"100", "10000"};
      SourceServerBenchmark.main(toSend);
      toSend = new String[] {"1000", "15000", "10"};
      DbStress.main(toSend);
    }

}
