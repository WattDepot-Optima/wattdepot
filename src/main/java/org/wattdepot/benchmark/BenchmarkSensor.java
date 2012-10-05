package org.wattdepot.benchmark;

import java.util.Hashtable;
import org.wattdepot.datainput.SensorSource;
import org.wattdepot.resource.sensordata.jaxb.SensorData;
import org.wattdepot.sensor.hammer.HammerSensor;
import org.wattdepot.util.tstamp.Tstamp;

/**
 * A Sensor that tracks successful requests and total errors.
 * Based on HammerSensor by Robert Brewer and Andrea Connell.
 * @author Greg Burgess
 */

public class BenchmarkSensor extends HammerSensor {

  /** The number of successful requests. **/
  private static long requestCount = 0;
  /** The number of errors generated. **/
  private static long errorCount = 0;
  /** A monitor used for concurrency. **/
  private static Object monitor = new Object();

  /**
   * Initializes a BenchmarkSensor by calling the constructor for HamerSensor.
   * @param wattDepotUri URI of the WattDepot server to send this sensor data
   * to.
   * @param wattDepotUsername Username to connect to the WattDepot server with.
   * @param wattDepotPassword Password to connect to the WattDepot server with.
   * @param sensorSource The SensorSource containing configuration settings.
   * for this sensor.
   * @param debug If true then display new sensor data when sending it.
   */
  public BenchmarkSensor(final String wattDepotUri,
      final String wattDepotUsername,
      final String wattDepotPassword,
      final SensorSource sensorSource,
      final boolean debug) {
    super(wattDepotUri, wattDepotUsername, wattDepotPassword, sensorSource,
        debug);
  }


  /**
   * Generates fake sensor data, then tries to do a put to the WD server.
   */
  @Override
  public final void run() {
    SensorData data = generateFakeSensorData();
    if (data != null) {
      try {
        this.client.storeSensorData(data);
        synchronized (monitor) {
          requestCount++;
        }
      } catch (Exception e) {
        System.err
            .format(
                "%s: Unable to store sensor data from %s due to exception "
                + "(%s), " + "hopefully this is temporary.%n",
                Tstamp.makeTimestamp(), this.sourceKey, e);
        synchronized (monitor) {
          errorCount++;
        }
      }
    }
  }

  /**
   * Returns a Hashtable containing results for this class.
   * @return dict A dictionary containing the keys
   * 'errorCount' and 'requestCount', 'errorCount'
   * holds the total number of errors, and 'requestCount'
   * contains the number of successful (non-erroneous)
   * requests.
   */
  public static final Hashtable<String, Long> getResults() {
    Hashtable<String, Long> dict = new Hashtable<String, Long>();
    dict.put("errorCount", errorCount);
    dict.put("requestCount", requestCount);
    return dict;
  }

}
