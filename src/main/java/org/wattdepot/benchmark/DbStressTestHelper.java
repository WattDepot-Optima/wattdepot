package org.wattdepot.benchmark;

import javax.xml.datatype.XMLGregorianCalendar;
import org.wattdepot.resource.property.jaxb.Property;
import org.wattdepot.resource.sensordata.jaxb.SensorData;
import org.wattdepot.resource.source.jaxb.Source;
import org.wattdepot.resource.user.jaxb.User;
import org.wattdepot.server.Server;

/**
 * Helper class for creating test data to be used in the database stress test.
 * @author George Lee
 */
public abstract class DbStressTestHelper {
  /** Server Object.  **/
  private static Server server;


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
   * Returns the Server object.
   * @return the Server object.
   */
  public static final Server getServer() {
    return server;
  }

  /**
   * Allows access to the private server object.
   * @param newServer the object to set the private server to.
   */
  public static final void setServer(final Server newServer) {
    server = newServer;
  }
}
