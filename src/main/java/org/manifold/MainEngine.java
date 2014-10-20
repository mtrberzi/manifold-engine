package org.manifold;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.manifold.compiler.Backend;
import org.manifold.compiler.Frontend;
import org.manifold.compiler.middle.Schematic;
import org.manifold.compiler.middle.serialization.SchematicDeserializer;
import org.manifold.compiler.middle.serialization.SchematicSerializer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class MainEngine {

  private static Logger log = LogManager.getLogger("MainEngine");

  private void setupLogging() {
    PatternLayout layout = new PatternLayout(
        "%-5p [%t]: %m%n");
    LogManager.getRootLogger().addAppender(
        new ConsoleAppender(layout, ConsoleAppender.SYSTEM_ERR));
  }

  private InitialOptions initialOptions;
  private String arguments[]; // whatever is left after parsing initial options
  private Options options;

  private boolean verbose;
  public boolean isVerbose() {
    return verbose;
  }

  private void createOptionVerbose() {
    Option oVerbose = new Option("v", "verbose", false,
        "Display informational messages in addition to warnings and errors");
    options.addOption(oVerbose);
  }

  private void collectOptionVerbose(CommandLine cmd) {
    verbose = false;
    if (cmd.hasOption("verbose")) {
      verbose = true;
    }
  }

  private void createOptionDefinitions() {
    createOptionVerbose();
  }

  private void collectOptions(CommandLine cmd) {
    collectOptionVerbose(cmd);
  }

  private ServiceLoader<Frontend> frontendLoader;
  private ServiceLoader<Backend> backendLoader;

  private Frontend frontend = null;
  private boolean useFrontend = true;
  private Backend backend = null;
  private boolean useBackend = true;

  private Frontend loadFrontend(String frontendName) {
    for (Frontend f : frontendLoader) {
      if (f.getFrontendName().equals(frontendName)) {
        return f;
      }
    }
    return null;
  }

  private Backend loadBackend(String backendName) {
    for (Backend b : backendLoader) {
      if (b.getBackendName().equals(backendName)) {
        return b;
      }
    }
    return null;
  }

  private MainEngine(String args[]) throws MalformedURLException {
    setupLogging();
    this.initialOptions = new InitialOptions(args);
    this.arguments = initialOptions.getRemainingArguments()
        .toArray(new String[0]);
    List<URL> urlList = new LinkedList<URL>();
    for (String path : initialOptions.getExtraSearchURLs()) {
      File file = new File(path);
      if (!file.exists()) {
        log.error("path not found: '" + path + "'");
        return;
      }
      if (file.isDirectory()) {
        List<File> files = new ArrayList<File>(Arrays.asList(file.listFiles()));
        for (File subfile : files) {
          if (subfile.isFile()) {
            // if it is a JAR, add it to the URL
            String name = subfile.getName();
            if (name.endsWith(".jar")) {
              urlList.add(subfile.toURI().toURL());
            }
          }
        }
      } else if (file.isFile()) {
        urlList.add(file.toURI().toURL());
      }
    }
    URL[] urls = urlList.toArray(new URL[0]);
    URLClassLoader urlLoader = new URLClassLoader(urls,
        MainEngine.class.getClassLoader());
    // load all frontends and backends
    frontendLoader = ServiceLoader.load(Frontend.class, urlLoader);
    backendLoader = ServiceLoader.load(Backend.class, urlLoader);
  }

  public void run() throws Exception {
    // there are three possible modes of operation:
    // 1. frontend + backend (default:
    //    no special options, backend must be specified)
    // 2. frontend only ("--compile-only")
    // 3. backend only ("--intermediate")
    if (initialOptions.compileOnly() && initialOptions.useIntermediate()) {
      // this is not a legal combination
      log.error(
          "cannot specify both -c/--compile-only and -i/--intermediate");
      return;
    } else if (initialOptions.compileOnly()) {
      useFrontend = true;
      useBackend = false;
    } else if (initialOptions.useIntermediate()) {
      useFrontend = false;
      useBackend = true;
    }
    if (useFrontend) {
      frontend = loadFrontend(initialOptions.getFrontendName());
      if (frontend == null) {
        log.error("could not load frontend '" +
            initialOptions.getFrontendName() + "'");
        return;
      }
    }
    if (useBackend) {
      // backend MUST be specified if it will be used
      if (initialOptions.getBackendName().isEmpty()) {
        log.error(
            "backend must be specified (use -b/--backend [backend-name])");
        return;
      }
      backend = loadBackend(initialOptions.getBackendName());
      if (backend == null) {
        log.error("could not load backend '" +
            initialOptions.getBackendName() + "'");
        return;
      }
    }
    // set up and parse the full argument list
    options = new Options();
    createOptionDefinitions();
    if (useFrontend) {
      frontend.registerArguments(options);
    }
    if (useBackend) {
      backend.registerArguments(options);
    }
    CommandLineParser parser = new org.apache.commons.cli.BasicParser();
    CommandLine cmd = parser.parse(options, arguments);
    collectOptions(cmd);
    if (verbose) {
      LogManager.getRootLogger().setLevel(Level.ALL);
    } else {
      LogManager.getRootLogger().setLevel(Level.WARN);
    }
    // now begin workflow
    if (cmd.getArgList().isEmpty()) {
      log.warn("no input files specified");
      return;
    }

    if (initialOptions.useIntermediate()) {
      // if we're only using the intermediate,
      // deserialize the first argument and invoke backend
      // TODO strategy for handling multiple input files
      Path schematicPath = Paths.get(cmd.getArgs()[0]);
      if (!(Files.isReadable(schematicPath))) {
        log.error("cannot open '" + schematicPath.toString() + "': "
            + "file is not readable");
        return;
      }
      try (BufferedReader reader = Files.newBufferedReader(schematicPath)) {
        JsonObject schematicJson = new JsonParser().parse(reader)
            .getAsJsonObject();
        SchematicDeserializer deserial = new SchematicDeserializer();
        Schematic schematic = deserial.deserialize(schematicJson);
        backend.invokeBackend(schematic, cmd);
      } catch (IOException e) {
        log.error("error reading '" + schematicPath.toString() + "': "
            + e.getMessage());
        return;
      }
    } else if (initialOptions.compileOnly()) {
      // if we're only compiling,
      // invoke the frontend and serialize the schematic it provides
      Schematic schematic = frontend.invokeFrontend(cmd);
      JsonObject schematicJson = SchematicSerializer.serialize(schematic);
      File schematicFile = null; // TODO file path
      FileWriter fileWriter = new FileWriter(schematicFile);
      try {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(schematicJson.toString());
        String prettyJsonString = gson.toJson(je);
        fileWriter.write(prettyJsonString);
      } finally {
        fileWriter.close();
      }
    } else {
      // invoke frontend, then pass the schematic directly to the backend
      Schematic schematic = frontend.invokeFrontend(cmd);
      backend.invokeBackend(schematic, cmd);
    }
  }

  public static void main(String args[]) {
    MainEngine engine = null;
    try {
      engine = new MainEngine(args);
      engine.run();
    } catch (Exception e) {
      log.error(e.getMessage());
      if (engine != null && engine.isVerbose()) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        log.error(sw.toString());
      }
      System.exit(1);
    }
  }
}
