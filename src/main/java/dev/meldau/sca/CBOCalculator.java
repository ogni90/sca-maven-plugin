package dev.meldau.sca;

import org.jgrapht.graph.DirectedMultigraph;

import java.util.ArrayList;
import java.util.HashMap;

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
 * This class calculates the Coupling Between Objects metric for a provided program in form of a
 * multigraph.
 */
public class CBOCalculator {

  final DirectedMultigraph<String, LabeledEdge> CLASS_GRAPH;

  public CBOCalculator(DirectedMultigraph<String, LabeledEdge> classGraph) {
    this.CLASS_GRAPH = classGraph;
  }

  HashMap<String, HashMap<String, Integer>> calculateCBO() {
    HashMap<String, HashMap<String, Integer>> CBOScores = new HashMap<>();
    for (String vertex : CLASS_GRAPH.vertexSet()) {
      HashMap<String, Integer> degrees = new HashMap<>();
      degrees.put("degree", CLASS_GRAPH.degreeOf(vertex));
      degrees.put("inDegree", CLASS_GRAPH.inDegreeOf(vertex));
      degrees.put("outDegree", CLASS_GRAPH.outDegreeOf(vertex));
      CBOScores.put(vertex, degrees);
    }
    return CBOScores;
  }

  ArrayList<ArrayList<String>> calculatePairCBO() {
    // Array with list of pairs and respective values
    ArrayList<ArrayList<String>> pairCBOListOfLists = new ArrayList<>();
    // iterates over all unique pairs of vertices
    for (int i = 0; i < CLASS_GRAPH.vertexSet().size(); i++) {
      for (int j = i + 1; j < CLASS_GRAPH.vertexSet().size(); j++) {

        // Add sum of all edges between vertices to hashmap
        ArrayList<String> pairCBOValues = new ArrayList<>();
        pairCBOValues.add(CLASS_GRAPH.vertexSet().toArray()[i].toString());
        pairCBOValues.add(CLASS_GRAPH.vertexSet().toArray()[j].toString());
        pairCBOValues.add(String.valueOf(
            CLASS_GRAPH
                    .getAllEdges(
                        (String) CLASS_GRAPH.vertexSet().toArray()[i],
                        (String) CLASS_GRAPH.vertexSet().toArray()[j])
                    .size()
                + CLASS_GRAPH
                    .getAllEdges(
                        (String) CLASS_GRAPH.vertexSet().toArray()[j],
                        (String) CLASS_GRAPH.vertexSet().toArray()[i])
                    .size()));
        pairCBOListOfLists.add(pairCBOValues);
      }
    }
    return pairCBOListOfLists;
  }
}
