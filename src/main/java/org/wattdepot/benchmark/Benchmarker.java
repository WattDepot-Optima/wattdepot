package org.wattdepot.benchmark;

/** Executes all the Benchmarks.
 * Used to run benchmarks from Maven.
 * @author Greg Burgess
**/
public class Benchmarker {
  /** Error message for bad command line invocation. **/
  private static final String ARG_ERROR_MSG = "Insufficcient "
      + "arguments";
    /**
     * Runs Benchmark classes.
     * @param args Comand line args.
     * @throws Exception If something goes wrong.
     */
    public static void main(final String[] args) throws Exception {

      if (args.length != 6) {
        System.out.println(ARG_ERROR_MSG);
        System.out.println("Recieved "
          + args.length + " args.");
        System.exit(1);
      }
      System.in.read();
      String[] toSend = {args[0]};
      UserBenchmark.main(toSend);
      toSend = new String[] {args[1], args[2]};
      SourceServerBenchmark.main(toSend);
      toSend = new String[] {args[3], args[4], args[5]};
      DbStress.main(toSend);

    }

}
