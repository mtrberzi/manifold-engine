package org.manifold;

import java.util.LinkedList;
import java.util.List;

import com.google.common.collect.ImmutableList;

/**
 * Scan the supplied command line arguments and look for any occurrences
 * of the following three options (which can begin with
 * either one or two dashes):
 * -f,--frontend [frontend-name]
 * -b,--backend [backend-name]
 * -c,--compile-only (do not load a backend, emit a schematic)
 * -i,--intermediate (read a schematic, do not load a frontend)
 * --search-url [url]
 */
public class InitialOptions {

  private List<String> remainingArguments = new LinkedList<String>();
  public List<String> getRemainingArguments() {
    return ImmutableList.copyOf(remainingArguments);
  }

  private String frontendName = "default";
  public String getFrontendName() {
    return frontendName;
  }

  private String backendName = ""; // there is no default backend
  public String getBackendName() {
    return backendName;
  }

  private boolean compile = false;
  public boolean compileOnly() {
    return compile;
  }

  private boolean intermediate = false;
  public boolean useIntermediate() {
    return intermediate;
  }

  private List<String> extraSearchURLs = new LinkedList<String>();
  public List<String> getExtraSearchURLs() {
    return ImmutableList.copyOf(extraSearchURLs);
  }

  public InitialOptions(String args[]) {
    for (int i = 0; i < args.length; ++i) {
      String arg = args[i];
      // options begin with one or two dashes;
      // seeing an argument that matches "--" means "stop processing"

      // look for option values starting here
      boolean isOption = false;
      int startIdx = 0;
      if (arg.equals("--")) {
        // this and every other argument are exempt from further processing
        for (int lastI = i; lastI < args.length; ++lastI) {
          remainingArguments.add(args[lastI]);
        }
        return;
      }
      if (arg.startsWith("-")) {
        if (arg.length() == 1) {
          throw new IllegalArgumentException(
              "malformed command-line argument '" + arg + "'");
        }
        isOption = true;
        startIdx = 1;
        if (arg.charAt(1) == '-') {
          startIdx = 2;
          // "--" is a proper prefix of arg
          if (arg.charAt(2) == '-') {
            // "---" or similar; this is not allowed
            throw new IllegalArgumentException(
                "malformed command-line argument '" + arg + "'");
          }
        }
      }
      if (isOption) {
        String optionString = arg.substring(startIdx);
        if (optionString.equals("f")
            || optionString.equals("frontend")) {
          if (i+1 >= args.length) {
            throw new IllegalArgumentException(
                "option " + arg + " requires a parameter");
          } else if (args[i+1].startsWith("-")) {
            throw new IllegalArgumentException(
                "option " + arg + " requires a parameter");
          } else {
            ++i;
            frontendName = args[i];
          }
        } else if (optionString.equals("b")
            || optionString.equals("backend")) {
          if (i+1 >= args.length) {
            throw new IllegalArgumentException(
                "option " + arg + " requires a parameter");
          } else if (args[i+1].startsWith("-")) {
            throw new IllegalArgumentException(
                "option " + arg + " requires a parameter");
          } else {
            ++i;
            backendName = args[i];
          }
        } else if (optionString.equals("i")
            || optionString.equals("intermediate")) {
          intermediate = true;
        } else if (optionString.equals("c")
            || optionString.equals("compile-only")) {
          compile = true;
        } else if (optionString.equals("search-url")) {
          if (i+1 >= args.length) {
            throw new IllegalArgumentException(
                "option " + arg + " requires a parameter");
          } else if (args[i+1].startsWith("-")) {
            throw new IllegalArgumentException(
                "option " + arg + " requires a parameter");
          } else {
            ++i;
            extraSearchURLs.add(args[i]);
          }
        } else {
          // unrecognized; pass through
          remainingArguments.add(arg);
        }
      } else {
        remainingArguments.add(arg);
      }
    } // for(i)
  }
}
