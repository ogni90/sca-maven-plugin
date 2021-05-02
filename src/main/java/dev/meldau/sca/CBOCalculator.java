package dev.meldau.sca;

import org.jgrapht.graph.DirectedMultigraph;

import java.util.HashMap;

/**
 * This class calculates the Coupling Between Objects metric for a provided program in form of a
 * multigraph.
 */
public class CBOCalculator {

  DirectedMultigraph<String, LabeledEdge> classGraph;

  public CBOCalculator(DirectedMultigraph<String, LabeledEdge> classGraph) {
    this.classGraph = classGraph;
  }

  HashMap<String, HashMap<String, Integer>> calculateCBO() {
    HashMap<String, HashMap<String, Integer>> CBOScores = new HashMap<>();
    for (String vertex : classGraph.vertexSet()) {
      HashMap<String, Integer> degrees = new HashMap<>();
      degrees.put("degree", classGraph.degreeOf(vertex));
      degrees.put("inDegree", classGraph.inDegreeOf(vertex));
      degrees.put("outDegree", classGraph.outDegreeOf(vertex));
      CBOScores.put(vertex, degrees);
    }
    return CBOScores;
  }

  HashMap<String, Integer> calculatePairCBO() {
    // Array with list of pairs and respective values
    HashMap<String, Integer> hashPairCBO = new HashMap<>();
    // iterates over all unique pairs of vertices
    for (int i = 0; i < classGraph.vertexSet().size(); i++) {
      for (int j = i + 1; j < classGraph.vertexSet().size(); j++) {

        // Add sum of all edges between vertices to hashmap
        hashPairCBO.put(
            classGraph.vertexSet().toArray()[i] + "_" + classGraph.vertexSet().toArray()[j],
            classGraph
                    .getAllEdges(
                        (String) classGraph.vertexSet().toArray()[i],
                        (String) classGraph.vertexSet().toArray()[j])
                    .size()
                + classGraph
                    .getAllEdges(
                        (String) classGraph.vertexSet().toArray()[j],
                        (String) classGraph.vertexSet().toArray()[i])
                    .size());
      }
    }
    return hashPairCBO;
  }
}
