package dev.meldau.sca;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.sink.SinkEventAttributes;
import org.apache.maven.doxia.sink.impl.SinkEventAttributeSet;
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
import java.util.*;

import static org.apache.maven.doxia.sink.Sink.JUSTIFY_LEFT;

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
 * Pairwise Coupling Between Objects (pair CBO) Reporting Mojo
 *
 * @author Ingo Meldau
 */
@SuppressFBWarnings("DM_DEFAULT_ENCODING")
@Mojo(name = "sca-coupling-pair-cbo-report", defaultPhase = LifecyclePhase.SITE, threadSafe = true)
public class SCACouplingPairCBOReportingMojo extends AbstractMavenReport {

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

  /**
   * Build pairwise CBO Report with Maven Site Plugin
   */
  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
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
    ArrayList<ArrayList<String>> pairCBOList = null;
    try {
      File couplingCBOJSONFile =
          new File(scaOutputDir.getAbsolutePath() + "/sca-coupling-pair-cbo-results.json");
      if (!couplingCBOJSONFile.isFile()) {
        myLog.info("No result File. Skipping Pair CBO results report...");
        return;
      }
      try (FileReader couplingCBOJSONFileReader = new FileReader(couplingCBOJSONFile)) {
        //noinspection unchecked
        pairCBOList = (ArrayList<ArrayList<String>>) jsonParser.parse(couplingCBOJSONFileReader);
      }
      myLog.info("Array:" + pairCBOList);
    } catch (FileNotFoundException e) {
      myLog.error(
          "Problems reading sca-output/sca-coupling-pair-cbo-results.json Did you run the sca-cohesion target first?");
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
    mainSink.text("SCA Coupling Pair CBO Report");
    mainSink.title_();
    mainSink.head_();

    mainSink.body();

    String[][] colorLegend = {
      {"Superclass", "Violet"},
      {"Instance Variable", "Chocolate"},
      {"Calls Method", "Blue"},
      {"Local Variable", "Orange"},
      {"Parameter Type", "Green"},
      {"Access Public Variable", "Red"},
    };

    SinkEventAttributeSet sinkEventAttributeSetColor = new SinkEventAttributeSet();

    mainSink.section1();
    mainSink.sectionTitle1();
    mainSink.text("Coupling Graph");
    mainSink.sectionTitle1_();
    File couplingImage = new File(scaOutputDir.getAbsolutePath() + "/" + "coupling_graph.png");
    if (!couplingImage.isFile()) {
      mainSink.text(
          "The coupling graph was not generated. This can occur when working on very large class structures.");
    } else {
      File copiedFile = new File(outputDirectory.getAbsolutePath() + "/coupling_graph.png");
      try {
        if (copiedFile.isFile()) {
          //noinspection ResultOfMethodCallIgnored
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
    mainSink.table();
    mainSink.tableRows(new int[] {JUSTIFY_LEFT, JUSTIFY_LEFT}, true);
    mainSink.tableRow();
    mainSink.tableHeaderCell();
    mainSink.text("Color");
    mainSink.tableHeaderCell_();
    mainSink.tableHeaderCell();
    mainSink.text("Definition");
    mainSink.tableHeaderCell_();
    for (String[] color : colorLegend) {
      sinkEventAttributeSetColor.addAttribute(SinkEventAttributes.BGCOLOR, color[1]);
      mainSink.tableRow_();
      mainSink.tableRow();
      mainSink.tableCell(sinkEventAttributeSetColor);
      mainSink.text(color[1]);
      mainSink.tableCell_();
      mainSink.tableCell();
      mainSink.text(color[0]);
      mainSink.tableCell_();
      mainSink.tableRow_();
      sinkEventAttributeSetColor.removeAttribute(SinkEventAttributes.BGCOLOR);
    }
    mainSink.tableRows_();
    mainSink.table_();
    Objects.requireNonNull(pairCBOList).sort((al1, al2) -> (Integer.parseInt(al2.get(2)) - (Integer.parseInt(al1.get(2)))));
    for( ArrayList<String> arrayList : pairCBOList ) {
      mainSink.section2();
      mainSink.sectionTitle2();
      String sectionTitle = "Pair " + arrayList.get(0) + " and " + arrayList.get(1);
      mainSink.text(sectionTitle);
      mainSink.sectionTitle2_();
      mainSink.text("Pair CBO: " + arrayList.get(2));
      mainSink.section2_();
    }
    mainSink.body_();
  }

  @Override
  public String getOutputName() {
    return "sca-coupling-pair-cbo-report";
  }

  @Override
  public String getName(Locale locale) {
    return "SCA: Pair CBO Report";
  }

  @Override
  protected MavenProject getProject() {
    return project;
  }

  @Override
  public String getDescription(Locale locale) {
    return "CBO pair scores Report";
  }
}
