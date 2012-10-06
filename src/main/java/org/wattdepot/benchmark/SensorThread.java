package org.wattdepot.benchmark;

import java.util.concurrent.CountDownLatch;
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
  /** CountDownLatch to wait on, so that all threads start at once. **/
  private final CountDownLatch startSignal;
  /** CountDownLatch to notify calling thread that this thread has
   ** terminated. **/
  private final CountDownLatch doneSignal;
  /** Hint to the Thread that it should stop. **/
  private static volatile boolean shouldStop = false;

  /**
   * Constructor.
   * @param newSensor The sensor to run.
   * @param start A CountDownLatch to wait on, so all threads start at once.
   * @param done A countDownLatch to notify the calling thread that this thread
   * is done executing.
   */
  public SensorThread(final CountDownLatch start,
      final CountDownLatch done, final MultiThreadedSensor newSensor) {
    sensor = newSensor;
    startSignal = start;
    doneSignal = done;
  }

  /**
   * Continuously runs the sensor as specified.
   */
  @Override
  public final void run() {

    try {
      startSignal.await();
      while (!shouldStop) {
        sensor.run();
      }
    }
    catch (InterruptedException e) {
      System.out.println("Thread " + this.sensor + " has been interrupted");
    }
     doneSignal.countDown();
  }

  /** Hints to the Thread that it should halt. **/
  public static final void halt() {
    shouldStop = true;
  }
}
