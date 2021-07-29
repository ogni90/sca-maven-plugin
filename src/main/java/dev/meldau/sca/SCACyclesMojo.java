package dev.meldau.sca;

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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.json.simple.JSONValue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;


/**
 * Searches for cyclic dependcies
 * if cyclic dependencies are found a FeedbackArcSet is calculated
 * to show the user which dependencies may be removed to break the cycle
 *
 * @author Ingo Meldau
*/
@SuppressFBWarnings("DM_DEFAULT_ENCODING")
@Mojo(name = "sca-cycles", defaultPhase = LifecyclePhase.TEST, threadSafe = true)
public class SCACyclesMojo extends AbstractMojo {
  /** Maven Log Variable */
  Log mvnLog = this.getLog();

  /** If this parameter is true, the test phase will break with an exception */
  @Parameter(name = "breakOnCycle", required = true, defaultValue = "false")
  boolean breakOnCycle;
  /** Maven output Directory */
  @Parameter(property = "project.build.directory", required = true, readonly = true)
  private File outputDirectory;
  /** sca output Directory */
  @Parameter(
      name = "scaOutputDir",
      required = true,
      defaultValue = "${project.build.directory}/sca-output")
  private File scaOutputDir;

  /**
   * Getter for sca cycles output directory
   * (cannot use Parameter because inheriting of other defaults from annotations is not possible)
   */
  private File getScaCyclesOutputDir() {
    return new File(scaOutputDir.getAbsolutePath() + "/cycles");
  }

  /**
   * Save FeedbackArcSet as JSON
   */
  @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
  void saveFeedbackArcSetJSON(Set<InformativeEdge> feedbackArcSet) throws MojoExecutionException {
    Log myLog = this.getLog();
    ArrayList<ArrayList<String>> feedbackArcSetAL = new ArrayList<>();
    for (InformativeEdge edge : feedbackArcSet) {
      ArrayList<String> arc = new ArrayList<>();
      arc.add(edge.getSource().toString());
      arc.add(edge.getTarget().toString());
      feedbackArcSetAL.add(arc);
    }

    String myPath = scaOutputDir.getAbsolutePath() + "/cycles/feedback-arc-set.json";
    File myFile = new File(myPath);

    // delete result file from previous run if it exists
    if (myFile.isFile()) {
      //noinspection ResultOfMethodCallIgnored
      myFile.delete();
    }

    myLog.info("Writing Feedback Arc Set to JSON: " + myPath);

    try (FileWriter resultsFile = new FileWriter(myPath)) {
      resultsFile.write(JSONValue.toJSONString(feedbackArcSetAL));
      resultsFile.flush();
    } catch (IOException exception) {
      throw new MojoExecutionException("Couldn't write result JSON-File");
    }
  }

  /**
   *  Create and Save java dependency graph.<!-- -->
   *  Calculate FeedbackArcSet (FAS) in case of cyclic dependencies
   *  to show the likeliest set of dependencies that may be removed to break the cycle
   */
  @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
  public void execute() throws MojoFailureException, MojoExecutionException {

    // Create Directories if they don't exist
    for (File f : new File[] {scaOutputDir, getScaCyclesOutputDir()}) {
      if (!f.exists()) {
        //noinspection ResultOfMethodCallIgnored
        f.mkdir();
      }
    }

    // Create Graph
    JdepsGraphCreator jdepsGraphCreator =
        new JdepsGraphCreator(outputDirectory, getScaCyclesOutputDir());

    // Check if graph has cycles - if so calculate FAS
    if (jdepsGraphCreator.hasCycles()) {
      mvnLog.info("Found cycles finding solution.");
      FeedbackArcSetFinder feedbackArcSetFinder =
          new FeedbackArcSetFinder(jdepsGraphCreator.getCycleGraph());
      Set<InformativeEdge> feedbackArcSet = feedbackArcSetFinder.getFeedbackArcSet();
      saveFeedbackArcSetJSON(feedbackArcSet);
      mvnLog.info("This is the likeliest Set of dependencies to remove: " + feedbackArcSet);
      if (breakOnCycle) {
        throw new MojoFailureException("There is a cycle dependency in the project. Aborting.");
      }
      jdepsGraphCreator.saveGraphForReport(feedbackArcSet);
    } else {
      jdepsGraphCreator.saveGraphForReport();
    }
  }
}
