package org.wattdepot.test;

import org.wattdepot.sensor.MultiThreadedSensor;

/**
 * A Thread that runs a Sensor.  This is necessary as the current
 * MultiThreadedSensor class will continuously run only sensors attached to
 * sources that were defined in the client.properties file.
 * @author Greg Burgess
 *
 */
public class SensorThread extends Thread {
  /** The Sensor the thread will execute. **/
  private MultiThreadedSensor sensor;

  /**
   * Constructor.
   * @param newSensor The sensor to run.
   */
  public SensorThread(final MultiThreadedSensor newSensor) {
    sensor = newSensor;
  }

  /**
   * Continuously runs the sensor as specified.
   */
  public final void run() {
    //Start timer
    //Set some run condition
    while (true) {
      sensor.run();
    }
    //Stop timer
  }
}
