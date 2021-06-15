package dev.meldau.sca;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.spi.ToolProvider;

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
          if (Objects.requireNonNull(edge.from())
                  .name()
                  .value()
                  .equals(arc.getSource().toString().replace('.', '_').replace('$', '_'))
              && edge.to()
                  .name()
                  .value()
                  .equals(arc.getTarget().toString().replace('.', '_').replace('$', '_'))) {
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
