package org.wattdepot.benchmark;

import java.util.Hashtable;

/**
 * Keeps track of the number of successful HTTP requests and
 * the number of errors generated.
 * @author Greg Burgess
 */
public class ResultSet {
  /** The number of successful HTTP requests. **/
  private long success = 0;
  /** The number of errors generated. **/
  private long errors = 0;
  /** The time since the test started. **/
  private long time = 0;

  /**
   * Initializes ResultSet to 0.
   */
  public ResultSet() {
    success = 0;
    errors = 0;
    time = 0;
  }

  /** Create a ResultSet on demand.
   * @param succ Number of Successful HTTP requests.
   * @param error Number of errors.
   * @param timeStamp Number Current Time.
   */
  public ResultSet(final long succ, final long error, final long timeStamp) {
    success = succ;
    errors = error;
    time = timeStamp;
  }

  /** Create a copy of the ResultSet at the current time.
   * @param timeStamp The timestamp to record for this state.
   * @return returns a copy of this result set's current state.
   */
  public final synchronized ResultSet record(final long timeStamp) {
    return new ResultSet(success, errors, timeStamp);
  }

  /**
   * Synchronized method to increment number of successes.
   */
  public final synchronized void succes() {
    success++;
  }

  /**
   * Synchronized method to increment number of errors.
   */
  public final synchronized void error() {
    errors++;
  }

  /**
   * Returns a Hashtable containing results for this class.
   * @return dict A Hashtable<String,Long> containing the keys
   * 'errorCount', 'time', and 'requestCount', 'errorCount'
   * holds the total number of errors, and 'requestCount'
   * contains the number of successful (non-erroneous)
   * requests.
   */
  public final Hashtable<String, Long> getResults() {
    Hashtable<String, Long> dict = new Hashtable<String, Long>();
    dict.put("errorCount", errors);
    dict.put("requestCount", success);
    dict.put("time", time);
    return dict;
  }
}
