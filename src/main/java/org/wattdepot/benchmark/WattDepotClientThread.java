package org.wattdepot.benchmark;

import java.util.Hashtable;
import java.util.concurrent.CountDownLatch;
import org.wattdepot.client.WattDepotClient;

/**
 * A Thread that runs a method associated with the WattDepotClient object.
 * Utilizes WattDepotEnums to map Enums to parameterized methods.
 * @author Greg Burgess
 *
 */
public class WattDepotClientThread extends Thread {
  /** CountDownLatch to wait on, so that all threads start at once. **/
  private final CountDownLatch startSignal;
  /** CountDownLatch to notify calling thread that this thread has
   ** terminated. **/
  private final CountDownLatch doneSignal;
  /** Hint to the Thread that it should stop. **/
  private static volatile boolean shouldStop = false;
  /** The WattDepotClient to use. **/
  private WattDepotClient client;
  /** Enum holding the WattDepotClient method to call.**/
  private WattDepotEnum method;
  /** Object to hold result set. **/
  private static ResultSet result = new ResultSet();
  /** Parameters passed in as an Object array. **/
  private Object[] parameters;
  /** Thread's ID. **/
  private int id;
  /** Number of times a thread has executed. **/
  private int numExecutions;

  /**
   * Constructor.
   * @param start A CountDownLatch to wait on, so all threads start at once.
   * @param done A countDownLatch to notify the calling thread that this thread
   * is done executing.
   * @param methodToExecute A WattDeotEnum holding the method to execute.
   * @param uri The URI for the WattDepotServer.
   * @param user The username for server authentication.
   * @param password The password for server authentication.
   * @param params Optional parameters to pass to the threads.
   * @param id The thread's ID.
   */
  public WattDepotClientThread(final String uri, final String user,
      final String password, final CountDownLatch start,
      final CountDownLatch done, final WattDepotEnum methodToExecute,
      final int ID, final Object... params) {
    client = new WattDepotClient(uri, user, password);
    startSignal = start;
    doneSignal = done;
    method = methodToExecute;
    parameters = params;
    id = ID;
    numExecutions = 0;
  }

  /**
   * Returns a ResultSet representing the current state of execution.
   * @return a copy of the Class's internal ResultSet.
   * @param timeStamp The time to record for this state.
   */
  public static ResultSet record(final long timeStamp) {
    return result.record(timeStamp);
  }

  /**
   * Initializes the class for a new run.
   */
  public static void reset() {
    result = new ResultSet();
    shouldStop = false;
  }
  /**
   * Continuously executes the method.
   */
  @Override
  public final void run() {

    try {
      startSignal.await();
      while (!shouldStop) {
        method.execute(result, client, id, numExecutions, parameters);
        numExecutions++;
      }
    }
    catch (InterruptedException e) {
      System.out.println("Thread " + id + " has been interrupted");
    }
     doneSignal.countDown();
  }

  /** Hints to the Thread that it should halt. **/
  public static final void halt() {
    shouldStop = true;
  }

  /**
   * Returns the ResultSet.
   * @return The results of the test in a ResultSet
   * object.
   */
  public static Hashtable<String, Long> getResults() {
    return result.getResults();
  }
}
