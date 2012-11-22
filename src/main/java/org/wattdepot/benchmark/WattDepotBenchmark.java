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

   /**
   * Executes the benchmark.
   * @param cmdName String representing the WattDepotEnum command
   * to execute.
   * @param params Hashtable with various optional parameters.
   * @throws Exception If something goes wrong with initializing
   *                     the server.
   */
    public WattDepotBenchmark(final String cmdName,
        final Hashtable<String, String> params) throws Exception {
      try {
        command = WattDepotEnum.valueOf(cmdName);
      }
      catch (IllegalArgumentException e) {
        System.err.println("Invalid Command \"" + cmdName + "\".");
        System.exit(1);
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

      System.out.println("Creating Threads...");
      for (int i = 0; i < numThreads; i++) {
        threads[i] = new WattDepotClientThread(mServerURI, mServerUser,
            mServerPwd, startSignal, doneSignal,
            command);
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
        System.out.println("Recording..." + currentTime);
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
      String toReturn = "Results:";
      Hashtable<String, Long> temp;


      for (ResultSet rslt : results) {
        temp = rslt.getResults();
        successes = temp.get("requestCount");
        errors = temp.get("errorCount");
        timestamp = temp.get("time");

        successSeries.add(timestamp, successes);
        errorSeries.add(timestamp, errors);

        toReturn += "\nTime: ";
        toReturn += timestamp;
        toReturn += "ms\nSuccess: ";
        toReturn += successes;
        toReturn += "\nErrors: ";
        toReturn +=  errors;
        toReturn += "\n";
      }

      //Print text results
      System.out.println(toReturn);

      try {
        new File(dir).mkdirs();

        System.out.println("Writing output to "
            + dir);
        FileWriter fstream = new FileWriter(
            dir + numThreads + ".txt");
        BufferedWriter out = new BufferedWriter(fstream);
        out.write(command + " Benchmark");
        out.newLine();
        out.write(toReturn);
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
      JFreeChart chart1 = createChart("HTTP Requests vs Time",
          "Time (ms)", "#Requests", dataset1);
      createSVG(chart1, command + "_" + numThreads);
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

    /** Creates a Chart object.
     * @param title Title of the chart.
     * @param xAxisLabel Label for x axis.
     * @param yAxisLabel Label for y axis.
     * @param dataset Dataset to add to graph.
     * @return A Chart object.
     */
    private JFreeChart createChart(final String title,
        final String xAxisLabel, final String yAxisLabel,
        final XYSeriesCollection dataset) {
      final JFreeChart chart = ChartFactory.createXYLineChart(
          title,      // chart title
          xAxisLabel,                      // x axis label
          yAxisLabel,                      // y axis label
          dataset,                  // data
          PlotOrientation.VERTICAL,
          true,                     // include legend
          true,                     // tooltips
          false                     // urls
      );
      return chart;
    }

    /**
     * Creates and prints an SVG representation of the chart.
     * @param chart Chart to print.
     * @param name File name of the chart.
     */
    private void createSVG(final JFreeChart chart, final String name) {
      // THE FOLLOWING CODE BASED ON THE EXAMPLE IN THE BATIK DOCUMENTATION...
      // Get a DOMImplementation
      DOMImplementation domImpl
          = GenericDOMImplementation.getDOMImplementation();

      // Create an instance of org.w3c.dom.Document
      Document document = domImpl.createDocument(null, "svg", null);

      // Create an instance of the SVG Generator
      SVGGraphics2D svgGenerator = new SVGGraphics2D(document);

      // set the precision to avoid a null pointer exception in Batik 1.5
      svgGenerator.getGeneratorContext().setPrecision(6);

      // Ask the chart to render into the SVG Graphics2D implementation
      chart.draw(svgGenerator, new Rectangle2D.Double(0, 0, 400, 300), null);

      // Finally, stream out SVG to a file using UTF-8 character to
      // byte encoding
      boolean useCSS = true;
      Writer out;
      try {
        out = new OutputStreamWriter(
            new FileOutputStream(new File(dir + name + ".svg")), "UTF-8");
            svgGenerator.stream(out, useCSS);
      }
      catch (UnsupportedEncodingException e) {
        e.printStackTrace();
      }
      catch (FileNotFoundException e) {
        e.printStackTrace();
      }
      catch (SVGGraphics2DIOException e) {
        e.printStackTrace();
      }
    }
}
