package org.wattdepot.benchmark;

/**
 * Test class that runs a WattDepotBenchmark for the
 * Health_Check test.
 * @author Greg Burgess
 *
 */
public class TestRun {

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
    WattDepotBenchmark benchmark = new WattDepotBenchmark("HEALTH_CHECK",
        100, 2000);
    benchmark.start();
  }
}