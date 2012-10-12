package org.wattdepot.benchmark;
import static org.wattdepot.server.ServerProperties.DB_IMPL_KEY;
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

    /** Amount of data to store. **/
    private static final long DATA_AMOUNT = 100;
    /** Time between data.**/
    private static final long DATA_INTERVAL = 15000;
    /** Starting date for data. **/
    private static final long START_DATE = new Date().getTime();
    /** Source name.  **/
    private static final String TEST_SOURCE_NAME = "hale-test";
    /** Iteration count. **/
    private static final long TEST_ITERATIONS = 1000;
    /** Convert s to ms.  **/
    private static final long S_TO_MS = 60000;
    /** Convert mins to days.  **/
    private static final long MINS_IN_DAY = 60 * 24;
    /** The DbManagement object. **/
    private static DbManager manager = null;

    /**
     * Sets up the server and inserts data into the current database
     * implementation.
     * @param args Command line args.
     * @throws Exception if there is an error setting the server up.
     */
    public static void main(final String[] args) throws Exception {
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
      for (int i = 0; i < DATA_AMOUNT; i++) {
        testData =
            createSensorData(Tstamp.makeTimestamp(DbStress.START_DATE
                + (i * DATA_INTERVAL)),
                source);
        manager.storeSensorDataNoCache(testData);
      }

      Date testEnd = new Date();
      double msElapsed = testEnd.getTime() - testStart.getTime();
      System.out.format("Time to insert %d rows serially: %.1f ms%n",
          DATA_AMOUNT / 2, msElapsed);

      randomRetrieval(manager);
      randomDailyIndexes(manager);
    }

    /**
     * Randomly retrieves a row of data from the database.
     * @param dbm The DBM to use.
     */
    public static final void randomRetrieval(final DbManager dbm) {
      Random random = new Random();
      long offset = 0;
      Date testStart = new Date();
      random.setSeed(testStart.getTime());
      for (int i = 0; i < TEST_ITERATIONS; i++) {
        offset = (random.nextLong() * DATA_INTERVAL) % DATA_AMOUNT;
        dbm
            .getSensorData(TEST_SOURCE_NAME, Tstamp.makeTimestamp(
                DbStress.START_DATE + offset));
      }
      Date testEnd = new Date();
      double msElapsed = testEnd.getTime() - testStart.getTime();
      System.out.print("Time to randomly query the database");
      System.out.format(" %d times: %.1f ms%n",
          TEST_ITERATIONS, msElapsed);
    }

    /**
     * Randomly retrieves a day's worth of information from WattDepot.
     * @param dbm The DBM to use.
     * @throws Exception If there's an error shutting down the server.
     */
    public static final void randomDailyIndexes(final DbManager dbm)
      throws Exception {

      Random random = new Random();
      long startOffset = 0;
      // Day's worth of data.
      long timePeriod = (S_TO_MS / DATA_INTERVAL) * MINS_IN_DAY;

      Date testStart = new Date();
      random.setSeed(testStart.getTime());
      for (int i = 0; i < TEST_ITERATIONS; i++) {
        startOffset = (random.nextLong() * DATA_INTERVAL) % DATA_AMOUNT;
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
          timePeriod, TEST_ITERATIONS, msElapsed);
      getServer().shutdown();
    }
  }
