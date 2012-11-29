package org.wattdepot.benchmark;

import java.awt.geom.Rectangle2D;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.concurrent.CountDownLatch;
import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.batik.svggen.SVGGraphics2DIOException;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.wattdepot.client.WattDepotClient;
import org.wattdepot.server.Server;
import org.wattdepot.server.ServerProperties;

/**
 * An object representing a benchmark test for a specific
 * WattDepotEnum command.
 * @author Greg Burgess
 *
 */
public final class WattDepotBenchmark {

    /** The path to the server properties file. **/
    private static final String PROTOCOL = "http://";
    /** Default Wattdepot server URI. **/
    private static String mServerURI = "http://localhost:8182/wattdepot/";
    /** Default Wattdepot server Username. **/
    private static String mServerUser = "foo@example.com";
    /** Default Wattdepot server Password. **/
    private static String mServerPwd = "CHANGE-ME";
    /** The Sensor Name constant. **/
    private static String sensorName = "BenchmarkSensor";
    /** The Source Name constant. **/
    private static String sourceName = "BenchmarkSource";
    /** The client the test will use to construct sources. **/
    private static WattDepotClient client;
    /** The WattDepot Server to use. **/
    private static Server server;
    /** Number of ms to run for. **/
    private long timeToLive = 60000;
    /** Number of initial ms to ignore (Startup time).**/
    private long startUp = 300;
    /** Polling period in ms. **/
    private long pollPeriod = 1000;
    /** Number of threads to use. **/
    private int numThreads = 100;
    /** Name of the enum command to run.**/
    private WattDepotEnum command;
    /** ArrayList of results. **/
    private ArrayList<ResultSet> results;
    /** The path to the folder to print to. **/
    private String dir;
    /** Hashtable containing config information. **/
    private Hashtable<String, String> params;

   /**
   * Executes the benchmark.
   * @param cmdName String representing the WattDepotEnum command
   * to execute.
   * @param parameters Hashtable with various optional parameters.
   * @throws Exception If something goes wrong with initializing
   *                     the server.
   */
    public WattDepotBenchmark(final String cmdName,
        final Hashtable<String, String> parameters) throws Exception {
      try {
        command = WattDepotEnum.valueOf(cmdName);
      }
      catch (IllegalArgumentException e) {
        System.err.println("Invalid Command \"" + cmdName + "\".");
        System.exit(1);
      }
      //Deeply clone the hashtable, as Findbugs says simply assigning
      //it is a big security breach...
      params = new Hashtable<String, String>();
      Enumeration <String> keys = parameters.keys();
      String next = "";
      while (keys.hasMoreElements()) {
        next = keys.nextElement();
        params.put(next, parameters.get(next));
      }

      results = new ArrayList<ResultSet>();
      dir = System.getProperty("user.home") + "\\benchmark\\"
      + command + "\\";

      if (params != null) {
        if (params.containsKey("timeToLive")) {
          String ttl = params.get("timeToLive");
          timeToLive = new Long(ttl);
          System.out.println("Setting timeToLive to: " + ttl);
        }
        if (params.containsKey("dir")) {
          dir = params.get("dir");
          String d = params.get("dir");
          System.out.println("Setting output directory to: " + d);
        }
        if (params.containsKey("startUp")) {
          String strt = params.get("startUp");
          startUp = new Long(strt);
          System.out.println("Setting startUp to: " + strt);
        }
        if (params.containsKey("pollPeriod")) {
          String pp = params.get("pollPeriod");
          pollPeriod = new Long(pp);
          System.out.println("Setting pollPeriod to: " + pp);
        }
        if (params.containsKey("numThreads")) {
          String numThrds = params.get("numThreads");
          numThreads = new Integer(numThrds);
          System.out.println("Setting numThreads to: " + numThrds);
        }
      }
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
      //Do any required setup for the specific test.
      Object[] setupObjects = command.setup(client, numThreads, params);

      System.out.println("Creating Threads...");
      for (int i = 0; i < numThreads; i++) {
        threads[i] = new WattDepotClientThread(mServerURI, mServerUser,
            mServerPwd, startSignal, doneSignal,
            command, i, setupObjects);
        threads[i].start();
      }
      System.out.println("Starting threads...");
      startSignal.countDown();

      //START THE CLOCK!
      long currentTime = 0;

      //Wait for threads to stabilize
      try {
        Thread.sleep(startUp);
        System.out.println("Starting data collection...");
      }
      catch (InterruptedException e) {
        System.err.println("ERROR: Benchmark Thread Interupted.");
      }
      currentTime = startUp;
      while (currentTime < timeToLive) {
        //Record results
        System.out.println("Recording " + currentTime + "ms...");
        results.add(WattDepotClientThread.record(currentTime));
        currentTime += pollPeriod;
        //wait
        try {
          Thread.sleep(pollPeriod);
        }
        catch (InterruptedException e) {
          System.err.println("ERROR: Benchmark Thread Interupted.");
        }
      }
      //Record final result
      results.add(WattDepotClientThread.record(
          timeToLive - startUp));
      System.out.println("Stopping Threads...");
      //Send a hint to the threads that they should stop running
      WattDepotClientThread.halt();
      //Wait for threads to comply
      try {
        doneSignal.await();
      }
      catch (InterruptedException e) {
        System.out.println("Encountered an error before all threads"
            + " finished.");
      }
      printResults();

      //Teardown & reset for the next test
      WattDepotClientThread.reset();
      //Shutdown server
      System.out.println("Waiting for WDserver to shutdown...");
      try {
        server.shutdown();
      }
      catch (Exception e1) {
        System.out.println("Error shutting down server.");
        System.exit(1);
      }
    }

    /** Prints the results of the test to the screen,
     *  a graph, and a dump file.
     */
    private void printResults() {
      long errors = 0;
      long successes = 0;
      long timestamp = 0;
      final XYSeries successSeries = new XYSeries("Successes");
      final XYSeries errorSeries = new XYSeries("\nErrors");
      StringBuffer toReturn = new StringBuffer();
      Hashtable<String, Long> temp;

      toReturn.append("Results:");
      for (ResultSet rslt : results) {
        temp = rslt.getResults();
        successes = temp.get("requestCount");
        errors = temp.get("errorCount");
        timestamp = temp.get("time");

        successSeries.add(timestamp, successes);
        errorSeries.add(timestamp, errors);

        toReturn.append("\nTime: ");
        toReturn.append(timestamp);
        toReturn.append("ms\nSuccess: ");
        toReturn.append(successes);
        toReturn.append("\nErrors: ");
        toReturn.append(errors);
        toReturn.append("\n");
      }

      //Print text results
      System.out.println(toReturn);

      try {
        boolean newDir = new File(dir).mkdirs();
        if (newDir) {
          System.out.println("Creating directory " + dir);
        }
        System.out.println("Writing output to "
            + dir);
        FileWriter fstream = new FileWriter(
            dir + numThreads + ".txt");
        BufferedWriter out = new BufferedWriter(fstream);
        out.write(command + " Benchmark");
        out.newLine();
        out.write(toReturn.toString());
        out.newLine();
        out.close();
      }
      catch (Exception e) {
        System.err.println("Error: " + e.getMessage());
        }

      //Print graphs
      final XYSeriesCollection dataset1 = new XYSeriesCollection();
      dataset1.addSeries(successSeries);
      dataset1.addSeries(errorSeries);
      JFreeChart chart1 = Parser.createChart("HTTP Requests vs Time",
          "Time (ms)", "#Requests", dataset1);
      Parser.createSVG(chart1, command + "_" + numThreads, dir);
  }

    /** Returns the final ResultSet from the test.
     * @return The last ResultSet from the test.
     */
    public ResultSet getFinalResults() {
      return results.get(results.size() - 1);
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

  

    /**
     * Returns the Sensor Name constant.
     * @return The Sensor Name constant
     */
    public static String getSensorName() {
      return sensorName;
    }

    /**
     * Returns the Source Name constant.
     * @return The Source Name constant.
     */
    public static String getSourceName() {
      return sourceName;
    }
}
