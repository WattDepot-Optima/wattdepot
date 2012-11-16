package org.wattdepot.benchmark;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.NumberFormat;
import java.util.Hashtable;
import java.util.concurrent.CountDownLatch;
import org.wattdepot.client.WattDepotClient;
import org.wattdepot.server.Server;
import org.wattdepot.server.ServerProperties;

/**
 * An object representing a benchmark test for a specific
 * WattDepotEnum command.
 * @author Greg Burgess
 *
 */
public final class WattDepotBenchmark extends Thread {

    /** The path to the server properties file. **/
    private static final String PROTOCOL = "http://";
    /** Default Wattdepot server URI. **/
    private static String mServerURI = "http://localhost:8182/wattdepot/";
    /** Default Wattdepot server Username. **/
    private static String mServerUser = "foo@example.com";
    /** Default Wattdepot server Password. **/
    private static String mServerPwd = "CHANGE-ME";
    /** The client the test will use to construct sources. **/
    private static WattDepotClient client;
    /** The WattDepot Server to use. **/
    private static Server server;
    /** Number of threads to use. **/
    private int numThreads = 0;
    /** Number of ms to run for. **/
    private long ttl = 0;
    /** Name of the enum command to run.**/
    private String commandName;
    /** Literal used to convert from decimal to percentage. **/
    private static final int HUNDRED = 100;
   /**
   * Executes the benchmark.
   * @param cmdName String representing the WattDepotEnum command
   * to execute.
   * @param numberOfThreads Int representing the number of threads to use.
   * @param timeToRun long representing the number of ms to run the test for.
   * @throws Exception If something goes wrong with initializing
   *                     the server.
   */
    public WattDepotBenchmark(final String cmdName, final int numberOfThreads,
        final long timeToRun) throws Exception {
      commandName = cmdName;
      ttl = timeToRun;
      numThreads = numberOfThreads;
    }

    /**
     * Runs the thread.
     */
    public void run() {
      //Parse args & start server
      setup();
      CountDownLatch startSignal = new CountDownLatch(1);
      CountDownLatch doneSignal = new CountDownLatch(numThreads);
      WattDepotClientThread[] threads = new WattDepotClientThread[numThreads];

      System.out.println("Creating Threads...");
      for (int i = 0; i < numThreads; i++) {
        threads[i] = new WattDepotClientThread(mServerURI, mServerUser,
            mServerPwd, startSignal, doneSignal,
            WattDepotEnum.valueOf(commandName));
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
      System.out.println("Stopping Threads...");
      //Send a hint to the threads that they should stop running
      WattDepotClientThread.halt();
      //take a snapshot of the result at the deadline
      Hashtable<String, Long> result = WattDepotClientThread.getResults();
      //Wait for threads to comply
      try {
        doneSignal.await();
      }
      catch (InterruptedException e) {
        System.out.println("Encountered an error before all threads"
            + " finished.");
      }
      printResults(result);
    }

    /** Prints the results of the test to the screen and a dump file.
     * @param result The Hashtable given by the Benchmark test object.
     */
    private void printResults(final Hashtable<String, Long> result) {
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
      try {
        server.shutdown();
      }
      catch (Exception e1) {
        System.out.println("Error shutting down server.");
        System.exit(1);
      }
      System.out.println(toReturn);

      String dir = System.getProperty("user.home") + "\\benchmark\\"
      + commandName + "\\";

      try {
        new File(dir).mkdirs();

        System.out.println("Writing output to "
            + System.getProperty("user.home"));
        FileWriter fstream = new FileWriter(
            dir + numThreads + ".txt");
        BufferedWriter out = new BufferedWriter(fstream);
        out.write(commandName + " Benchmark");
        out.newLine();
        out.write(toReturn);
        out.newLine();
        out.close();
      }
      catch (Exception e) {
        System.err.println("Error: " + e.getMessage());
        }
  }

    /**
     * Does the required setup for the test to run.
     */
    private static void setup() {
      try {
        server = Server.newInstance();
      }
      catch (Exception e) {
        System.out.println("Error starting server");
        e.printStackTrace();
        System.exit(1);
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
