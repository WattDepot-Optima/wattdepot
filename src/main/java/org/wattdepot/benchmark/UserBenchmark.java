package org.wattdepot.benchmark;

import static org.wattdepot.server.ServerProperties.ADMIN_EMAIL_KEY;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import org.wattdepot.server.Server;
import org.wattdepot.server.ServerProperties;
import org.wattdepot.server.db.DbManager;
import org.wattdepot.test.DataGenerator;
import org.wattdepot.util.tstamp.Tstamp;

/**
 * Provides a benchmarking tool for measuring the response times for the
 * user-side URI interface to the WattDepot Server.  Measures the response time
 * in milliseconds for serialized requests covering: single point data,
 * aggregate data, and aggregate resource data.
 *
 * @author Keone Hirade
 * @author Greg Burgess
 *
 */
public class UserBenchmark {
    /** Set HTTP timeout cutoff to 10s.  **/
    public static final int HTTP_TIMEOUT = 10000;
    /** The path to the server properties file. **/
    private static final String PROTOCOL = "http://";
    /** Default Wattdepot server URI. **/
    private static String mServerURI =
      "http://localhost:8182/wattdepot/sources/";
    /** A day represented as ms. **/
    private static final long DAY_TO_MS = 7 * 3600 * 1000;
    /** Two Minutes represented as ms.  **/
    private static final long TEN_MINS_IN_MS = 10 * 60 * 1000;
    /** Thirty Minutes represented as ms.  **/
    private static final long THIRTY_MINS_IN_MS = 30 * 60 * 1000;
    /** Number of times to run the test.  **/
    private static double dataSetSize = 1;
    /** Default name of the sensor to be used. **/
    private static String sensorName = "source01";
    /** Error message for bad command line invocation. **/
    private static final String ARG_ERROR_MSG = "Usage is: "
        + "<Number of times to run test (integer > 0)> ";
    /**
     * Executes an HTTP GET request on a given url, returning
     * the response time in milliseconds if successful, -1
     * otherwise.
     *
     * @param url The URL to point the GET request at.
     * @return How long the request took in milliseconds
     * @throws Exception If something went wrong while executing.
     */
    public final long getRequest(final String url) throws Exception {
        HttpURLConnection connection = null;
        URL serverAddress = null;
        long startTime = 0;
        long endTime = 0;
        long elapsedTime = 0;
        int responseCode = 0;
        serverAddress = new URL(url);
        //set up out connection
        connection = null;
        //Set up the initial connection
        startTime = System.currentTimeMillis();
        connection =
        (HttpURLConnection) serverAddress.openConnection();
        serverAddress.openConnection();
        connection.setRequestMethod("GET");
        connection.setDoOutput(true);
        connection.setReadTimeout(HTTP_TIMEOUT);
        connection.connect();
        responseCode = connection.getResponseCode();
        if (responseCode != 200) {
          System.err.println("Encountered error: HTTP"
              + connection.getResponseCode());
          System.exit(0);
        }
        endTime = System.currentTimeMillis();
        elapsedTime = endTime - startTime;
        //close the connection, set all objects to null
        connection.disconnect();
        connection = null;
        return elapsedTime;
    }

    /**
     * Executes the benchmark.
     * @param args command line args.
     * @throws Exception If there's a problem starting the server.
     */
    public static void main(final String[] args) throws Exception {

    //parse command line args
      if (args.length < 1) {
        System.out.println(ARG_ERROR_MSG);
        System.exit(0);
      }
      try {
        dataSetSize = Integer.parseInt(args[0]);
      }
      catch (NumberFormatException e) {
          System.out.println(ARG_ERROR_MSG);
          System.exit(0);
      }

      //Build Server URI
      ServerProperties properties = null;
      properties = new ServerProperties();
      mServerURI = PROTOCOL
        + properties.get("wattdepot-server.hostname") + ":"
        + properties.get("wattdepot-server.port") + "/"
        + properties.get("wattdepot-server.context.root")
        + "/sources/";

      //Check for active servers before we start ours
      UserBenchmark bench = new UserBenchmark();
      try {
        bench.getRequest(mServerURI);
        System.err.println("Active server detected."
            + "  Please close all instances of WattDepotServer");
         System.exit(1);
      }
      catch (ConnectException e) {
        System.out.println("Checking For Open Servers...");
      }

      Server server = Server.newInstance();
      String adminEmail = server.getServerProperties().get(ADMIN_EMAIL_KEY);
      DbManager dbManager = (DbManager) server.getContext()
        .getAttributes().get("DbManager");
      String adminUserUri = dbManager.getUser(adminEmail).toUri(server);
      DataGenerator test = new DataGenerator(dbManager, adminUserUri, server);

      long startTime = System.currentTimeMillis();
      //long twoMinsAgo = startTime - TWO_MINS_IN_MS;
      long endTime = startTime - DAY_TO_MS;
      System.out.println("Storing data...");
      test.storeData(Tstamp.makeTimestamp(endTime), Tstamp
          .makeTimestamp(startTime), 1);


      //Extend the uri to include a sensor name
      mServerURI += sensorName;
      long singlePointCachedTime = 0;
      long singlePointUncachedTime = 0;
      long aggDataTime = 0;
      long resourceDataTime = 0;

      for (int i = 0; i < dataSetSize; i++) {
          //Single point cached response time
          singlePointCachedTime += bench.getRequest(mServerURI
              + "/sensordata/" + Tstamp.makeTimestamp(startTime - 60000));

          //Single point uncached response time
          singlePointUncachedTime = bench.getRequest(mServerURI
              + "/sensordata/" + Tstamp.makeTimestamp(endTime));


          //Aggregate data rsponse time
          aggDataTime = bench.getRequest(mServerURI
              + "/carbon/?"
              + "startTime=" + Tstamp.makeTimestamp(startTime
                  - THIRTY_MINS_IN_MS)
              + "&endTime=" + Tstamp.makeTimestamp(startTime
                  - THIRTY_MINS_IN_MS + TEN_MINS_IN_MS));


          //Resource data response time
          resourceDataTime = bench.getRequest(mServerURI
              + "/sensordata/?"
              + "startTime=" + Tstamp.makeTimestamp(startTime
                  - THIRTY_MINS_IN_MS)
              + "&endTime=" + Tstamp.makeTimestamp(startTime
                  - THIRTY_MINS_IN_MS + TEN_MINS_IN_MS));
      }

      server.shutdown();

      String toReturn = "";
      toReturn += "Response time(ms) for point data (cached): "
          + singlePointCachedTime / dataSetSize + " ms\n";
      toReturn += "Response time(ms) for a long time ago (uncached): "
          + singlePointUncachedTime / dataSetSize + " ms\n";
      toReturn += "Response time(ms) of resource request"
          + "for 30 data points: "
          + resourceDataTime / dataSetSize + " ms\n";
      toReturn += "Response time(ms) of aggregate request"
          + " for 30 data points: "
          + aggDataTime / dataSetSize + " ms\n";

      System.out.println(toReturn);
      try {
        System.out.println("Writing output to "
            + System.getProperty("user.home"));
        FileWriter fstream = new FileWriter(
            System.getProperty("user.home") + "\\userBench.txt");
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
}
