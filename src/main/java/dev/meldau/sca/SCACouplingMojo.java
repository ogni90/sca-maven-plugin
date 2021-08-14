package dev.meldau.sca;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.jgrapht.graph.DirectedMultigraph;
import org.json.simple.JSONValue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import static java.util.Collections.max;

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
 * This Mojo can be used for calculating Coupling Between Objects (CBO).
 *
 * @author Ingo Meldau
 */
@SuppressFBWarnings({"DM_DEFAULT_ENCODING", "DM_DEFAULT_ENCODING"})
@Mojo(name = "sca-coupling", defaultPhase = LifecyclePhase.TEST, threadSafe = true)
public class SCACouplingMojo extends AbstractMojo {
  /** break threshold for CBO */
  @Parameter(name = "breakOnCBO", required = true, defaultValue = "0")
  int breakOnCBO;
  /** break threshold for Pair-CBO */
  @Parameter(name = "breakOnPairCBO", required = true, defaultValue = "0")
  int breakOnPairCBO;
  /** Location of the file. */
  @Parameter(property = "project.build.directory", required = true, readonly = true)
  private File outputDirectory;
  /** sca output Directory */
  @Parameter(
      name = "scaOutputDir",
      required = true,
      defaultValue = "${project.build.directory}/sca-output")
  private File scaOutputDir;

  private Log myLog;

  /** Save CBO results as JSON */
  void saveCBOResultJSON(HashMap<String, Integer> myCBOScores) throws MojoExecutionException {
    String myPath = scaOutputDir.getAbsolutePath() + "/sca-coupling-cbo-results.json";
    myLog.info("Writing Results JSON: " + myPath);

    try (FileWriter resultsFile = new FileWriter(myPath)) {
      resultsFile.write(JSONValue.toJSONString(myCBOScores));
      resultsFile.flush();
    } catch (IOException exception) {
      throw new MojoExecutionException("Couldn't write result JSON-File");
    }
  }

  /** Save pairwise CBO results as JSON */
  void savePairCBOResultJSON(ArrayList<ArrayList<String>> myCBOScores)
      throws MojoExecutionException {
    String myPath = scaOutputDir.getAbsolutePath() + "/sca-coupling-pair-cbo-results.json";
    myLog.info("Writing Results JSON: " + myPath);

    try (FileWriter resultsFile = new FileWriter(myPath)) {
      resultsFile.write(JSONValue.toJSONString(myCBOScores));
      resultsFile.flush();
    } catch (IOException exception) {
      throw new MojoExecutionException("Couldn't write result JSON-File");
    }
  }

  /** Calculates CBO and pairwise CBO values for all classes and saves the results as JSON */
  @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    myLog = this.getLog();

    // Create target dir and cycles-output dir in target if they don't exist
    for (File f : new File[] {outputDirectory, scaOutputDir}) {
      if (!f.exists()) {
        //noinspection ResultOfMethodCallIgnored
        f.mkdirs();
      }
    }

    HashMap<String, Integer> CBOValues;
    ArrayList<ArrayList<String>> PairCBOValues;

    try {
      CouplingMultiGraphGenerator couplingMultiGraphGenerator =
          new CouplingMultiGraphGenerator(
              new File(outputDirectory.getAbsolutePath() + "/classes"));
      DirectedMultigraph<String, LabeledEdge> couplingMultiGraph =
          couplingMultiGraphGenerator.getGraph();
      // Save graph as DOT File and Image for reporting
      couplingMultiGraphGenerator.saveGraph(scaOutputDir);

      CBOCalculator cboCalculator = new CBOCalculator(couplingMultiGraph);
      CBOValues = cboCalculator.calculateCBO();
      PairCBOValues = cboCalculator.calculatePairCBO();

    } catch (IOException e) {
      e.printStackTrace();
      throw new MojoExecutionException(
          "The plugin encountered a problem while reading the class files.");
    }

    saveCBOResultJSON(CBOValues);
    savePairCBOResultJSON(PairCBOValues);

    // Check if CBO Metric exceeds configured threshold
    if (breakOnCBO != 0) {
      if (max(CBOValues.values()) > breakOnCBO) {
        throw new MojoFailureException("The threshold for the CBO metric is exceeded.");
      }
    }

    // Check if Pair-CBO Metric exceeds configured threshold
    if (breakOnPairCBO != 0) {
      for (ArrayList<String> values : PairCBOValues) {
        if (Integer.parseInt(values.get(2)) > breakOnPairCBO) {
          throw new MojoFailureException("The threshold for the Pair-CBO metric is exceeded.");
        }
      }
    }
  }
}
