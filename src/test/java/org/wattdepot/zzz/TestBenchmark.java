package org.wattdepot.zzz;

import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.wattdepot.benchmark.SourceServerBenchmark;
import org.wattdepot.benchmark.UserBenchmark;

/**
 * Tests the Stack Trace class.
 * @author Philip Johnson
 */
public class TestBenchmark {
  
  /**
   * Run Benchmark
   * @throws Exception 
   */
  @Test public void testBenchmark () throws Exception {
    String[] toSend = {"10", "1000"};
    UserBenchmark.main(toSend);
    SourceServerBenchmark.main(toSend);
    assertTrue("It worked!", true);
  }

}

