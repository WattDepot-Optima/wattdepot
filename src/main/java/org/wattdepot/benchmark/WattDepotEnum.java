package org.wattdepot.benchmark;

import java.util.Hashtable;
import java.util.List;
import java.util.Random;
import javax.xml.datatype.XMLGregorianCalendar;
import org.wattdepot.client.OverwriteAttemptedException;
import org.wattdepot.client.WattDepotClient;
import org.wattdepot.resource.property.jaxb.Property;
import org.wattdepot.resource.sensordata.jaxb.SensorData;
import org.wattdepot.resource.sensordata.jaxb.SensorDataIndex;
import org.wattdepot.resource.sensordata.jaxb.SensorDataRef;
import org.wattdepot.resource.source.jaxb.Source;
import org.wattdepot.resource.user.jaxb.User;
import org.wattdepot.util.tstamp.Tstamp;

/** A collection of command Enums for the WattDepotClient object.  Each
 *  command corresponds to a command in the WattDepot Rest API and runs
 *  a benchmark for that command.  All tests use a WattDepotClient to
 *  make HTTP requests to the WDserver.  Uses code taken from Yongwen Xu's
 *  WattExample.java.
 * @author Greg Burgess
 * @author Yongwen Xu
 */

public enum WattDepotEnum {
  /**
   * Implements the isHealthy() command, basically a ping test.
   */
  HEALTH_CHECK() {
      /**
       * Benchmarks a ping test for the WattDepot Server.
       * @param result The ResultSet object in which to store results.
       * @param client The WattDepotClient object to use.
       * @param parameters Parameters required by the specific WattDepotClient
       * @param id Used for accessing arrays.
       * @param numExecutions The number of times the thread has executed.
       *  command.
       */
      @Override
      public void execute(final ResultSet result, final WattDepotClient client,
          final int id, final int numExecutions, final Object... parameters) {
        boolean check = client.isHealthy();
        if (check) {
          result.success();
        }
        else {
          result.error();
        }
      }

      /**
       * Does nothing, Health_check is simple.
       * @param client The WattDepotClient to use.
       * @param numThreads The number of threads in use by the test.
       * @param params Misc. configuration parameters.
       * @return Nothing.
       */
      @Override
      public Object[] setup(final WattDepotClient client, final int numThreads,
          final Hashtable<String, String> params) {
        return null;
      }
  },

  /**
   * Implements the GET_POWER() command.
   */
  GET_POWER() {
      /**The command-specific implementation.
       * @param result The ResultSet object in which to store results.
       * @param client The WattDepotClient object to use.
       * @param parameters Parameters required by the
       *  specific WattDepotClient
       *  command.
       *  @param numExecutions The number of times the Thread
       *  has executed.
       *  @param id Used for accessing arrays.
       */
      @Override
      public void execute(final ResultSet result,
            final WattDepotClient client,
            final int numExecutions, final int id,
            final Object... parameters) {
         String[][] timestamps = null;
           try {
             timestamps = (String[][]) parameters;
           }
           catch (Exception e) {
             System.out.println("Error parsing parameter list.");
             e.printStackTrace();
           }
           try {
             String[] timestamp = timestamps[id];
             int size = timestamp.length;
             Random rand = new Random();
             int lowerBound = rand.nextInt(size);
             client.getSensorData(WattDepotBenchmark.getSourceName() + id,
                 Tstamp.makeTimestamp(timestamp[lowerBound]));
             client.getPower(WattDepotBenchmark.getSourceName() + id,
                 Tstamp.makeTimestamp(timestamp[lowerBound]));

             result.success();
           }
           catch (Exception e) {
             result.error();
           }
        }

        /**
         * Does things!
         * @param client The WattDepotClient object to use.
         * @param numThreads The number of threads to be used by the
         * benchmark; indicates the number of objects to setup.
         * @param params Misc configuration parameters.
         * @return Something!
         */
      @Override
      public Object[] setup(final WattDepotClient client,
              final int numThreads,
              final Hashtable<String, String> params) {
          WattDepotEnum.GET_SENSOR_DATA_FROM_SOURCE.setup(
              client, numThreads, params);
            return null;
          }
    },

  /**
   * Implements the getEnergy() command.
   */
  GET_ENERGY() {
      /**
       * The command-specific implementation.
       * @param result The ResultSet object in which to store results.
       * @param client The WattDepotClient object to use.
       * @param parameters Parameters required by the specific
       *  WattDepotClient command.
       *  @param numExecutions The number of times the Thread
       *  has executed.
       *  @param id Used for accessing arrays.
       */
      @Override
      public void execute(final ResultSet result,
            final WattDepotClient client,
            final int numExecutions,
            final int id, final Object... parameters) {
         String[][] timestamps = null;
           try {
             timestamps = (String[][]) parameters;
           }
           catch (Exception e) {
             System.out.println("Error parsing parameter list.");
             e.printStackTrace();
           }
           try {
             String[] timestamp = timestamps[id];
             int size = timestamp.length;
             int userInterval = 10000;
             Random rand = new Random();
             int lowerBound = rand.nextInt(size);
             int upperBound = rand.nextInt(size - lowerBound)
             + lowerBound;
             client.getEnergy(WattDepotBenchmark.getSourceName() + id,
                 Tstamp.makeTimestamp(timestamp[lowerBound]),
                 Tstamp.makeTimestamp(timestamp[upperBound]),
                 userInterval);
             result.success();
           }
           catch (Exception e) {
             result.error();
           }
        }

      /**
       * Does things!
       * @param client The WattDepotClient to use.
       * @param numThreads The number of Threads in use.
       * @param params Misc. configuration parameters.
       * @return Something!
       */
      @Override
      public Object[] setup(final WattDepotClient client,
              final int numThreads,
              final Hashtable<String, String> params) {
          WattDepotEnum.GET_SENSOR_DATA_FROM_SOURCE.setup(
              client, numThreads, params);
            return null;
          }
    },

    /**
     * Implements the PUT_USER() command.
     */
    PUT_USER() {
        /**
         * The command-specific implementation.
         * @param result The ResultSet object in which to
         *  store results.
         * @param client The WattDepotClient object to use.
         * @param parameters Parameters required by the specific
         *  WattDepotClient
         *  command.
         *  @param numExecutions The number of times the
         *  Thread has executed.
         *  @param id Used for accessing arrays.
         */
      @Override
        public void execute(final ResultSet result,
              final WattDepotClient client,
              final int numExecutions,
              final int id, final Object... parameters) {
        }

        @Override
        /** Run a Benchmark to add the necessary number of sources.
         * One source is added for each thread used in the test.
         * @param client The WattDepotClient object to use.
         * @param numThreads The number of threads to be used by the
         * benchmark; indicates the number of objects to setup.
         */
        public Object[] setup(final WattDepotClient client,
            final int numThreads,
            final Hashtable<String, String> params) {
          String userName = "foo@example.com";
          User user = new User(userName, "foobar", true, null);

          try {
            client.storeUser(user);
          }
          catch (OverwriteAttemptedException e1) {
            System.out.println("User already stored, continuing.");
          }
          catch (Exception e1) {
            System.out.println("Error in setup");
            e1.printStackTrace();
            System.exit(1);
          }
          return null;
        }

      },

      /**
       * Implements the GET_USER() command.
       */
      GET_USER() {
          /**
           * The command-specific implementation.
           * @param result The ResultSet object in which
           *  to store results.
           * @param client The WattDepotClient object to use.
           * @param parameters Parameters required by the
           *  specific WattDepotClient
           *  command.
           *  @param numExecutions The number of times the
           *  Thread has executed.
           *  @param id Used for accessing arrays.
           */
          @Override
          public void execute(final ResultSet result,
              final WattDepotClient client,
                final int numExecutions,
                final int id, final Object... parameters) {
            //String userName = (string)parameters;
            //client.getUser(userName);
            }

            /**
             * Does things!
             * @param client The WattDepotClient to use.
             * @param numThreads The number of Threads in use.
             * @param params Misc. configuration parameters.
             * @return Something!
             */
          @Override
          public Object[] setup(final WattDepotClient client,
              final int numThreads,
              final Hashtable<String, String> params) {
              WattDepotEnum.PUT_USER.setup(
                  client, numThreads, params);
                  return null;
                }
        },

        /** Implements the getCarbon(Source, timestamp,
         *  timestamp)
       * command.
       */
      GET_CARBON() {
          /**
           * The command-specific implementation.
           * @param result The ResultSet object in which to
           * store results.
           * @param client The WattDepotClient object to use.
           * @param parameters Parameters required
           * by the specific WattDepotClient
           *  command.
           *  @param id Used for accessing arrays.
           *  @param numExecutions The number of times the
           *  Thread has executed.
           */
          @Override
          public void execute(final ResultSet result,
                final WattDepotClient client,
                final int numExecutions, final int id,
                final Object... parameters) {
             String[][] timestamps = null;
               try {
                 timestamps = (String[][]) parameters;
               }
               catch (Exception e) {
                 System.out.println("Error parsing parameter"
                     + " list.");
                 e.printStackTrace();
               }
               try {
                 String[] timestamp = timestamps[id];
                 int size = timestamp.length;
                 int userInterval = 10000;
                 Random rand = new Random();
                 int lowerBound = rand.nextInt(size);
                 int upperBound = rand.nextInt(size
                     - lowerBound)
                 + lowerBound;
                 client.getCarbon(
                     WattDepotBenchmark.getSourceName() + id,
                     Tstamp.makeTimestamp(
                         timestamp[lowerBound]),
                     Tstamp.makeTimestamp(
                         timestamp[upperBound]),
                     userInterval);
                 result.success();
               }
               catch (Exception e) {
                 result.error();
               }
            }

          /**
           * Does things!
           * @param client The WattDepotClient to use.
           * @param numThreads The number of Threads in use.
           * @param params Misc. configuration parameters.
           * @return Something!
           */
          @Override
          public Object[] setup(final WattDepotClient client,
              final int numThreads,
                  final Hashtable<String, String> params) {
              WattDepotEnum.GET_SENSOR_DATA_FROM_SOURCE.setup(
                  client, numThreads,
                  params);
                return null;
              }
        },
  /** Implements the store(SensorData) command on a WattDepotClient.
   */
  ADD_SENSOR_DATA() {
    /**
     * Stores data using a WattDepotClient.  Based off of WattExample by
     * Yongwen Xu.
     * @param result The ResultSet object in which to store results.
     * @param client The WattDepotClient object to use.
     * @param parameters Parameters required by the specific WattDepotClient
     * @param id Used for accessing arrays.
     * @param numExecutions The number of times the thread has executed.
     *  command.
     */
    @Override
    public void execute(final ResultSet result, final WattDepotClient client,
          final int numExecutions, final int id, final Object... parameters) {

      String sourceName = WattDepotBenchmark.getSourceName();
      String sourceUri = client.getWattDepotUri() + "sources/"
        + sourceName + id;

      try {
                  XMLGregorianCalendar timestamp = Tstamp.makeTimestamp(
                      System.currentTimeMillis());
                  Property powerConsumed = new Property(
                      SensorData.POWER_CONSUMED, 200);
                  SensorData data = new SensorData(timestamp,
                      "ExampleSensorTool", sourceUri, powerConsumed);
                  client.storeSensorData(data);
                  result.success();
                  Thread.sleep(3);
      }

      catch (OverwriteAttemptedException e) {
        System.err.println("Overwrite attempt detected."
            + " (Client is storing data too quickly).");
        result.error();
      }
      catch (Exception e) {
        System.err.println("Unable to store sensordata for source "
            + sourceName + id + ". Exception is " + e);
        result.error();
      }
    }

    @Override
    /** Run a Benchmark to add the necessary number of sources.
     * One source is added for each thread used in the test.
     * @param client The WattDepotClient object to use.
     * @param numThreads The number of threads to be used by the
     * benchmark; indicates the number of objects to setup.
     */
    public Object[] setup(final WattDepotClient client, final int numThreads,
        final Hashtable<String, String> params) {

      String sourceName = WattDepotBenchmark.getSourceName();
      String userName = "foo@example.com";
      User user = new User(userName, "foobar", true, null);

      try {
        client.storeUser(user);
      }
      catch (OverwriteAttemptedException e1) {
        System.out.println("User already stored, continuing.");
      }
      catch (Exception e1) {
        System.out.println("Error in setup");
        e1.printStackTrace();
        System.exit(1);
      }

      //create a Source for each sensor to store to.
      for (int i = 0; i < numThreads; i++) {
        try {
          client.storeSource(new Source(
              sourceName + i,
             client.getWattDepotUri() + "/" + userName,
             true,                 // public
             false,                // not virtual
             "Example Coordinates",
             "Example Location",
             "Example Description",
             null,                 // no additional props
             null),               // no sub source
          true);
        }
        catch (Exception e) {
           System.out.println("Unable to store source " + sourceName
               + i + ". Exception is " + e);
           e.printStackTrace();
        }
      }
      return null;
    }

  },
  /**
   * Implements the getSensorDataIndex(Source) method.
   */
  GET_SENSOR_DATA_FROM_SOURCE() {
    /**
     * Benchmarks the retrieval of an index of all sensor data for
     * a single source.
     * @param result The ResultSet object in which to store results.
     * @param client The WattDepotClient object to use.
     * @param parameters Parameters required by the specific WattDepotClient
     *  command.
     * @param id Used for accessing arrays.
     * @param numExecutions The number of times the thread has executed.
     */
      @Override
      public void execute(final ResultSet result, final WattDepotClient client,
          final int id, final int numExecutions, final Object... parameters) {
        try {
          client.getSensorDataIndex(WattDepotBenchmark.getSourceName() + id);
          result.success();
        }
        catch (Exception e) {
          result.error();
          e.printStackTrace();
        }
      }

      /**
       * Stores some data to the DB so that we have
       * something to fetch.  We do this by running
       * another benchmark.
       * @param client The WattDepotClient to use.
       * @param params Misc. configuration parameters.
       * @param numThreads The number of Threads used by this
       * test.
       * @return null!
       */
    @Override
    public Object[] setup(final WattDepotClient client, final int numThreads,
        final Hashtable<String, String> params) {
      WattDepotBenchmark bench = null;
      try {
        bench = new WattDepotBenchmark("ADD_SENSOR_DATA", params);
      }
      catch (Exception e) {
        System.out.println("Error occured setting up test.");
        e.printStackTrace();
        System.exit(1);
      }
      bench.run();
      return null;
    }
  },
  /**
   * Implements the getLateestSensorData(Source) command.
   */
  GET_LATEST_SENSOR_DATA() {
    /**
     * Gets the latest sensor data from a single source.
     * @param result The ResultSet object in which to store results.
     * @param client The WattDepotClient object to use.
     * @param parameters Parameters required by the specific WattDepotClient
     *  command.
     *  @param id Used for accessing arrays.
     *  @param numExecutions The number of times the thread has executed.
     */
      @Override
      public void execute(final ResultSet result, final WattDepotClient client,
          final int id, final int numExecutions, final Object... parameters) {
        try {
          client.getLatestSensorData(WattDepotBenchmark.getSourceName() + id);
          result.success();
        }
        catch (Exception e) {
          result.error();
          e.printStackTrace();
        }
      }


      /**
       * Stores some data to the DB so that we have
       * something to fetch.  We do this by running
       * another benchmark.
       * @param numThreads The number of Threads used by this
       * test.
       * @param client The WattDepotClient to use.
       * @param params Misc. configuration parameters.
       * @return Something!
       */
    @Override
    public Object[] setup(final WattDepotClient client, final int numThreads,
        final Hashtable<String, String> params) {
      WattDepotEnum.GET_SENSOR_DATA_FROM_SOURCE.setup(client, numThreads,
          params);
      return null;
    }
  },

  /**
   * Implements the getSensorData(Source, timestamp) command, to get
   * sensor data at a specific time.
   */
  GET_SENSOR_DATA_TIMESTAMP() {
    /**
     * The command-specific implementation.
     * @param result The ResultSet object in which to store results.
     * @param client The WattDepotClient object to use.
     * @param parameters Parameters required by the specific WattDepotClient
     *  command.
     *  @param id Used for accessing arrays.
     *  @param numExecutions The number of times the thread has executed.
     */
      @Override
      public void execute(final ResultSet result, final WattDepotClient client,
          final int id, final int numExecutions, final Object... parameters) {
        String[][] timestamps = null;
        try {
          timestamps = (String[][]) parameters;
        }
        catch (Exception e) {
          System.out.println("Error parsing parameter list.");
          e.printStackTrace();
        }
        try {
          String[] timestamp = timestamps[id];
          int size = timestamp.length;
          client.getSensorData(WattDepotBenchmark.getSourceName() + id,
              Tstamp.makeTimestamp(timestamp[numExecutions % size]));
          result.success();
        }
        catch (Exception e) {
          result.error();
        }

      }


      /**
       * Does things!
       * @param client The WattDepotClient to use.
       * @param numThreads The number of Threads used by this
       * test.
       * @param params Misc. configuration parameters.
       * @return Something!
       */
    @Override
    public Object[] setup(final WattDepotClient client, final int numThreads,
        final Hashtable<String, String> params) {
      //Runs an ADD_SENSOR_DATA benchmark to populate the DB
      WattDepotEnum.GET_SENSOR_DATA_FROM_SOURCE.setup(client, numThreads,
          params);

      String[][] timestamps = new String [numThreads][];
      String sourceName = WattDepotBenchmark.getSourceName();
      List<SensorDataRef> sensorDatas = null;
      //Get the available timestamps associated with each source.
      for (int threadID = 0; threadID < numThreads; threadID++) {
        String instanceSourceName = sourceName + threadID;
        SensorDataIndex dataIndex = null;
        try {
          dataIndex = client.getSensorDataIndex(instanceSourceName);
          //get a list of data entries
          sensorDatas = dataIndex.getSensorDataRef();
          int size = sensorDatas.size();
          String[] timestamp = new String[size];
          //for each data entry, get the timestamp as a string
          for (int i = 0; i < size; i++) {
            timestamp[i] = sensorDatas.get(i).getTimestamp().toString();
          }
          timestamps[threadID] = timestamp;
        }
        catch (Exception e) {
          System.out.println("Error geting Sensor data for "
              + instanceSourceName);
          e.printStackTrace();
        }
      }
      return timestamps;
    }
  },

  /**
   * Implements the deleteSensorData(Source, timestamp) command, to delete
   * a sensor data entry at a specific time.
   */
  DELETE_SENSOR_DATA_TIMESTAMP() {
    /**
     * The command-specific implementation.
     * @param result The ResultSet object in which to store results.
     * @param client The WattDepotClient object to use.
     * @param parameters Parameters required by the specific WattDepotClient
     *  command.
     *  @param id Used for accessing arrays.
     *  @param numExecutions The number of times the thread has executed.
     *  command.
     */
      @Override
      public void execute(final ResultSet result, final WattDepotClient client,
          final int id, final int numExecutions, final Object... parameters) {
        String[][] timestamps = null;
        try {
          timestamps = (String[][]) parameters;
        }
        catch (Exception e) {
          System.out.println("Error parsing parameter list.");
          e.printStackTrace();
        }
        try {
          String[] timestamp = timestamps[id];
          int size = timestamp.length;
          //Threads spin if they've deleted all timestamps for their source.
          if (numExecutions <= size && size > 0) {
            client.getSensorData(WattDepotBenchmark.getSourceName() + id,
                Tstamp.makeTimestamp(timestamp[numExecutions % size]));
            result.success();
          }
        }
        catch (Exception e) {
          e.printStackTrace();
          result.error();
        }

      }


      /**
       * Populates the DB with values to delete, and gets available timestamps.
       * @param client The WattDepot client to use.
       * @param numThreads The number of Threads used by this
       * test.
       * @param params Misc. configuration parameters.
       * @return Something!
       */
    @Override
    public Object[] setup(final WattDepotClient client, final int numThreads,
        final Hashtable<String, String> params) {

      //Populates the DB and gets a list of available timestamps.
      Object[] timestamps = WattDepotEnum.GET_SENSOR_DATA_TIMESTAMP.
          setup(client, numThreads, params);
      return timestamps;
    }
  },
  /**
   * Implements the getSensorDataIndex(Source, timestamp,timestamp)
   * command, to get all sensor data between two specified times.
   */
  GET_SENSOR_DATA_TIMESTAMP_RANGE() {
    /**
     * The command-specific implementation.
     * @param result The ResultSet object in which to store results.
     * @param client The WattDepotClient object to use.
     * @param parameters Parameters required by the specific WattDepotClient
     *  command.
     * @param numExecutions The number of times the thread has executed.
     * @param id Used for accessing arrays.
     */
      @Override
      public void execute(final ResultSet result, final WattDepotClient client,
          final int id, final int numExecutions, final Object... parameters) {
        String[][] timestamps = null;
        try {
          timestamps = (String[][]) parameters;
        }
        catch (Exception e) {
          System.out.println("Error parsing parameter list.");
          e.printStackTrace();
        }
        try {
          String[] timestamp = timestamps[id];
          int size = timestamp.length;
          Random rand = new Random();
          int lowerBound = rand.nextInt(size);
          int upperBound = rand.nextInt(size - lowerBound) + lowerBound;
          client.getSensorDataIndex(WattDepotBenchmark.getSourceName() + id,
              Tstamp.makeTimestamp(timestamp[lowerBound]),
              Tstamp.makeTimestamp(timestamp[upperBound]));
          result.success();
        }
        catch (Exception e) {
          result.error();
        }

      }


      /**
       * Does things!
       * @param client The WattDepotClient to use.
       * @param numThreads The number of Threads used by this
       * test.
       * @param params Misc. configuration parameters.
       * @return Something!
       */
    @Override
    public Object[] setup(final WattDepotClient client, final int numThreads,
        final Hashtable<String, String> params) {
      //Populate the DB
      WattDepotEnum.GET_SENSOR_DATA_FROM_SOURCE.setup(client, numThreads,
          params);
      //Get available timestamps
      Object[] timestamps = WattDepotEnum.GET_SENSOR_DATA_TIMESTAMP.
          setup(client, numThreads, params);
      return timestamps;
    }
  },

  /**
   * Implements the deleteSource(source) command, which removes
   * all data for a single named source.
   */
  DELETE_ALL_SENSOR_DATA() {
    /**
     * The command-specific implementation.
     * @param result The ResultSet object in which to store results.
     * @param client The WattDepotClient object to use.
     * @param parameters Parameters required by the specific WattDepotClient
     *  command.
     * @param id Used for accessing arrays.
     * @param numExecutions The number of times the thread has executed.
     */
      @Override
      public void execute(final ResultSet result, final WattDepotClient client,
          final int id, final int numExecutions, final Object... parameters) {
        Object[] sources = (Object[]) parameters[0];
        int numThreads = (Integer) parameters[1];
        int size = sources.length;
        int target = id + numThreads * numExecutions;
        //If we have a legal target, proceed
        if (target < size) {
          try {
            client.deleteAllSensorData(WattDepotBenchmark.getSourceName()
                + target);
            result.success();
          }
          catch (Exception e) {
            System.out.println("Error in execution thread " + id + " "
                + target);
            result.error();
            e.printStackTrace();
          }
        }
        //Otherwise, sleep and let other threads run.
        else {
          try {
            Thread.sleep(2000);
          }
          catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      }


      /**
       * Delete all existing sources, then populate the server with
       * our benchmark sources.
       * @param client The WattDepotClient to use.
       * @param numThreads The number of Threads used by this
       * test.
       * @param params Misc. configuration parameters.
       * @return Something!
       */
    @Override
    public Object[] setup(final WattDepotClient client, final int numThreads,
        final Hashtable<String, String> params) {
      Integer sourceMultiplier = 5;
      if (params.contains("sourceMultiplier")) {
        sourceMultiplier = new Integer(params.get("sourceMulitiplier"));
      }
      try {
        //Delete all existing sources
        List<Source> sources = client.getSources();
        for (Source s : sources) {
          client.deleteSource(s.getName());
        }
        //Rebuild our test sources.
        WattDepotEnum.ADD_SENSOR_DATA.setup(client, sourceMultiplier
            * numThreads, params);
        //Return the new list of sources.
        return new Object[] {client.getSources().toArray(), numThreads};
      }
      catch (Exception e) {
        System.out.println("Error setting up test");
        e.printStackTrace();
        return null;
      }
    }
  },

  /**
   * Example code.
   */
  SOLDIER() {
    /**
     * The command-specific implementation.
     * @param result The ResultSet object in which to store results.
     * @param client The WattDepotClient object to use.
     * @param parameters Parameters required by the specific WattDepotClient
     *  command.
     * @param id Used for accessing arrays.
     * @param numExecutions The number of times the thread has executed.
     */
      @Override
      public void execute(final ResultSet result, final WattDepotClient client,
          final int id, final int numExecutions, final Object... parameters) {
        // TODO Auto-generated method stub
      }


      /**
       * Does things!
       * @param client The WattDepotClient to use.
       * @param numThreads The number of Threads used by this
       * test.
       * @param params Misc. configuration parameters.
       * @return Something!
       */
    @Override
    public Object[] setup(final WattDepotClient client, final int numThreads,
        final Hashtable<String, String> params) {
      return null;
    }
  };

  /**
   * Abstract method to be overridden for each command.
   * @param result The ResultSet object in which to store results.
   * @param client The WattDepotClient object to use.
   * @param parameters Parameters required by the specific WattDepotClient
   *  command.
   * @param id Used for accessing arrays.
   * @param numExecutions The number of times the Thread has executed.
   */
  public abstract void execute(ResultSet result, WattDepotClient client,
      final int id, final int numExecutions, Object... parameters);

  /**
   * Performs any required setup to run a test, returning
   * any required objects.  Used by the WattDepotBenchmark class.
   * @param client The WattDepotClient to use.
   * @param numThreads The number of Threads used by this
   * test.
   * @param params Misc. configuration parameters.
   * @return An array of objects that an execute() method might need.
   */
  public abstract Object[] setup(final WattDepotClient client,
      final int numThreads, final Hashtable<String, String> params);
}
