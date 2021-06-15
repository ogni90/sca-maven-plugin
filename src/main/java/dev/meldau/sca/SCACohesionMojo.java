package dev.meldau.sca;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.json.simple.JSONValue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

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
@Mojo(name = "sca-cohesion", defaultPhase = LifecyclePhase.TEST, threadSafe = true)
public class SCACohesionMojo extends AbstractMojo {

  Log myLog;

  /** Location of output directory */
  @Parameter(property = "project.build.directory", required = true, readonly = true)
  private File outputDirectory;
  /** sca output Directory */
  @Parameter(
      name = "scaOutputDir",
      required = true,
      defaultValue = "${project.build.directory}/sca-output")
  private File scaOutputDir;

  void saveResultJSON(Map<String, Integer> myLcomScores) throws MojoExecutionException {
    String myPath = scaOutputDir.getAbsolutePath() + "/sca-cohesion-results.json";
    myLog.info("Writing Results JSON: " + myPath);

    try (FileWriter resultsFile = new FileWriter(myPath)) {
      resultsFile.write(JSONValue.toJSONString(myLcomScores));
      resultsFile.flush();
    } catch (IOException exception) {
      throw new MojoExecutionException("Couldn't write result JSON-File");
    }
  }

  @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
  @Override
  public void execute() throws MojoExecutionException {
    /* Maven Log Variable */
    myLog = this.getLog();

    // Create Directories if they don't exist
    for (File f : new File[] {outputDirectory, scaOutputDir}) {
      if (!f.exists()) {
        //noinspection ResultOfMethodCallIgnored
        f.mkdir();
      }
    }

    /* Result Variable */
    Map<String, Integer> lcomScores;

    ClassFileFinder classFileFinder =
        new ClassFileFinder(new File(outputDirectory.getAbsolutePath() + "/classes"));
    try {
      LCOMScoreCalculator lcomScoreCalculator =
          new LCOMScoreCalculator(classFileFinder.getClassFiles());
      lcomScores = lcomScoreCalculator.getLCOMScores();
      lcomScoreCalculator.saveGraph(scaOutputDir);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      throw new MojoExecutionException("Output directory does not exist.");
    } catch (IOException e) {
      e.printStackTrace();
      throw new MojoExecutionException("File not found when trying to gather class files.");
    }

    // Save cohesion output to JSON-file for reporting plugin
    saveResultJSON(lcomScores);

    myLog.info("LCOM Scores: " + lcomScores);
  }
}
