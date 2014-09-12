package org.manifold;

import static org.junit.Assert.*;

import org.junit.Test;

public class TestArgsPreprocessor {

  @Test
  public void testNoArguments() {
    String args[] = {};
    ArgsPreprocessor proc = new ArgsPreprocessor(args);
    assertTrue(proc.getBackendOptions().isEmpty());
    assertTrue(proc.getEngineOptions().isEmpty());
    assertTrue(proc.getFileArguments().isEmpty());
    assertTrue(proc.getFrontendOptions().isEmpty());
    assertTrue(proc.getOptionArguments().isEmpty());
  }

  @Test
  public void testEscapedFiles() {
    String args[] = {
      "-an-argument", "--another-argument",
      "--",
      "a_file", "-anotherfile.txt"
    };
    ArgsPreprocessor proc = new ArgsPreprocessor(args);
    
    assertTrue(proc.getOptionArguments().contains(args[0]));
    assertTrue(proc.getOptionArguments().contains(args[1]));
    assertFalse(proc.getOptionArguments().contains(args[2]));
    assertFalse(proc.getOptionArguments().contains(args[3]));
    assertFalse(proc.getOptionArguments().contains(args[4]));
    
    assertFalse(proc.getFileArguments().contains(args[0]));
    assertFalse(proc.getFileArguments().contains(args[1]));
    assertFalse(proc.getFileArguments().contains(args[2]));
    assertTrue(proc.getFileArguments().contains(args[3]));
    assertTrue(proc.getFileArguments().contains(args[4]));
  }
  
  @Test(expected = java.lang.IllegalArgumentException.class)
  public void testMalformed_ManyDashes_ThrowsException()
      throws IllegalArgumentException {
    String args[] = {
      "--arg-one", "----", "arg_two"
    };
    ArgsPreprocessor proc = new ArgsPreprocessor(args);
  }
  
  @Test
  public void testMixedOptionsAndFiles() {
    String args[] = {
      "-an-argument", "a_file", "-another-argument"
    };
    ArgsPreprocessor proc = new ArgsPreprocessor(args);
    
    assertTrue(proc.getOptionArguments().contains(args[0]));
    assertFalse(proc.getOptionArguments().contains(args[1]));
    assertTrue(proc.getOptionArguments().contains(args[2]));
    
    assertFalse(proc.getFileArguments().contains(args[0]));
    assertTrue(proc.getFileArguments().contains(args[1]));
    assertFalse(proc.getFileArguments().contains(args[2]));
  }
  
  @Test
  public void testOptionSeparation() {
    String args[] = {
      "--engine-one", "--frontend-two", "--backend-three"
    };
    ArgsPreprocessor proc = new ArgsPreprocessor(args);
    
    // all three arguments are options...
    assertTrue(proc.getOptionArguments().contains(args[0]));
    assertTrue(proc.getOptionArguments().contains(args[1]));
    assertTrue(proc.getOptionArguments().contains(args[2]));
    
    assertFalse(proc.getFrontendOptions().contains(args[0]));
    assertFalse(proc.getBackendOptions().contains(args[0]));
    assertTrue(proc.getEngineOptions().contains(args[0]));
    
    assertTrue(proc.getFrontendOptions().contains(args[1]));
    assertFalse(proc.getBackendOptions().contains(args[1]));
    assertFalse(proc.getEngineOptions().contains(args[1]));
    
    assertFalse(proc.getFrontendOptions().contains(args[2]));
    assertTrue(proc.getBackendOptions().contains(args[2]));
    assertFalse(proc.getEngineOptions().contains(args[2]));
  }
  
}
