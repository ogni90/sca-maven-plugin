package dev.meldau.sca;

import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Mojo(name = "sca-coupling-cbo-report", defaultPhase = LifecyclePhase.SITE, threadSafe = true)
public class SCACouplingCBOReportingMojo extends AbstractMavenReport {

  @Parameter(
      defaultValue = "${project.reporting.outputDirectory}",
      readonly = true,
      required = true)
  protected File outputDirectory;
  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  private MavenProject project;
  /** sca output Directory */
  @Parameter(
      name = "scaOutputDir",
      required = true,
      defaultValue = "${project.build.directory}/sca-output")
  private File scaOutputDir;

  @Override
  protected String getOutputDirectory() {
    getLog().info(outputDirectory.toString());

    return outputDirectory.toString();
  }

  @Override
  protected void executeReport(Locale locale) throws MavenReportException {
    // initialize Logger
    Log myLog = getLog();

    myLog.info("Creating report for sca-maven-plugin");

    File scaReportDirectory = new File(outputDirectory.getAbsolutePath() + "/sca-report");

    // Create subdir for sca-report if it doesn't exit
    if (!scaReportDirectory.exists()) {
      if (!scaReportDirectory.mkdir()) {
        throw new MavenReportException(
            "Couldn't create directory for Report under " + scaReportDirectory.getAbsolutePath());
      }
    }

    // Read JSON Results from previous cohesion run
    JSONParser jsonParser = new JSONParser();
    HashMap<String, HashMap<String, Integer>> myMap = null;
    try {
      File couplingCBOJSONFile =
          new File(scaOutputDir.getAbsolutePath() + "/sca-coupling-cbo-results.json");
      if (!couplingCBOJSONFile.isFile()) {
        myLog.info("No result File. Skipping CBO results report...");
        return;
      }
      FileReader couplingCBOJSONFileReader = new FileReader(couplingCBOJSONFile);
      myMap = (HashMap<String, HashMap<String, Integer>>) jsonParser.parse(couplingCBOJSONFileReader);
      myLog.info("Map:" + myMap);
    } catch (FileNotFoundException e) {
      myLog.error(
          "Problems reading sca-output/sca-coupling-cbo-results.json Did you run the sca-cohesion target first?");
      e.printStackTrace();
    } catch (ParseException | IOException e) {
      e.printStackTrace();
    }

    // Get the Maven Doxia Sink, which will be used to generate the
    // various elements of the document

    Sink mainSink = getSink();
    if (mainSink == null) {
      throw new MavenReportException("Could not get the Doxia sink");
    }

    // Header incl. Title
    mainSink.head();
    mainSink.title();
    mainSink.text("SCA Coupling CBO Report");
    mainSink.title_();
    mainSink.head_();

    mainSink.body();

    mainSink.section1();
    mainSink.sectionTitle1();
    mainSink.text("Coupling Graph");
    mainSink.sectionTitle1_();
    File couplingImage = new File(scaOutputDir.getAbsolutePath() + "/" + "coupling_graph.png");
    if (!couplingImage.isFile())
    {
      mainSink.text("The coupling graph was not generated. This can occur when working on very large class structures.");
    }
    else
    {
      File copiedFile = new File(outputDirectory.getAbsolutePath() + "/coupling_graph.png");
      try {
        if(copiedFile.isFile()) {
          copiedFile.delete();
        }
        Files.copy(couplingImage.toPath(), copiedFile.toPath());
      } catch (IOException e) {
        e.printStackTrace();
      }
      mainSink.figure();
      mainSink.figureGraphics(couplingImage.getAbsolutePath());
      mainSink.figure_();
    }

    for (Map.Entry<String,HashMap<String, Integer>> classEntry : myMap.entrySet()) {
      myLog.debug("Coupling CBO score for " + classEntry.getKey() + ": " + classEntry.getValue().get("degree"));
      mainSink.section2();
      mainSink.sectionTitle2();
      mainSink.text("Report for class " + classEntry.getKey() + ":");
      mainSink.sectionTitle2_();
      mainSink.paragraph();
      mainSink.text("CBO Score: " + classEntry.getValue().get("degree"));
      mainSink.paragraph_();
      mainSink.section2_();
    }
    mainSink.body_();
  }

  @Override
  public String getOutputName() {
    return "sca-coupling-cbo-report";
  }

  @Override
  public String getName(Locale locale) {
    return "SCA: CBO Report";
  }

  @Override
  protected MavenProject getProject() {
    return project;
  }

  @Override
  public String getDescription(Locale locale) {
    return "CBO scores Report";
  }
}
