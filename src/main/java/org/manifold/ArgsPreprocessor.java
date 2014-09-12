package org.manifold;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.google.common.collect.ImmutableList;

public class ArgsPreprocessor {
  // arguments corresponding to options/flags
  private List<String> optionArguments = new LinkedList<>();

  public List<String> getOptionArguments() {
    return ImmutableList.copyOf(optionArguments);
  }

  // options for frontend
  private List<String> frontendOptions = new LinkedList<>();

  public List<String> getFrontendOptions() {
    return ImmutableList.copyOf(frontendOptions);
  }

  // options for backend
  private List<String> backendOptions = new LinkedList<>();

  public List<String> getBackendOptions() {
    return ImmutableList.copyOf(backendOptions);
  }

  // options for engine
  private List<String> engineOptions = new LinkedList<>();

  public List<String> getEngineOptions() {
    return ImmutableList.copyOf(engineOptions);
  }

  // arguments corresponding to filenames
  private List<String> fileArguments = new LinkedList<>();

  public List<String> getFileArguments() {
    return ImmutableList.copyOf(fileArguments);
  }

  public ArgsPreprocessor(String args[]) {
    List<String> argList = new LinkedList<>(Arrays.asList(args));
    // start by looking for a double-dash, which escapes file arguments
    int dashIdx = -1;
    Iterator<String> it = argList.iterator();
    int i = 0;
    while (it.hasNext()) {
      String arg = it.next();
      // look for the double-dash if we haven't found it yet
      if (dashIdx == -1) {
        if (arg.equals("--")) {
          dashIdx = i;
        }
      } else {
        // otherwise extract these arguments as files
        fileArguments.add(arg);
      }
      i += 1;
    }
    if (dashIdx >= 0) {
      // keep only arguments before the dash in argList
      it = argList.iterator();
      i = 0;
      while (it.hasNext()) {
        it.next();
        if (i >= dashIdx) {
          it.remove();
        }
        i += 1;
      }
    }
    // now deal with whatever is left in argList
    // start by differentiating between option arguments, which start with a
    // dash, and
    // file arguments, which do not
    for (String arg : argList) {
      if (arg.startsWith("-")) {
        // option
        optionArguments.add(arg);
      } else {
        // file
        fileArguments.add(arg);
      }
    }

    // then, of the option arguments we've found, separate into
    // frontend options, whose first non-dash character is 'f',
    // backend options, whose first non-dash character is 'b',
    // and engine options, which are everything else
    for (String arg : optionArguments) {
      char firstNonDash = '-';
      for (i = 0; i < arg.length(); ++i) {
        char c = arg.charAt(i);
        if (c == '-') {
          continue;
        } else {
          firstNonDash = c;
          break;
        }
      }
      if (firstNonDash == '-') {
        // not found
        throw new IllegalArgumentException("malformed command-line argument '"
            + arg + "'");
      } else if (firstNonDash == 'f') {
        frontendOptions.add(arg);
      } else if (firstNonDash == 'b') {
        backendOptions.add(arg);
      } else {
        engineOptions.add(arg);
      }
    }
  }
}
