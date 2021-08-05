package dev.meldau.sca;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
import java.util.ArrayList;
import java.util.Locale;

/*
 * Copyright 2020-2021 Ingo Meldau
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Cyclic dependencies reporting Mojo
 *
 * @author Ingo Meldau
 */
@SuppressFBWarnings("DM_DEFAULT_ENCODING")
@Mojo(name = "sca-cycles-report", defaultPhase = LifecyclePhase.SITE, threadSafe = true)
public class SCACyclesReportingMojo extends AbstractMavenReport {

  @Parameter(
      defaultValue = "${project.reporting.outputDirectory}",
      readonly = true,
      required = true)
  protected File outputDirectory;

  @Parameter(defaultValue = "${project.build.directory}", readonly = true, required = true)
  protected File pluginOutputDirectory;

  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  private MavenProject project;
  /** sca output Directory */
  @Parameter(
      name = "scaOutputDir",
      required = true,
      defaultValue = "${project.build.directory}/sca-output")
  private File scaOutputDir;

  /**
   * Get report output directory
   *
   * @return outputDirectory
   */
  @Override
  protected String getOutputDirectory() {
    getLog().info(outputDirectory.toString());

    return outputDirectory.toString();
  }

  /** Build Cyclic Dependency Report with Maven Site Plugin */
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

    // Read JSON Results from previous cycles run
    JSONParser jsonParser = new JSONParser();
    ArrayList<ArrayList<String>> feedbackArcSet = new ArrayList<>();
    try {
      File feedbackArcSetJSONFile =
          new File(scaOutputDir.getAbsolutePath() + "/cycles/feedback-arc-set.json");
      if (feedbackArcSetJSONFile.isFile()) {
        FileReader cohesionJSONFileReader = new FileReader(feedbackArcSetJSONFile);
        //noinspection unchecked
        feedbackArcSet = (ArrayList<ArrayList<String>>) jsonParser.parse(cohesionJSONFileReader);
      }
      myLog.info("List:" + feedbackArcSet);
    } catch (FileNotFoundException e) {
      myLog.error(
          "Problems reading sca-output/cycles/feedback-arc-set.json. Did you run the sca-cycles target first?");
      e.printStackTrace();
    } catch (ParseException | IOException e) {
      e.printStackTrace();
    }

    // Get Image File of Graph
    File cyclesGraph =
        new File(
            scaOutputDir.getAbsolutePath()
                + "/cycles/"
                + pluginOutputDirectory.getName()
                + "_clean_colored.png");
    File cyclesGraphCopy =
        new File(
            outputDirectory.getAbsolutePath()
                + "/"
                + pluginOutputDirectory.getName()
                + "_clean_colored.png");

    try {
      if (cyclesGraphCopy.exists()) {
        if (!cyclesGraphCopy.delete()) {
          throw new MavenReportException(
              "Couldn't delete old version of image: " + cyclesGraphCopy.getAbsoluteFile());
        }
      }
      Files.createLink(
          cyclesGraphCopy.getAbsoluteFile().toPath(), cyclesGraph.getAbsoluteFile().toPath());
    } catch (IOException e) {
      e.printStackTrace();
      throw new MavenReportException("Problems while linking Image Files");
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
    mainSink.text("SCA Cycles Report");
    mainSink.title_();
    mainSink.head_();

    mainSink.body();
    mainSink.section1();
    mainSink.sectionTitle1();
    mainSink.text("Dependency Cycle Finder Report");
    mainSink.sectionTitle1_();
    if (feedbackArcSet.isEmpty()) {
      mainSink.text("No cycles have been found.");
    } else {
      mainSink.text("Loops have been found! Here is a suggested set of dependencies to remove:");
      for (ArrayList<String> arc : feedbackArcSet) {
        mainSink.paragraph();
        mainSink.text(arc.get(0) + " => " + arc.get(1));
        mainSink.paragraph_();
      }
    }
    if (cyclesGraph.isFile()) {
      mainSink.figure();
      mainSink.figureGraphics(cyclesGraphCopy.getAbsolutePath());
      mainSink.figure_();
    } else {
      mainSink.text(
          "Unfortunately there is no Image File of the graph - did the sca-cycles Plugin run first?");
    }

    mainSink.body_();
  }

  @Override
  public String getOutputName() {
    return "sca-report";
  }

  @Override
  public String getName(Locale locale) {
    return "SCA: Cycles Report";
  }

  @Override
  protected MavenProject getProject() {
    return project;
  }

  @Override
  public String getDescription(Locale locale) {
    return "Cycles Report";
  }
}
