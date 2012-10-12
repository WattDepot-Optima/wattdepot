package org.wattdepot.benchmark;

import org.wattdepot.benchmark.DbStress;
import org.wattdepot.benchmark.SourceServerBenchmark;
import org.wattdepot.benchmark.UserBenchmark;

public class Benchmarker {
    /**
     * Runs Benchmark classes.
     * @throws Exception If something goes wrong.
     */

    public static void main(String[] args) throws Exception {
      String[] toSend = {"100"};
      UserBenchmark.main(toSend);
      toSend = new String[] {"100", "10000"};
      SourceServerBenchmark.main(toSend);
      toSend = new String[] {"1000", "15000", "10"};
      DbStress.main(toSend);
    }

}
