package dev.meldau.sca;

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

@Mojo(name = "sca-coupling", defaultPhase = LifecyclePhase.TEST, threadSafe = true)
public class SCACouplingMojo extends AbstractMojo {
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

  void saveCBOResultJSON(HashMap<String, HashMap<String,Integer>> myCBOScores) throws MojoExecutionException {
    String myPath = scaOutputDir.getAbsolutePath() + "/sca-coupling-cbo-results.json";
    myLog.info("Writing Results JSON: " + myPath);

    try (FileWriter resultsFile = new FileWriter(myPath)) {
      resultsFile.write(JSONValue.toJSONString(myCBOScores));
      resultsFile.flush();
    } catch (IOException exception) {
      throw new MojoExecutionException("Couldn't write result JSON-File");
    }
  }

  void savePairCBOResultJSON(HashMap<ArrayList<String>, Integer> myCBOScores) throws MojoExecutionException {
    String myPath = scaOutputDir.getAbsolutePath() + "/sca-coupling-pair-cbo-results.json";
    myLog.info("Writing Results JSON: " + myPath);

    try (FileWriter resultsFile = new FileWriter(myPath)) {
      resultsFile.write(JSONValue.toJSONString(myCBOScores));
      resultsFile.flush();
    } catch (IOException exception) {
      throw new MojoExecutionException("Couldn't write result JSON-File");
    }
  }

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    myLog = this.getLog();

    // Create target dir and cycles-output dir in target if they don't exist
    for (File f : new File[] {outputDirectory, scaOutputDir}) {
      if (!f.exists()) {
        f.mkdirs();
      }
    }

    HashMap<String,HashMap<String,Integer>> CBOValues;
    HashMap<ArrayList<String>, Integer> PairCBOValues;

    try {
      CouplingMultiGraphGenerator couplingMultiGraphGenerator =
          new CouplingMultiGraphGenerator(new File(outputDirectory.getAbsolutePath() + "/classes"), myLog);
      DirectedMultigraph<String, LabeledEdge> couplingMultiGraph =
          couplingMultiGraphGenerator.getGraph();
      // Save graph as DOT File and Image for reporting
      couplingMultiGraphGenerator.saveGraph(scaOutputDir);

      CBOCalculator cboCalculator = new CBOCalculator(couplingMultiGraph);
      CBOValues = cboCalculator.calculateCBO();
      PairCBOValues = cboCalculator.calculatePairCBO();

    } catch (IOException e) {
      e.printStackTrace();
      throw new MojoExecutionException("The plugin encountered a problem while reading the class files.");
    }

    saveCBOResultJSON(CBOValues);
    savePairCBOResultJSON(PairCBOValues);


  }
}
