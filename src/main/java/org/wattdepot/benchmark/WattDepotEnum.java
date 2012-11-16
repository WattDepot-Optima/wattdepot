package org.wattdepot.benchmark;

import org.wattdepot.client.WattDepotClient;

/** A collection of command Enums for the WattDepotClient object.
 * @author Greg Burgess
 *
 */
public enum WattDepotEnum {
  /**
   * Implements the isHealthy() command, basically a ping test.
   */
  HEALTH_CHECK() {
      /**
       * The command-specific implementation.
       * @param result The ResultSet object in which to store results.
       * @param client The WattDepotClient object to use.
       * @param parameters Parameters required by the specific WattDepotClient
       *  command.
       */
      @Override
      public void execute(final ResultSet result, final WattDepotClient client,
          final Object... parameters) {
        boolean check = client.isHealthy();
        if (check) {
          result.succes();
        }
        else {
          result.error();
        }
      }
  },

  /**
   * Implements the isHealthy() command, basically a ping test.
   */
  SOLDIER() {
    /**
     * The command-specific implementation.
     * @param result The ResultSet object in which to store results.
     * @param client The WattDepotClient object to use.
     * @param parameters Parameters required by the specific WattDepotClient
     *  command.
     */
      @Override
      public void execute(final ResultSet result, final WattDepotClient client,
          final Object... parameters) {
        // TODO Auto-generated method stub
      }
  };

  /**
   * Abstract method to be overridden for each command.
   * @param result The ResultSet object in which to store results.
   * @param client The WattDepotClient object to use.
   * @param parameters Parameters required by the specific WattDepotClient
   *  command.
   */
  public abstract void execute(ResultSet result, WattDepotClient client,
      Object... parameters);
}
