package org.wattdepot.benchmark;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.text.NumberFormat;
import java.util.Hashtable;
import java.util.concurrent.CountDownLatch;
import org.wattdepot.client.WattDepotClient;
import org.wattdepot.datainput.SensorSource;
import org.wattdepot.resource.source.jaxb.Source;
import org.wattdepot.server.Server;
import org.wattdepot.server.ServerProperties;

/**
 * Provides a class for Benchmarking the interaction between Sensors/Sources
 * and the WDServer.  Tracks successful requests, total errors, and execution
 * time.  Based on WattExample by Yongwen Xu
 * (https://code.google.com/p/wattdepot/wiki/ManySourcesEvaluation).
 * @author Greg Burgess
 */

public final class SourceServerBenchmark extends Thread {
  /** The path to the server properties file. **/
  private static final String PROTOCOL = "http://";
  /** Default source URI. **/
  private static String mSourceOwnerUri =
    "http://localhost:8182/wattdepot/users/foo@example.com";
  /** Default source name. **/
  private static String mSourceName = "MyMeter";
  /** Default Wattdepot server URI. **/
  private static String mServerURI = "http://localhost:8182/wattdepot/";
  /** Default Wattdepot server Username. **/
  private static String mServerUser = "foo@example.com";
  /** Default Wattdepot server Password. **/
  private static String mServerPwd = "CHANGE-ME";
  /** The client the test will use to construct sources. **/
  private static WattDepotClient client;
  /** Number of ms to run for. **/
  private static long ttl = 0;
  /** Number of sensors & threads to run.  **/
  private static int numThreads = 0;
  /** Literal used to convert from decimal to percentage. **/
  private static final int HUNDRED = 100;
  /** Error message for bad command line invocation. **/
  private static final String ARG_ERROR_MSG = "Usage is: "
      + "<Number of Sensors to Run (integer > 0)> "
      + "<Time to run for in ms (long > 0)>";


  /** Private constructor to satisfy Checkstyle.
   */
  private SourceServerBenchmark() {
  }

 /**
 * Executes the benchmark.
 * @param args Command line arguments of the form
 *    <number of sensors to run> <time to run in ms>.
 * @throws Exception If something goes wrong with initializing
 *                     the server.
 */
  public static void main(final String[] args) throws Exception {
    Server server = Server.newInstance();
    //parse command line args
    if (args.length < 2) {
      System.out.println(ARG_ERROR_MSG);
      System.exit(0);
    }
    try {
      numThreads = Integer.parseInt(args[0]);
      ttl = Long.parseLong(args[1]);
    }
    catch (NumberFormatException e) {
        System.out.println(ARG_ERROR_MSG);
        server.shutdown();
        System.exit(0);
    }

    if (!(numThreads > 0 && ttl > 0)) {
      System.out.println(ARG_ERROR_MSG);
      server.shutdown();
      System.exit(0);
    }

    ServerProperties properties = null;
    properties = new ServerProperties();
    mServerURI = PROTOCOL
      + properties.get("wattdepot-server.hostname") + ":"
      + properties.get("wattdepot-server.port") + "/"
      + properties.get("wattdepot-server.context.root") + "/";

    mServerUser = properties.get("wattdepot-server.admin.email");
    mServerPwd = properties.get("wattdepot-server.admin.password");
    client = new WattDepotClient(mServerURI, mServerUser, mServerPwd);
    healthCheck();
    CountDownLatch startSignal = new CountDownLatch(1);
    CountDownLatch doneSignal = new CountDownLatch(numThreads);
    SensorSource[] senSources = new SensorSource[numThreads];
    BenchmarkSensor[] benches = new BenchmarkSensor[numThreads];
    SensorThread[] threads = new SensorThread[numThreads];
    System.out.println("Creating Threads...");
    for (int i = 0; i < numThreads; i++) {
      String indexedName = mSourceName + i;
      createSource(i);
      senSources[i] = new SensorSource(indexedName);
      senSources[i].setName(indexedName);
      senSources[i].setMeterHostname(indexedName);
      benches[i] = new BenchmarkSensor(mServerURI, mServerUser,
          mServerPwd, senSources[i] , false);
      threads[i] = new SensorThread(startSignal, doneSignal, benches[i]);
      threads[i].start();
    }
    System.out.println("Starting threads...");
    startSignal.countDown();

    //START THE CLOCK!
    try {
      sleep(ttl);
    }
    catch (InterruptedException e) {
      System.err.println("ERROR: Benchmark Thread Interupted.");
    }
    //Send a hint to the threads that they
    //should stop running
    System.out.println("Stopping Threads...");
    SensorThread.halt();
    //take a snapshot of the result at the deadline
    Hashtable<String, Long> result = BenchmarkSensor.getResults();
    //Wait for threads to comply
    //unnecessary, but good practice...
    doneSignal.await();

    double requests = result.get("requestCount");
    double errors = result.get("errorCount");
    NumberFormat nf = NumberFormat.getInstance();
    nf.setMaximumFractionDigits(0);
    String toReturn = "";
    toReturn += "Result:\n-------\n" + "Execution Time: "
        + nf.format(ttl) + "ms\n";
    toReturn += "Successful Requests: " + nf.format(requests)
        + "\n";
    toReturn += String.format("Errors: " + nf.format(errors)
        + " (");
    toReturn += String.format("%1$.2f", errors
        / (requests + errors) * HUNDRED);
    toReturn += "%)\n";
    //Shutdown server
    server.shutdown();
    System.out.println(toReturn);

    try {
      System.out.println("Writing output to "
          + System.getProperty("user.home"));
      FileWriter fstream = new FileWriter(
          System.getProperty("user.home") + "\\SourceServerBench.txt");
      BufferedWriter out = new BufferedWriter(fstream);
      out.write("Sensor/Source benchmark");
      out.newLine();
      out.write(toReturn);
      out.newLine();
      out.close();
      }
    catch (Exception e) {
      System.err.println("Error: " + e.getMessage());
      }
  }

  /** Registers a source with the WDServer using the mSourceName and an index.
   * @param index The value appended to the end of the Source name to give
   * each sensor a unique name.
   */
  private static synchronized void createSource(final int index) {
     try {
       client.storeSource(new Source(
          mSourceName + index,
          mSourceOwnerUri,
          true, // public
          false, // not virtual
          "Coordinates",
          "Location",
          "Description",
          null, // no additional props
          null), // no sub source
       true);
    }
    catch (Exception e) {
        System.out.println("Unable to store source " + mSourceName
            + index + ". Exception is " + e);
    }
  }

  /**
   * Checks the wattdepot server status. If not ok, exit the program.
   */
  public static void healthCheck() {
      if (client.isHealthy()) {
        System.out.println("WattDepot server found.");
      }
      else {
        System.out.println("WattDepot server NOT found.");
        System.out.println("This may be due to a bad URI,"
           + " or the server not being active.");
        System.exit(-1);
      }
  }
}
