package org.manifold;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class TestInitialOptions {

  @Test
  public void testDefaultOptions() {
    String noArgs[] = {};
    InitialOptions opts = new InitialOptions(noArgs);
    // both of the following options should be unset
    assertFalse(opts.compileOnly());
    assertFalse(opts.useIntermediate());
  }

  @Test
  public void testOptionFrontend() {
    String frontendName = "front";
    String args[] = {"-f", frontendName};
    InitialOptions opts = new InitialOptions(args);
    assertEquals(frontendName, opts.getFrontendName());
  }

}
