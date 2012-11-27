package org.wattdepot.benchmark;

import java.util.Hashtable;

/**
 * Test class that runs a WattDepotBenchmark for the
 * Health_Check test.
 * @author Greg Burgess
 *
 */
public final class TestRun {
  /** Private constructor for Checkstyle.
   */
  private TestRun() {

  }

  /**
   * Executes the test.
   * @param args command line args.
   * @throws Exception If something went wrong starting the test.
   */
  public static void main(final String[] args) throws Exception {
    Hashtable<String, String> table = new Hashtable<String, String>();
    table.put("timeToLive",  "5000");
    table.put("pollPeriod", "1000");
    table.put("startUp", "0");
    WattDepotBenchmark benchmark = new WattDepotBenchmark("ADD_SENSOR_DATA",
        table);
    benchmark.run();
    /*table.put("numThreads", "20");
    benchmark = new WattDepotBenchmark("HEALTH_CHECK",
        table);
    benchmark.run();*/
  }
}
