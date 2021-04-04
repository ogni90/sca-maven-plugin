package dev.meldau.sca;

import org.jgrapht.alg.cycle.CycleDetector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.nio.dot.DOTImporter;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Optional;
import java.util.spi.ToolProvider;

public class JdepsGraphCreator {

  private final File CLASS_DIR;
  private final File OUTPUT_DIR;

  public SimpleDirectedGraph<String, DefaultEdge> getCycleGraph() {
    return cycleGraph;
  }

  public boolean hasCycles() {
    CycleDetector<String, DefaultEdge> cycleDetector = new CycleDetector<>(cycleGraph);
    return cycleDetector.detectCycles();
  }

  private SimpleDirectedGraph<String, DefaultEdge> cycleGraph;

   /**
   * Runs jdeps to generate dot files
   */
  void createDotFiles(){
    // Initialize jdeps Tool
    Optional<ToolProvider> jdeps = ToolProvider.findFirst("jdeps");

    // Get class level dependencies
    if (jdeps.isPresent()) {
      int jdepsReturnCode =
          jdeps
              .get()
              .run(
                  System.out,
                  System.err,
                  "-dotoutput",
                  OUTPUT_DIR.getAbsolutePath(),
                  "-verbose:class",
                  "-filter:none",
                  CLASS_DIR.getAbsolutePath());
      if (jdepsReturnCode != 0) {
        throw new RuntimeException("Couldn't run jdeps");
      }
    } else {
      throw new RuntimeException("jdeps is not present");
    }
  }
  void createGraph(){
    cycleGraph = new SimpleDirectedGraph<>(DefaultEdge.class);

    try (FileReader dotFileReader =
                 new FileReader(OUTPUT_DIR.getAbsolutePath() + "/" + CLASS_DIR.getName() + ".dot")) {
      // Create Graph
      DOTImporter<String, DefaultEdge> dotImporter = new DOTImporter<>();
      dotImporter.setVertexFactory(label -> label.split(" ")[0]);
      dotImporter.importGraph(cycleGraph, dotFileReader);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

    public JdepsGraphCreator(File classDir, File outputDir) {
      this.CLASS_DIR = classDir;
      this.OUTPUT_DIR = outputDir;
      this.createDotFiles();
      this.createGraph();
    }
}
