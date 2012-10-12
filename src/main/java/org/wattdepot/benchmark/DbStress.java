package org.wattdepot.benchmark;
import static org.wattdepot.server.ServerProperties.DB_IMPL_KEY;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Date;
import java.util.Random;
import org.wattdepot.resource.sensordata.jaxb.SensorData;
import org.wattdepot.resource.source.jaxb.Source;
import org.wattdepot.resource.user.jaxb.User;
import org.wattdepot.server.Server;
import org.wattdepot.server.db.DbManager;
import org.wattdepot.util.tstamp.Tstamp;

/**
 * Stress test for loading and retrieving data from WattDepot.
 * so that it can be run in Eclipse.
 * @author George Lee

 * Modified to run as non Junit Tests.
 * @author Jordan Do
 * @author Greg Burgess
 */

public class DbStress extends DbStressTestHelper {

    /** Amount of data to store. Def = 100.**/
    private static long dataAmmount = 0;
    /** Time between data. Def = 15000.**/
    private static long dataInterval = 0;
    /** Starting date for data. **/
    private static final long START_DATE = new Date().getTime();
    /** Source name.  **/
    private static final String TEST_SOURCE_NAME = "hale-test";
    /** Iteration count. Def = 1000.**/
    private static long testIterations = 0;
    /** Convert s to ms.  **/
    private static final long S_TO_MS = 60000;
    /** Convert mins to days.  **/
    private static final long MINS_IN_DAY = 60 * 24;
    /** The DbManagement object. **/
    private static DbManager manager = null;
    /** Error message for bad command line invocation. **/
    private static final String ARG_ERROR_MSG = "Usage is: "
        + "<Number of Iterations to Run (long > 0)> "
        + "<Data interval in ms (long > 0)> "
        + "<Number of peices of data to store (long > 0)>";

    /**
     * Sets up the server and inserts data into the current database
     * implementation.
     * @param args Command line args.
     * @throws Exception if there is an error setting the server up.
     */
    public static void main(final String[] args) throws Exception {
      //parse command line args
      if (args.length < 3) {
        System.out.println(ARG_ERROR_MSG);
        System.exit(0);
      }
      else {
        try {
          testIterations = Integer.parseInt(args[0]);
          dataInterval = Long.parseLong(args[1]);
          dataAmmount = Long.parseLong(args[2]);
        }
        catch (NumberFormatException e) {
            System.out.println(ARG_ERROR_MSG);
            System.exit(0);
        }
      }

      if (!(testIterations > 0
          && dataInterval > 0
          && dataAmmount > 0)) {
        System.out.println(ARG_ERROR_MSG);
        System.exit(0);
      }

      setServer(Server.newTestInstance());
      manager =
          new DbManager(getServer(),
              getServer().getServerProperties().get(DB_IMPL_KEY),
              true);
      // Initialize test source(s).
      User user = makeTestUser("test@test.org");
      manager.storeUser(user);
      final Source source = createTestSource(TEST_SOURCE_NAME, user,
         true, false);
      manager.storeSource(source);
      SensorData testData;

      // Insert data serially
      Date testStart = new Date();
      for (int i = 0; i < dataAmmount; i++) {
        testData =
            createSensorData(Tstamp.makeTimestamp(DbStress.START_DATE
                + (i * dataInterval)),
                source);
        manager.storeSensorDataNoCache(testData);
      }

      Date testEnd = new Date();
      double msElapsed = testEnd.getTime() - testStart.getTime();
      System.out.format("Time to insert %d rows serially: %.1f ms%n",
          dataAmmount / 2, msElapsed);
      try {
        System.out.println("Writing output to " + System.getProperty("user.home"));
        FileWriter fstream = new FileWriter(System.getProperty("user.home") + "\\output.txt");
        BufferedWriter out = new BufferedWriter(fstream);
        out.write("Database benchmark");
        out.newLine();
        out.write("Time to insert " + dataAmmount / 2 + " rows serially: " + .1 * msElapsed + " ms");
        out.newLine();
        out.write(randomRetrieval(manager));
        out.newLine();
        out.write(randomDailyIndexes(manager));
        out.close();
        }
      catch (Exception e) {
        System.err.println("Error: " + e.getMessage());
        }
      //randomRetrieval(manager);
      //randomDailyIndexes(manager);
    }

    /**
     * Randomly retrieves a row of data from the database.
     * @param dbm The DBM to use.
     *      * @return A string containing the result
     */
    public static final String randomRetrieval(final DbManager dbm) {
      Random random = new Random();
      long offset = 0;
      Date testStart = new Date();
      random.setSeed(testStart.getTime());
      for (int i = 0; i < testIterations; i++) {
        offset = (random.nextLong() * dataInterval) % dataAmmount;
        dbm
            .getSensorData(TEST_SOURCE_NAME, Tstamp.makeTimestamp(
                DbStress.START_DATE + offset));
      }
      Date testEnd = new Date();
      double msElapsed = testEnd.getTime() - testStart.getTime();
      System.out.print("Time to randomly query the database");
      System.out.format(" %d times: %.1f ms%n",
          testIterations, msElapsed);
      String toReturn = "Time to randomly query the database" +
          String.format(" %d times: %.1f ms",
          testIterations, msElapsed);
      return toReturn;
    }

    /**
     * Randomly retrieves a day's worth of information from WattDepot.
     * @param dbm The DBM to use.
     * @return A string containing the result
     * @throws Exception If there's an error shutting down the server.
     */
    public static final String randomDailyIndexes(final DbManager dbm)
      throws Exception {

      Random random = new Random();
      long startOffset = 0;
      // Day's worth of data.
      long timePeriod = (S_TO_MS / dataInterval) * MINS_IN_DAY;

      Date testStart = new Date();
      random.setSeed(testStart.getTime());
      for (int i = 0; i < testIterations; i++) {
        startOffset = (random.nextLong() * dataInterval) % dataAmmount;
        if (startOffset < timePeriod) {
          dbm.getSensorDataIndex(TEST_SOURCE_NAME,
              Tstamp.makeTimestamp(startOffset),
              Tstamp.makeTimestamp(startOffset + timePeriod));
        }
        else {
          dbm.getSensorDataIndex(TEST_SOURCE_NAME,
              Tstamp.makeTimestamp(startOffset - timePeriod),
              Tstamp.makeTimestamp(startOffset));
        }
      }

      Date testEnd = new Date();
      double msElapsed = testEnd.getTime() - testStart.getTime();
      System.out.print("Time to randomly retrieve indexes of size");
      System.out.format(
          " %d b from the database %d times: %.1f ms%n",
          timePeriod, testIterations, msElapsed);
      getServer().shutdown();
      String toReturn = "Time to randomly retrieve indexes of size" + 
          String.format(" %d b from the database %d times: %.1f ms",
          timePeriod, testIterations, msElapsed);
      return toReturn;
    }
  }
