package dev.meldau.sca;

import guru.nidi.graphviz.attribute.Color;
import guru.nidi.graphviz.attribute.Style;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.Link;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.parse.Parser;
import org.apache.maven.plugin.MojoExecutionException;
import org.jgrapht.alg.cycle.CycleDetector;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.nio.dot.DOTExporter;
import org.jgrapht.nio.dot.DOTImporter;

import java.io.*;
import java.util.Optional;
import java.util.Set;
import java.util.spi.ToolProvider;

public class JdepsGraphCreator {

  private final File CLASS_DIR;
  private final File OUTPUT_DIR;
  private SimpleDirectedGraph<String, InformativeEdge> cycleGraph;

  public JdepsGraphCreator(File classDir, File outputDir) {
    this.CLASS_DIR = classDir;
    this.OUTPUT_DIR = outputDir;
    this.createDotFiles();
    this.createGraph();
  }

  public SimpleDirectedGraph<String, InformativeEdge> getCycleGraph() {
    return cycleGraph;
  }

  public boolean hasCycles() {
    CycleDetector<String, InformativeEdge> cycleDetector = new CycleDetector<>(cycleGraph);
    return cycleDetector.detectCycles();
  }

  /** Runs jdeps to generate dot files */
  void createDotFiles() {
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

  void createGraph() {
    cycleGraph = new SimpleDirectedGraph<>(InformativeEdge.class);

    try (FileReader dotFileReader =
        new FileReader(OUTPUT_DIR.getAbsolutePath() + "/" + CLASS_DIR.getName() + ".dot")) {
      // Create Graph
      DOTImporter<String, InformativeEdge> dotImporter = new DOTImporter<>();
      dotImporter.setVertexFactory(label -> label.split(" ")[0]);
      dotImporter.importGraph(cycleGraph, dotFileReader);

      DOTExporter<String, InformativeEdge> dotExporter =
          new DOTExporter<>(v -> v.replace('.', '_').replace('$', '_'));
      dotExporter.exportGraph(
          cycleGraph,
          new FileWriter(OUTPUT_DIR.getAbsolutePath() + "/" + CLASS_DIR.getName() + "_clean.dot"));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  void saveGraphForReport() throws MojoExecutionException {
    try (InputStream dot =
        new FileInputStream(
            OUTPUT_DIR.getAbsolutePath() + "/" + CLASS_DIR.getName() + "_clean.dot")) {
      MutableGraph g = new Parser().read(dot);
      Graphviz.fromGraph(g)
          .render(Format.DOT)
          .toFile(
              new File(
                  OUTPUT_DIR.getAbsolutePath() + "/" + CLASS_DIR.getName() + "_clean_colored.dot"));
      Graphviz.fromGraph(g)
          .render(Format.PNG)
          .toFile(
              new File(
                  OUTPUT_DIR.getAbsolutePath() + "/" + CLASS_DIR.getName() + "_clean_colored.png"));
    } catch (IOException e) {
      e.printStackTrace();
      throw new MojoExecutionException("Could not read clean DOT File.");
    }
  }

  void saveGraphForReport(Set<InformativeEdge> feedbackArcSet) throws MojoExecutionException {
    try (InputStream dot =
        new FileInputStream(
            OUTPUT_DIR.getAbsolutePath() + "/" + CLASS_DIR.getName() + "_clean.dot")) {
      MutableGraph g = new Parser().read(dot);
      // Display edges which are part of the FAC in bold Red
      for (Link edge : g.edges()) {
        for (InformativeEdge arc : feedbackArcSet) {
          if (edge.from().name().value().equals(
                   arc.getSource().toString().replace('.', '_').replace('$', '_'))
              && edge.to().name().value().equals(arc.getTarget().toString().replace('.', '_').replace('$', '_'))) {
            edge.add(Color.RED);
            edge.add(Style.BOLD);
          }
        }
      }
      Graphviz.fromGraph(g)
          .render(Format.DOT)
          .toFile(
              new File(
                  OUTPUT_DIR.getAbsolutePath() + "/" + CLASS_DIR.getName() + "_clean_colored.dot"));
      Graphviz.fromGraph(g)
          .render(Format.PNG)
          .toFile(
              new File(
                  OUTPUT_DIR.getAbsolutePath() + "/" + CLASS_DIR.getName() + "_clean_colored.png"));
    } catch (IOException e) {
      e.printStackTrace();
      throw new MojoExecutionException("Could not read clean DOT File.");
    }
  }
}
