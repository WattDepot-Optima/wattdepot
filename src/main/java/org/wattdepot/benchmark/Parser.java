package org.wattdepot.benchmark;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.batik.svggen.SVGGraphics2DIOException;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Hashtable;
 
public class Parser {
 
  public static void main(String argv[]) {
 
    try {
      File fXmlFile = new File("c:\\file.xml");
      DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      Document doc = dBuilder.parse(fXmlFile);
      doc.getDocumentElement().normalize();
   
      NodeList listOfTests = doc.getElementsByTagName("test"); //create list of all tests
   
      for (int i = 0; i < listOfTests.getLength(); i++) {
        Node test = listOfTests.item(i); //look at an individual test
        NodeList tags = test.getChildNodes(); //list of all params for that test
        Hashtable<String, String> params = new Hashtable<String, String>();
        for (int k = 0;k < tags.getLength(); k++) {
          if (tags.item(k).getNodeType() == Node.ELEMENT_NODE) {
            String key = tags.item(k).getNodeName(); //name of the param
            String value = tags.item(k).getFirstChild().getNodeValue(); //value of param
            params.put(key, value);
          }  
        }
        String dir = System.getProperty("user.home") 
        + "\\benchmark\\" + params.get("name") + "\\";
        params.put("dir", dir);
        runTest(params);
      }
    } catch (Exception e) {
    e.printStackTrace();
    }
  }
  
  private static void runTest(Hashtable<String, String> params) {

    try { //check to make sure name entered is valid
      WattDepotEnum command = WattDepotEnum.valueOf(params.get("name"));
      }
    catch (IllegalArgumentException e) {
      System.err.println("Invalid Command \"" + params.get("name") + "\".");
      System.exit(1);
    }
    //set up for graphing
    final XYSeries successSeries = new XYSeries("Successes");
    final XYSeries errorSeries = new XYSeries("\nErrors");
    final XYSeriesCollection dataset1 = new XYSeriesCollection();
    try {
      WattDepotBenchmark run;
      //check how many tests to run
      if (params.containsKey("min") || 
          params.containsKey("max") || 
          params.containsKey("interval")) {
        if (params.contains("range")) {
          //there's both range and min/max
          System.out.println("Parameters must include a user defined range of threads " +
          		"or a min, max, and interval. (enter a single number as the range for one" +
          		" test only)");
          System.exit(1);
        }
        else if (params.containsKey("min") && 
            params.containsKey("max") && 
            params.containsKey("interval")) {
          //we're good to go
          int min = Integer.valueOf(params.get("min"));
          int max = Integer.valueOf(params.get("max"));
          int interval = Integer.valueOf(params.get("interval"));
          for (int i = min; i <= max; i+= interval) {
            params.put("numThreads", String.valueOf(i));
            run = new WattDepotBenchmark(params.get("name"), params);
            run.run();
            //store results for graphing
            long requests = run.getFinalResults().getResults().get("requestCount");
            long errors = run.getFinalResults().getResults().get("errorCount");
            successSeries.add(i, requests);
            errorSeries.add(i, errors);
          }
          //graph it
          dataset1.addSeries(successSeries);
          dataset1.addSeries(errorSeries);
          JFreeChart chart1 = createChart("HTTP Requests vs Threads",
              "#Threads", "#Requests", dataset1);
          String name = params.get("name") + "_" + min + " - " + max;
          createSVG(chart1, name, params.get("dir"));
        }
        else {
          //if there's one, you need all three
          System.out.println("min, max, and interval must all be defined");
          System.exit(1);
        }
      }
      else if (params.containsKey("range")) {
        //no min/max but range is defined
        if(params.get("range").contains(",")) { //if range is a csv
          String[] numbers = params.get("range").split(",");
          for (int i = 0; i < numbers.length; i++) {
            params.put("numThreads", numbers[i]);
            run = new WattDepotBenchmark(params.get("name"), params);
            run.run();
            //store results for graphing
            long requests = run.getFinalResults().getResults().get("requestCount");
            long errors = run.getFinalResults().getResults().get("errorCount");
            int threads = Integer.valueOf(numbers[i]);
            successSeries.add(threads, requests);
            errorSeries.add(threads, errors);
          }
          //graph it
          dataset1.addSeries(successSeries);
          dataset1.addSeries(errorSeries);
          JFreeChart chart1 = createChart("HTTP Requests vs Threads",
              "#Threads", "#Requests", dataset1);
          String name = params.get("name") + "_" + "USER_DEFINED_AGGREGATE";
          createSVG(chart1, name, params.get("dir"));
        }
        else {//if range is a single number
        params.put("numThreads", String.valueOf(params.get("range")));
        run = new WattDepotBenchmark(params.get("name"), params);
        run.run();
        }
      }
      else {
        //neither range nor min/max is defined
        run = new WattDepotBenchmark(params.get("name"), params);
        run.run(); 
      }
    }
    catch (Exception e) {
      System.out.println("Error running: " + params.get("name"));
      e.printStackTrace();
      System.exit(1);
    }
  }
  /** Creates a Chart object.
   * @param title Title of the chart.
   * @param xAxisLabel Label for x axis.
   * @param yAxisLabel Label for y axis.
   * @param dataset Dataset to add to graph.
   * @return A Chart object.
   */
  public static JFreeChart createChart(final String title,
      final String xAxisLabel, final String yAxisLabel,
      final XYSeriesCollection dataset) {
    final JFreeChart chart = ChartFactory.createXYLineChart(
        title,      // chart title
        xAxisLabel,                      // x axis label
        yAxisLabel,                      // y axis label
        dataset,                  // data
        PlotOrientation.VERTICAL,
        true,                     // include legend
        true,                     // tooltips
        false                     // urls
    );
    return chart;
  }

  /**
   * Creates and prints an SVG representation of the chart.
   * @param chart Chart to print.
   * @param name File name of the chart.
   * @param dir String directory to put chart in
   */
  public static void createSVG(final JFreeChart chart, final String name, final String dir) {
    // THE FOLLOWING CODE BASED ON THE EXAMPLE IN THE BATIK DOCUMENTATION...
    // Get a DOMImplementation
    DOMImplementation domImpl
        = GenericDOMImplementation.getDOMImplementation();

    // Create an instance of org.w3c.dom.Document
    Document document = domImpl.createDocument(null, "svg", null);

    // Create an instance of the SVG Generator
    SVGGraphics2D svgGenerator = new SVGGraphics2D(document);

    // set the precision to avoid a null pointer exception in Batik 1.5
    svgGenerator.getGeneratorContext().setPrecision(6);

    // Ask the chart to render into the SVG Graphics2D implementation
    chart.draw(svgGenerator, new Rectangle2D.Double(0, 0, 400, 300), null);

    // Finally, stream out SVG to a file using UTF-8 character to
    // byte encoding
    boolean useCSS = true;
    Writer out;
    try {
      out = new OutputStreamWriter(
          new FileOutputStream(new File(dir + name + ".svg")), "UTF-8");
          svgGenerator.stream(out, useCSS);
    }
    catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    catch (SVGGraphics2DIOException e) {
      e.printStackTrace();
    }
  }
}