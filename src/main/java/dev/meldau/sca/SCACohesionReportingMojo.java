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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

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

@SuppressFBWarnings("DM_DEFAULT_ENCODING")
@Mojo(name = "sca-cohesion-report", defaultPhase = LifecyclePhase.SITE, threadSafe = true)
public class SCACohesionReportingMojo extends AbstractMavenReport {

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
    Map<String, Long> myMap = null;
    try {
      File cohesionJSONFile =
          new File(scaOutputDir.getAbsolutePath() + "/sca-cohesion-results.json");
      FileReader cohesionJSONFileReader = new FileReader(cohesionJSONFile);
      //noinspection unchecked
      myMap = (HashMap<String, Long>) jsonParser.parse(cohesionJSONFileReader);
      myLog.info("Map:" + myMap);
    } catch (FileNotFoundException e) {
      myLog.error(
          "Problems reading sca-output/sca-cohesion-output.json. Did you run the sca-cohesion target first?");
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
    mainSink.text("SCA Report");
    mainSink.title_();
    mainSink.head_();

    mainSink.body();

    // Average, best and worst score

    for (Map.Entry<String, Long> classEntry : Objects.requireNonNull(myMap).entrySet()) {
      myLog.debug("Cohesion Score for " + classEntry.getKey() + ": " + classEntry.getValue());
      if (classEntry.getValue() < 1) {
        continue;
      }
      String imageFileName = classEntry.getKey().replace("/", "_") + "_lcom_graph.png";
      File imageFile = new File(outputDirectory.getAbsolutePath() + "/" + imageFileName);
      myLog.debug("Path for ImageFile: " + imageFile.getAbsoluteFile());
      File linkImageFile = new File(scaOutputDir.getAbsolutePath() + "/" + imageFileName);
      try {
        if (imageFile.exists()) {
          if (!imageFile.delete()) {
            throw new MavenReportException(
                "Couldn't delete old version of image: " + imageFile.getAbsoluteFile());
          }
        }
        Files.createLink(
            imageFile.getAbsoluteFile().toPath(), linkImageFile.getAbsoluteFile().toPath());
      } catch (IOException e) {
        e.printStackTrace();
      }

      myLog.debug("Cohesion Score for " + classEntry.getKey() + ": " + classEntry.getValue());
      String[] splitClassName = classEntry.getKey().split("/");
      String shortClassName = splitClassName[splitClassName.length - 1];
      mainSink.section1();
      mainSink.sectionTitle1();
      mainSink.text("Report for class " + shortClassName + ":");
      mainSink.sectionTitle1_();
      mainSink.paragraph();
      mainSink.text("LCOM Score: " + classEntry.getValue());
      mainSink.paragraph_();
      mainSink.figure();
      mainSink.figureGraphics(imageFileName);
      mainSink.figure_();
      mainSink.section1_();
    }
    mainSink.body_();
  }

  @Override
  public String getOutputName() {
    return "sca-cohesion-report";
  }

  @Override
  public String getName(Locale locale) {
    return "SCA: LCOM Report";
  }

  @Override
  protected MavenProject getProject() {
    return project;
  }

  @Override
  public String getDescription(Locale locale) {
    return "LCOM Scores Report";
  }
}
