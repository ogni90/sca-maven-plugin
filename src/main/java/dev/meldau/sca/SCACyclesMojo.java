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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.jgrapht.graph.DefaultEdge;

import java.io.File;
import java.util.Set;

@Mojo(name = "sca-cycles", defaultPhase = LifecyclePhase.TEST, threadSafe = true)
public class SCACyclesMojo extends AbstractMojo {
  /** Maven Log Variable */
  Log mvnLog = this.getLog();

  /**
   * If this parameter is true, the test phase will break with an exception
   */
  @Parameter(
      name = "breakOnCycle",
      required = true,
      defaultValue = "false")
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
   * getter for sca output Directory (cannot use Parameter because inheriting of other defaults from
   * annotations is not possible)
   */
  private File getScaCyclesOutputDir() {
    return new File(scaOutputDir.getAbsolutePath() + "/cycles");
  }

  public void execute() throws MojoFailureException {

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
      Set<DefaultEdge> feedbackArcSet = feedbackArcSetFinder.getFeedbackArcSet();
      mvnLog.info("This is the likeliest Set of dependencies to remove: " + feedbackArcSet);
      if (breakOnCycle) {
        throw new MojoFailureException("There is a cycle dependency in the project. Aborting.");
      }
    }
  }
}
