package org.wattdepot.test;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.*;
import java.io.*;

import javax.xml.datatype.XMLGregorianCalendar;

import org.wattdepot.util.tstamp.Tstamp;

public class UserBenchmark {
	
	public long getRequest(String url) {
		  HttpURLConnection connection = null;
	      OutputStreamWriter wr = null;
	      BufferedReader rd  = null;
	      StringBuilder sb = null;
	      String line = null;
	      URL serverAddress = null;
	      long startTime = 0;
	      long endTime = 0;
	      long elapsedTime = 0;
	     
	      
	      long timestamp;
	      //timestamp = System.currentTimeMillis();
	  //timestamp = timestamp - 120000;
	  //XMLGregorianCalendar calender = Tstamp.makeTimestamp(timestamp);
	
	   
	    
	      try {
	          serverAddress = new URL(url);
			  //set up out communications stuff
			  connection = null;
			  //Set up the initial connection
			  startTime = System.currentTimeMillis();
			  connection = (HttpURLConnection)serverAddress.openConnection();
			  URLConnection yc = serverAddress.openConnection();
			  connection.setRequestMethod("GET");
			  connection.setDoOutput(true);
			  connection.setReadTimeout(10000);
			  connection.connect();
			
			  //get the output stream writer and write the output to the server
			  //not needed in this example
			  //wr = new OutputStreamWriter(connection.getOutputStream());
			  //wr.write("");
			  //wr.flush();
			  //System.out.println("we're here 6.5");
		
			
			  //read the result from the server
			 // rd  = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			  //sb = new StringBuilder();
			
			 // while ((line = rd.readLine()) != null)
			  //{
			  //    sb.append(line + '\n');
			  //}
			  System.out.println("response code is: " + connection.getResponseCode());
			  endTime = System.currentTimeMillis();
			  elapsedTime = endTime - startTime;
			
			  //System.out.println(sb.toString());
			                
			  } catch (MalformedURLException e) {
			      e.printStackTrace();
			  } catch (ProtocolException e) {
			      e.printStackTrace();
			  } catch (IOException e) {
			      e.printStackTrace();
			  }
			  finally
			  {
			      //close the connection, set all objects to null
			          connection.disconnect();
			          rd = null;
			          sb = null;
			          wr = null;
			          connection = null;
		      }
			  return elapsedTime;
		}
	
	  public static void main(String[] args) {
		  long responseTime = 0;
		  UserBenchmark bench = new UserBenchmark();
		  responseTime = bench.getRequest("http://localhost:8182/wattdepot/sources/source01/sensordata/2012-09-27T22:00:00.000-10:00");
		  System.out.println("Response time(ms) for the last 2 minutes: " + responseTime + " ms\n");
		  
		  responseTime = bench.getRequest("http://localhost:8182/wattdepot/sources/source01/sensordata/2010-01-08T00:20:00.000-10:00");
		  System.out.println("Response time(ms) for a long time ago: " + responseTime + " ms\n");
		  
		  responseTime = bench.getRequest("http://localhost:8182/wattdepot/sources/source01/sensordata/?startTime=2012-09-27T23:56:00.000-10:00&endTime=2012-09-27T23:58:00.000-10:00");
		  System.out.println("Response time(ms) of aggregate request for 2 minutes worth of data(cached): " + responseTime + " ms\n");
		  
		  responseTime = bench.getRequest("http://localhost:8182/wattdepot/sources/source01/carbon/?startTime=2012-09-27T23:56:00.000-10:00&endTime=2012-09-27T23:58:00.000-10:00");
		  System.out.println("Response time(ms) for resource request(cached): " + responseTime + " ms\n");
		  
		  responseTime = bench.getRequest("http://localhost:8182/wattdepot/sources/source01/sensordata/?startTime=2012-09-27T23:30:00.000-10:00&endTime=2012-09-27T23:32:00.000-10:00");
		  System.out.println("Response time(ms) of aggregate request for 10 minutes worth of data(uncached): " + responseTime + " ms\n");
		  
		  responseTime = bench.getRequest("http://localhost:8182/wattdepot/sources/source01/carbon/?startTime=2012-09-27T23:30:00.000-10:00&endTime=2012-09-27T23:32:00.000-10:00");
		  System.out.println("Response time(ms) for resource request(uncached): " + responseTime + " ms\n");
		  
		  
	  }
}
