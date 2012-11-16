package org.wattdepot.benchmark;

import java.util.Hashtable;

/**
 * Keeps track of the number of successful HTTP requests and
 * the number of errors generated.
 * @author Greg Burgess
 */
public class ResultSet {
  /** The number of successful HTTP requests. **/
  private static long success = 0;
  /** The number of errors generated. **/
  private static long errors = 0;

  /**
   * Initializes ResultSet to 0.
   */
  public ResultSet() {
    success = 0;
    errors = 0;
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
   * 'errorCount' and 'requestCount', 'errorCount'
   * holds the total number of errors, and 'requestCount'
   * contains the number of successful (non-erroneous)
   * requests.
   */
  public final Hashtable<String, Long> getResults() {
    Hashtable<String, Long> dict = new Hashtable<String, Long>();
    dict.put("errorCount", errors);
    dict.put("requestCount", success);
    return dict;
  }
}
