package org.wattdepot.test;

import org.wattdepot.client.WattDepotClient;
import org.wattdepot.datainput.SensorSource;
import org.wattdepot.resource.source.jaxb.Source;

/**
 * Provides a class for Benchmarking the interaction between Sensors/Sources
 * and the WDServer.  Tracks successful requests, total errors, and execution
 * time.  Based on WattExample by Yongwen Xu
 * (https://code.google.com/p/wattdepot/wiki/ManySourcesEvaluation).
 * @author Greg Burgess
 */

public final class SourceServerBenchmark {
  /** Default source URI. **/
  private static String mSourceOwnerUri =
    "http://localhost:8182/wattdepot/users/foo@example.com";
  /** Default source name. **/
  private static String mSourceName = "MyMeter13";
  /** Default Wattdepot server URI. **/
  private static String mServerURI = "http://localhost:8182/wattdepot/";
  /** Default Wattdepot server Username. **/
  private static String mServerUser = "foo@example.com";
  /** Default Wattdepot server Password. **/
  private static String mServerPwd = "CHANGE-ME";
  /** The client the test will use to construct sources. **/
  private static WattDepotClient client;

  /** Private constructor to satisfy Checkstyle.
   */
  private SourceServerBenchmark() {
  }

 /**
 * Executes the benchmark.
 * @param args Command line arguments.
 */
  public static void main(final String[] args) {
    client = new WattDepotClient(mServerURI, mServerUser, mServerPwd);
    createSource(0);
    SensorSource senSource = new SensorSource(mSourceName);
    senSource.setName(mSourceName);
    senSource.setMeterHostname(mSourceName);
    BenchmarkSensor bench = new BenchmarkSensor(mServerURI, mServerUser,
        mServerPwd, senSource , false);
    SensorThread thread = new SensorThread(bench);
    thread.start();
  }

  /** Registers a source with the WDServer using the mSourceName and an index.
   * @param index The value appended to the end of the Source name to give
   * each sensor a unique name.
   */
  private static void createSource(final int index) {
     try {
       client.storeSource(new Source(
          mSourceName,
          mSourceOwnerUri,
          true, // public
          false, // not virtual
          "Coordinates",
          "Location",
          "Description",
          null, // no additional props
          null), // no sub source
       true);
    } catch (Exception e) {
        System.out.println("Unable to store source " + mSourceName
            + index + ". Exception is " + e);
    }
  }
}