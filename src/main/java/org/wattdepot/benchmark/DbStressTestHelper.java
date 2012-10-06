package org.wattdepot.benchmark;

import static org.wattdepot.server.ServerProperties.DB_IMPL_KEY;
import javax.xml.datatype.XMLGregorianCalendar;
import org.wattdepot.client.WattDepotClient;
import org.wattdepot.resource.property.jaxb.Property;
import org.wattdepot.resource.sensordata.jaxb.SensorData;
import org.wattdepot.resource.source.jaxb.Source;
import org.wattdepot.resource.user.jaxb.User;
import org.wattdepot.server.Server;
import org.wattdepot.server.db.DbManager;

/**
 * Helper class for creating test data to be used in the database stress test.
 * @author George Lee
 */
public abstract class DbStressTestHelper {
  /** **/
  private static Server server;
  /** **/
  private static DbManager manager;


  /**
   * Helper to generate test sensor data for the stress test.
   * @param tstamp The timestamp for this sensor data.
   * @param source The source to use.
   * @return Generated SensorData with a made up POWER_CONSUMED value.
   * @throws Exception if the sensor data could not be created.
   */
  protected static SensorData createSensorData(
      final XMLGregorianCalendar tstamp, final Source source)
      throws Exception {
    return new SensorData(tstamp, "JUnit", source.toUri(server), new Property(
        SensorData.POWER_CONSUMED, "10000"));
  }

  /**
   * Helper to generate a test user for the stress test.
   * @param email The email address of the user to create.
   * @return The user with a bogus property and password.
   */
  protected static User makeTestUser(final String email) {
    User user = new User(email, "secret", false, null);
    user.addProperty(new Property("awesomeness", "total"));
    return user;
  }

  /**
   * Helper to create a test source for the stress test.
   * @param name The name of the source.
   * @param user The name of the user that created the source.
   * @param isPublic True if this should be a public source, false otherwise.
   * @param isVirtual True if this should be a virtual source, false otherwise.
   * @return A test source with a bogus carbon intensity property.
   */
  protected static Source createTestSource(final String name,
      final User user, final boolean isPublic,
      final boolean isVirtual) {
    Source source =
        new Source(name, user.toUri(server), isPublic, isVirtual,
            "21.30078,-157.819129,41",
            "Made up location", "Obvius-brand power meter", null, null);
    source.addProperty(new Property(Source.CARBON_INTENSITY, "294"));
    return source;
  }

  /**
   * Returns the DbManager.
   * @return the DbManager.
   */
  public static final DbManager getDBM() {
    return manager;
  }

  /**
   * Returns the Server object.
   * @return the Server object.
   */
  public static final Server getServer() {
    return server;
  }
}

/**
 * Wrapper class to quickly instantiate multiple clients.
 * Threads need to override run method to execute.
 * @author George Lee
 *
 */
class ClientThread extends Thread {
  /** **/
  private WattDepotClient client;
  /** **/
  private long threadId;


  /**
   * Constructor for the client thread.
   * Instantiates the thread with the server used by the stress test.

  public ClientThread(int id) {
    this.client =
        new WattDepotClient(ParallelStressTest.server.getHostName(),
        ParallelStressTest.adminEmail,
        ParallelStressTest.adminPassword);
    this.threadId = id;
  }
  */

  /** Returns the client (WattDepotClient).
   * @return The WattDepotClient.
   */
  public WattDepotClient getClient() {
    return client;
  }

  /** Returns the Thread ID.
   * @return The Thread id
   */
  public long getThreadId() {
    return threadId;
  }
}


/**
 * Wrapper class to instantiate multiple DbManagers.
 * Threads will need to override run method to execute.
 * @author George Lee
 *
 */
class ManagerThread extends Thread {
  /** **/
  private DbManager manager;
  /** **/
  private long id = 0;

  /**
   * Constructor for the thread.
   * @param server The server implementation used to set up the thread.
   * @param idnum This object's id.
   */
  ManagerThread(final Server server, final long idnum) {
    this.manager = new DbManager(server,
        server.getServerProperties().get(DB_IMPL_KEY), true);
    this.id = idnum;
  }


  /**
   * Returns the DbManager.
   * @return the DbManager.
   */
  public DbManager getDBM() {
    return manager;
  }


  /**
   * Returns the ID.
   * @return The thread's ID.
   */
  public long getID() {
    return id;
  }
}
