package org.wattdepot.server;

import static org.wattdepot.server.ServerProperties.CONTEXT_ROOT_KEY;
import static org.wattdepot.server.ServerProperties.SERVER_HOME_DIR;
import static org.wattdepot.server.ServerProperties.GVIZ_CONTEXT_ROOT_KEY;
import static org.wattdepot.server.ServerProperties.GVIZ_PORT_KEY;
import static org.wattdepot.server.ServerProperties.LOGGING_LEVEL_KEY;
import static org.wattdepot.server.ServerProperties.PORT_KEY;
import static org.wattdepot.server.ServerProperties.TEST_INSTALL_KEY;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import org.restlet.Application;
import org.restlet.Component;
import org.restlet.Restlet;
import org.restlet.routing.Router;
import org.restlet.data.Protocol;
import org.wattdepot.resource.carbon.CarbonResource;
import org.wattdepot.resource.db.DatabaseResource;
import org.wattdepot.resource.energy.EnergyResource;
import org.wattdepot.resource.gviz.GVisualizationServlet;
import org.wattdepot.resource.health.HealthResource;
import org.wattdepot.resource.power.PowerResource;
import org.wattdepot.resource.property.jaxb.Property;
import org.wattdepot.resource.sensordata.SensorDataResource;
import org.wattdepot.resource.sensordata.jaxb.SensorData;
import org.wattdepot.resource.sensordata.jaxb.SensorDatas;
import org.wattdepot.resource.source.SourceResource;
import org.wattdepot.resource.source.jaxb.Source;
import org.wattdepot.resource.source.jaxb.SubSources;
import org.wattdepot.resource.source.summary.SourceSummaryResource;
import org.wattdepot.resource.user.UserResource;
import org.wattdepot.resource.user.jaxb.User;
import org.wattdepot.server.db.DbManager;
import org.wattdepot.util.logger.RestletLoggerUtil;
import org.wattdepot.util.logger.WattDepotLogger;

/**
 * Sets up HTTP server and routes requests to appropriate resources. Portions of this code are
 * adapted from http://hackystat-sensorbase-uh.googlecode.com/
 * 
 * @author Robert Brewer
 * @author Philip Johnson
 */
public class Server extends Application {

  /** Holds the Restlet Component associated with this Server. */
  private Component component;

  /** Holds the Jetty server associated with this Server. */
  private org.mortbay.jetty.Server jettyServer;
  
  /** Holds the hostname associated with this Server. */
  private String hostName;

  /** Holds the hostname associated with the Google Visualization API service. */
  private String gvizHostName;

  /** Holds the WattDepotLogger for the Server. */
  private Logger logger = null;

  /** Holds the ServerProperties instance associated with this Server. */
  private ServerProperties serverProperties;

  /** The URI used for the health resource. */
  public static final String HEALTH_URI = "health";

  /** The URI used for the sources resource. */
  public static final String SOURCES_URI = "sources";

  /** The URI used for the sensordata resource. */
  public static final String SENSORDATA_URI = "sensordata";

  /** The URI used for the users resource. */
  public static final String USERS_URI = "users";

  /** The URI used for the users resource. */
  public static final String GVIZ_URI = "gviz";

  /** URI fragment for source summary. */
  public static final String SUMMARY_URI = "summary";

  /** URI fragment for power. */
  public static final String POWER_URI = "power";

  /** URI fragment for energy. */
  public static final String ENERGY_URI = "energy";

  /** URI fragment for carbon emitted. */
  public static final String CARBON_URI = "carbon";

  /** URI fragment for database resource. */
  public static final String DATABASE_URI = "db";

  /** URI parameter for source name. */
  private static final String SOURCE_PARAM = "{source}";

  /** URI parameter for retrieving latest sensor data. */
  public static final String LATEST = "latest";

  /** URI parameter for deleting all sensor data. */
  public static final String ALL = "all";

  /** Users JAXBContext. */
  private static final JAXBContext userJAXB;
  /** SensorData JAXBContext. */
  private static final JAXBContext sensorDataJAXB;
  /** Source JAXBContext. */
  private static final JAXBContext sourceJAXB;
  // JAXBContexts are thread safe, so we can share them across all instances and threads.
  // https://jaxb.dev.java.net/guide/Performance_and_thread_safety.html
  static {
    try {
      userJAXB = JAXBContext.newInstance(org.wattdepot.resource.user.jaxb.ObjectFactory.class);
      sensorDataJAXB =
          JAXBContext.newInstance(org.wattdepot.resource.sensordata.jaxb.ObjectFactory.class);
      sourceJAXB = JAXBContext.newInstance(org.wattdepot.resource.source.jaxb.ObjectFactory.class);
    }
    catch (Exception e) {
      throw new RuntimeException("Couldn't create JAXB context instances.", e);
    }
  }

  /**
   * Creates a new instance of a WattDepot HTTP server, listening on the port defined either by the
   * properties file or a default. The server home directory is set to the default value of
   * "server".
   * 
   * @return The Server instance created.
   * @throws Exception If problems occur starting up this server.
   */
  public static Server newInstance() throws Exception {
    return newInstance(false, false);
  }

  /**
   * Creates a new instance of a WattDepot HTTP server, listening on the port defined either by the
   * properties file or a default. The server home directory is set to the default value of
   * "server".
   * 
   * @param compress If true, the server will compress the database and then exit.
   * @param reindex If true, the server will reindex the database and then exit.
   * @return The Server instance created.
   * @throws Exception If problems occur starting up this server.
   */
  public static Server newInstance(boolean compress, boolean reindex) throws Exception {
    return newInstance(new ServerProperties(), compress, reindex);
  }

  /**
   * Creates a new instance of a WattDepot HTTP server, listening on the port defined either by the
   * properties file or a default. The server home directory is set to the value provided by
   * serverSubdir parameter.
   * 
   * @param serverSubdir The filename of the subdirectory containing this server's files.
   * @param compress If true, the server will compress the database and then exit.
   * @param reindex If true, the server will reindex the database and then exit.
   * @return The Server instance created.
   * @throws Exception If problems occur starting up this server.
   */
  public static Server newInstance(String serverSubdir, boolean compress, boolean reindex)
      throws Exception {
    return newInstance(new ServerProperties(serverSubdir), compress, reindex);
  }

  /**
   * Creates a new instance of a WattDepot HTTP server suitable for unit testing. WattDepot
   * properties are initialized from the User's wattdepot-server.properties file, then set to their
   * "testing" versions.
   * 
   * @return The Server instance created.
   * @throws Exception If problems occur starting up this server.
   */
  public static Server newTestInstance() throws Exception {
    ServerProperties properties = new ServerProperties();
    properties.setTestProperties();
    return newInstance(properties, false, false);
  }

  /**
   * Creates a new instance of a WattDepot HTTP server, listening on the port defined either by the
   * properties file or a default.
   * 
   * @param serverProperties The ServerProperties used to initialize this server.
   * @param compress If true, the server will compress the database and then exit.
   * @param reindex If true, the server will reindex the database and then exit.
   * @return The Server instance created.
   * @throws Exception If problems occur starting up this server.
   */
  public static Server newInstance(ServerProperties serverProperties, boolean compress,
      boolean reindex) throws Exception {
    Server server = new Server();
    server.serverProperties = serverProperties;
    server.logger =
        WattDepotLogger.getLogger("org.wattdepot.server", server.serverProperties
            .get(SERVER_HOME_DIR));
    server.hostName = server.serverProperties.getFullHost();
    int port = Integer.valueOf(server.serverProperties.get(PORT_KEY));
    server.component = new Component();
    server.component.getServers().add(Protocol.HTTP, port);
    server.component.getDefaultHost().attach("/" + server.serverProperties.get(CONTEXT_ROOT_KEY),
        server);

    // Set up logging.
    RestletLoggerUtil.useFileHandler(server.serverProperties.get(SERVER_HOME_DIR));
    WattDepotLogger.setLoggingLevel(server.logger, server.serverProperties.get(LOGGING_LEVEL_KEY));
    server.logger.warning("Starting WattDepot server.");
    server.logger.warning("Host: " + server.hostName);
    server.logger.info(server.serverProperties.echoProperties());

    Map<String, Object> attributes = server.getContext().getAttributes();
    // Put server and serverProperties in first, because dbManager() will look at serverProperties
    attributes.put("WattDepotServer", server);
    attributes.put("ServerProperties", server.serverProperties);
    DbManager dbManager;
    // If we are in test mode
    if (server.serverProperties.get(TEST_INSTALL_KEY).equalsIgnoreCase("true")) {
      // Make sure database starts off wiped
      dbManager = new DbManager(server, true);
    }
    else {
      dbManager = new DbManager(server);
      try {
        server.loadDefaultResources(dbManager, server.serverProperties.get(SERVER_HOME_DIR));
      }
      catch (Exception e) {
        server.logger.severe("Unable to load default resources: " + e.toString());
      }
    }
    attributes.put("DbManager", dbManager);

    if (compress) {
      server.logger.warning("Compressing database tables.");
      dbManager.performMaintenance();
      server.logger.warning("Compressing database tables complete.");
    }
    if (reindex) {
      server.logger.warning("Reindexing database tables.");
      dbManager.indexTables();
      server.logger.warning("Reindexing database tables complete.");
    }
    if (compress || reindex) {
      // Just terminate if compression or reindexing was requested
      return null;
    }
    else {
      // Only start server up for queries if when not compressing or reindexing

      // Set up the Google Visualization API servlet
      server.gvizHostName = server.serverProperties.getGvizFullHost();
      int gvizPort = Integer.valueOf(server.serverProperties.get(GVIZ_PORT_KEY));
      server.jettyServer = new org.mortbay.jetty.Server(gvizPort);
      server.logger.warning("Google visualization URL: " + server.gvizHostName);
      Context jettyContext =
          new Context(server.jettyServer, "/" + server.serverProperties.get(GVIZ_CONTEXT_ROOT_KEY));

      ServletHolder servletHolder = new ServletHolder(new GVisualizationServlet(server));
      servletHolder.setInitParameter("applicationClassName",
          "org.wattdepot.resource.gviz.GVisualizationServlet");
      servletHolder.setInitOrder(1);
      jettyContext.addServlet(servletHolder, "/sources/*");

      // Now let's open for business.
      server.logger.info("Maximum Java heap size (MB): "
          + (Runtime.getRuntime().maxMemory() / 1000000.0));
      server.component.start();
     // server.jettyServer.start();
      server.logger.warning("WattDepot server (Version " + getVersion() + ") now running.");
      
      createDefaultData(server, dbManager);
      
      return server;
    }
  }
    
    
  //TODO: remove this..
    /**
     * Kludges up some default data so that SensorData can be stored. Originally this was to support a
     * demo (since there was no way to create sources or users), but now some tests use this data, so
     * it has been moved here.
     * 
     * @return True if the default data could be created, or false otherwise.
     */
    public static boolean createDefaultData(Server server, DbManager dbManager) {
      // Need to (re)create admin user, since the database gets wiped by each test
      ServerProperties serverProps =
          (ServerProperties) server.getContext().getAttributes().get("ServerProperties");
      String adminUsername = serverProps.get(ServerProperties.ADMIN_EMAIL_KEY);
      String adminPassword = serverProps.get(ServerProperties.ADMIN_PASSWORD_KEY);
      // create the admin User object based on the server properties
      User adminUser = new User(adminUsername, adminPassword, true, null);
      // stick admin user into database
      if (!dbManager.storeUser(adminUser)) {
        // server.getLogger().severe("Unable to create admin user from properties!");
        return false;
      }
      // create a non-admin user that owns a source for testing
      User ownerUser = new User("joebogus@example.com", "totally-bogus", false, null);
      if (!dbManager.storeUser(ownerUser)) {
        return false;
      }
      // create a non-admin user that owns nothing for testing
      User nonOwnerUser = new User("jimbogus@example.com", "super-bogus", false, null);
      if (!dbManager.storeUser(nonOwnerUser)) {
        return false;
      }

      // create public source
      Source source1 =
          new Source("saunders-hall", ownerUser.toUri(server), true, false,
              "21.30078,-157.819129,41", "Saunders Hall on the University of Hawaii at Manoa campus",
              "Obvius-brand power meter", null, null);
      source1.addProperty(new Property(Source.CARBON_INTENSITY, "1000"));
      // stick public source into database
      if (!dbManager.storeSource(source1)) {
        return false;
      }

      Source source2 =
          new Source("secret-place", ownerUser.toUri(server), false, false,
              "21.35078,-157.819129,41", "Made up private place", "Foo-brand power meter", null, null);
      source2.addProperty(new Property(Source.CARBON_INTENSITY, "3000"));
      // stick public source into database
      if (!dbManager.storeSource(source2)) {
        return false;
      }

      SubSources subSources = new SubSources();
      subSources.getHref().add(source1.toUri(server));
      subSources.getHref().add(source2.toUri(server));

      Source virtualSource =
          new Source("virtual-source", ownerUser.toUri(server), true, true,
              "31.30078,-157.819129,41", "Made up location 3", "Virtual source", null, subSources);
      return (dbManager.storeSource(virtualSource));
    }
  

  /**
   * Loads the default resources from canonical directory into database. Intended to be called after
   * the database has been created, but before the server has started accepting network connections.
   * 
   * @param dbManager The database the default resources will be loaded into.
   * @param serverHome The server home directory.
   * @throws JAXBException If there are problems unmarshalling XML from the files
   */
  protected void loadDefaultResources(DbManager dbManager, String serverHome) throws JAXBException {
    String defaultDir = serverHome + "/default-resources";
    File defaultDirFile = new File(defaultDir);
    Unmarshaller unmarshaller;

    if (defaultDirFile.isDirectory()) {
      File usersDir = new File(defaultDirFile, "users");
      if (usersDir.isDirectory()) {
        unmarshaller = userJAXB.createUnmarshaller();
        User user;
        for (File userFile : usersDir.listFiles()) {
          user = (User) unmarshaller.unmarshal(userFile);
          if (dbManager.storeUser(user)) {
            logger.info("Loaded user " + user.getEmail() + " from defaults.");
          }
          else {
            logger.warning("Default resource from file \"" + userFile.toString()
                + "\" could not be stored in DB.");
          }
        }
      }
      File sourcesDir = new File(defaultDirFile, "sources");
      if (sourcesDir.isDirectory()) {
        unmarshaller = sourceJAXB.createUnmarshaller();
        Source source;
        for (File sourceFile : sourcesDir.listFiles()) {
          source = (Source) unmarshaller.unmarshal(sourceFile);
          // Source read from the file might have an Owner field that points to a different
          // host URI. We want all defaults normalized to this server, so update it.
          source.setOwner(User.updateUri(source.getOwner(), this));
          // Source read from the file might have an Href elements under SubSources that points to
          // a different host URI. We want all defaults normalized to this server, so update it.
          if (source.isSetSubSources()) {
            List<String> hrefs = source.getSubSources().getHref();
            for (int i = 0; i < hrefs.size(); i++) {
              hrefs.set(i, Source.updateUri(hrefs.get(i), this));
            }
          }
          if (dbManager.storeSource(source)) {
            logger.info("Loaded source " + source.getName() + " from defaults.");
          }
          else {
            logger.warning("Default resource from file \"" + sourceFile.toString()
                + "\" could not be stored in DB.");
          }
        }
      }
      File sensorDataDir = new File(defaultDirFile, "sensordata");
      if (sensorDataDir.isDirectory()) {
        unmarshaller = sensorDataJAXB.createUnmarshaller();
        Object xmlObj;
        SensorData data;
        SensorDatas datas;
        int dataCount = 0;
        logger.info("Loading sensor data from defaults.");
        for (File sensorDataFile : sensorDataDir.listFiles()) {
          xmlObj = unmarshaller.unmarshal(sensorDataFile);
          if (xmlObj instanceof SensorData) {
            data = (SensorData) xmlObj;
            // SensorData read from the file might have an Owner field that points to a different
            // host URI. We want all defaults normalized to this server, so update it.
            data.setSource(Source.updateUri(data.getSource(), this));
            if (dbManager.storeSensorData(data)) {
              // Too voluminous to print every sensor data loaded
              // logger.info("Loaded sensor data for source " + data.getSource() + ", time "
              // + data.getTimestamp() + " from defaults.");
              dataCount++;
            }
            else {
              logger.warning("Default resource from file \"" + sensorDataFile.toString()
                  + "\" could not be stored in DB.");
            }
          }
          else if (xmlObj instanceof SensorDatas) {
            datas = (SensorDatas) xmlObj;
            for (SensorData theData : datas.getSensorData()) {
              // SensorData read from the file might have an Owner field that points to a different
              // host URI. We want all defaults normalized to this server, so update it.
              theData.setSource(Source.updateUri(theData.getSource(), this));
              if (dbManager.storeSensorData(theData)) {
                // Too voluminous to print every sensor data loaded
                // logger.info("Loaded sensor data for source " + data.getSource() + ", time "
                // + data.getTimestamp() + " from defaults.");
                dataCount++;
              }
              else {
                logger.warning("A default resource from file \"" + sensorDataFile.toString()
                    + "\" could not be stored in database.");
              }
            }
          }
          else {
            logger
                .warning("Found unknown XML type in sensordata file " + sensorDataFile.toString());
          }
        }
        logger.info("Loaded " + dataCount + " sensor data objects from defaults.");
      }
    }
    else {
      logger.warning("Default resource directory " + defaultDir
          + " not found, no default resources loaded.");
    }
  }

  /**
   * Starts up the WattDepot web service using the properties specified in
   * wattdepot-server.properties. Takes a single optional argument, which is the name of the
   * subdirectory under ~/.wattdepot to use as this server's "home directory", defaulting to
   * "server". Changing this is useful if you want to run multiple servers on the same system: just
   * create separate home directories under .wattdepot and start the servers with different command
   * line arguments.
   * 
   * On Unix-like systems, use Control-C to exit, which will shut down cleanly.
   * 
   * @param args Command line arguments.
   * @throws Exception if things go horribly awry during startup
   */
  public static void main(String[] args) throws Exception {
    Options options = new Options();
    options.addOption("h", "help", false, "Print this message");
    options.addOption("d", "directoryName", true,
        "subdirectory under ~/.wattdepot where this server's files are to be kept.");
    options.addOption("c", "compress", false, "Compress database files.");
    options.addOption("r", "reindex", false, "Rebuild database indices.");

    CommandLine cmd = null;
    String directoryName = null;
    boolean compress, reindex;

    CommandLineParser parser = new PosixParser();
    HelpFormatter formatter = new HelpFormatter();
    try {
      cmd = parser.parse(options, args);
    }
    catch (ParseException e) {
      System.err.println("Command line parsing failed. Reason: " + e.getMessage() + ". Exiting.");
      System.exit(1);
    }

    if (cmd.hasOption("h")) {
      formatter.printHelp("WattDepotServer", options);
      System.exit(0);
    }

    if (cmd.hasOption("d")) {
      directoryName = cmd.getOptionValue("d");
    }

    compress = cmd.hasOption("c");
    reindex = cmd.hasOption("r");

    if ((directoryName == null) || (directoryName.length() == 0)) {
      Server.newInstance(compress, reindex);
    }
    else {
      Server.newInstance(directoryName, compress, reindex);
    }
  }

  /**
   * Creates a root Restlet that will receive all incoming calls.
   * 
   * @return the newly created Restlet object
   */
  @Override
  public synchronized Restlet createInboundRoot() {
    Router router = new Router(getContext());
    router.setDefaultMatchingQuery(true);
    
    // This Router is used to control access to the User resource
    // Router userRouter = new Router(getContext());
    router.attach("/" + USERS_URI, UserResource.class);
    router.attach("/" + USERS_URI + "/{user}", UserResource.class);
    // Guard userGuard = new AdminAuthenticator(getContext());
    // userGuard.setNext(userRouter);

    // Health resource is public, so no Guard
    router.attach("/" + HEALTH_URI, HealthResource.class);

    // Source does its own authentication processing, so don't use Guard
    router.attach("/" + SOURCES_URI, SourceResource.class);
    router.attach("/" + SOURCES_URI + "/?fetchAll={fetchAll}", SourceResource.class);
    router.attach("/" + SOURCES_URI + "/" + SOURCE_PARAM, SourceResource.class);
    router.attach("/" + SOURCES_URI + "/" + SOURCE_PARAM + "?overwrite={overwrite}",
        SourceResource.class);
    router.attach("/" + SOURCES_URI + "/" + SOURCE_PARAM + "/" + SUMMARY_URI,
        SourceSummaryResource.class);

    // SensorData does its own authentication processing, so don't use Guard
    router.attach("/" + SOURCES_URI + "/" + SOURCE_PARAM + "/" + SENSORDATA_URI,
        SensorDataResource.class);
    // Specifying all the combinations of optional parameters is bogus, but don't want to deal
    // with parsing the query string right now.
    router.attach("/" + SOURCES_URI + "/" + SOURCE_PARAM + "/" + SENSORDATA_URI
        + "/?startTime={startTime}&endTime={endTime}&fetchAll={fetchAll}", SensorDataResource.class);
    router.attach("/" + SOURCES_URI + "/" + SOURCE_PARAM + "/" + SENSORDATA_URI
        + "/?startTime={startTime}&endTime={endTime}", SensorDataResource.class);
    router.attach("/" + SOURCES_URI + "/" + SOURCE_PARAM + "/" + SENSORDATA_URI + "/{timestamp}",
        SensorDataResource.class);

    // Power does its own authentication processing, so don't use Guard
    router.attach("/" + SOURCES_URI + "/" + SOURCE_PARAM + "/" + POWER_URI + "/{timestamp}",
        PowerResource.class);

    // Energy does its own authentication processing, so don't use Guard
    router.attach("/" + SOURCES_URI + "/" + SOURCE_PARAM + "/" + ENERGY_URI
        + "/?startTime={startTime}&endTime={endTime}&samplingInterval={samplingInterval}",
        EnergyResource.class);
    router.attach("/" + SOURCES_URI + "/" + SOURCE_PARAM + "/" + ENERGY_URI
        + "/?startTime={startTime}&endTime={endTime}", EnergyResource.class);

    // Carbon does its own authentication processing, so don't use Guard
    router.attach("/" + SOURCES_URI + "/" + SOURCE_PARAM + "/" + CARBON_URI
        + "/?startTime={startTime}&endTime={endTime}&samplingInterval={samplingInterval}",
        CarbonResource.class);
    router.attach("/" + SOURCES_URI + "/" + SOURCE_PARAM + "/" + CARBON_URI
        + "/?startTime={startTime}&endTime={endTime}", CarbonResource.class);

    // Database does its own authentication processing, so don't use Guard
    router.attach("/" + DATABASE_URI + "/" + "{method}", DatabaseResource.class);

    // // Google Visualization API resource
    // Route route = router.attach("/" + SOURCES_URI + "/{source}" + "/" + GVIZ_URI,
    // GVisualizationResource.class);
    // route.getTemplate().setMatchingMode(Template.MODE_EQUALS);
    // router.attach("/" + SOURCES_URI + "/{source}" + "/" + GVIZ_URI + "?{parameters}",
    // GVisualizationResource.class);
    // router.attachDefault(userGuard);

    return router;
  }

  /**
   * Returns the version associated with this Package, if available from the jar file manifest. If
   * not being run from a jar file, then returns "Development".
   * 
   * @return The version.
   */
  public static String getVersion() {
    String version = Package.getPackage("org.wattdepot.server").getImplementationVersion();
    return (version == null) ? "Development" : version;
  }

  /**
   * Returns the hostname associated with this server. Example: "http://localhost:8182/wattdepot/"
   * 
   * @return The hostname.
   */
  public String getHostName() {
    return this.hostName;
  }

  /**
   * Returns the ServerProperties instance associated with this server.
   * 
   * @return The server properties.
   */
  public ServerProperties getServerProperties() {
    return this.serverProperties;
  }

  /**
   * Shuts down the WattDepot server, in the hope that it will stop listening for connections. This
   * might not actually work, currently untested.
   * 
   * @throws Exception if something goes wrong during the shutdown.
   */
  public void shutdown() throws Exception {
    this.component.stop();
   // this.jettyServer.stop();
  }

  /**
   * Returns the logger for the WattDepot server.
   * 
   * @return The logger.
   */
  @Override
  public Logger getLogger() {
    return this.logger;
  }

}