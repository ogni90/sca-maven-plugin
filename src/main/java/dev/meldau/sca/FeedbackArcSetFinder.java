package dev.meldau.sca;

import org.jgrapht.Graphs;
import org.jgrapht.graph.SimpleDirectedGraph;

import java.util.*;

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
 * Implements FeedbackArcSet Algorithm from
 * https://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.47.7745&rep=rep1&type=pdf as Proposed
 * by Eades, Lin and Smyth
 */
public class FeedbackArcSetFinder {

  private final SimpleDirectedGraph<String, InformativeEdge> candidateGraph;

  public FeedbackArcSetFinder(SimpleDirectedGraph<String, InformativeEdge> candidateGraph) {
    this.candidateGraph = candidateGraph;
  }

  /**
   * Finds all sinks in a graph
   *
   * @return listOfSinks
   */
  List<String> getSinks(SimpleDirectedGraph<String, InformativeEdge> workingGraph) {
    List<String> listOfSinks = new ArrayList<>();
    for (String vertex : workingGraph.vertexSet()) {
      if (Graphs.successorListOf(workingGraph, vertex).isEmpty()) {
        listOfSinks.add(vertex);
      }
    }
    return listOfSinks;
  }

  /**
   * Finds all sources in a graph
   *
   * @return listOfSources
   */
  List<String> getSources(SimpleDirectedGraph<String, InformativeEdge> workingGraph) {
    List<String> listOfSources = new ArrayList<>();
    for (String vertex : workingGraph.vertexSet()) {
      if (Graphs.predecessorListOf(workingGraph, vertex).isEmpty()) {
        listOfSources.add(vertex);
      }
    }
    return listOfSources;
  }

  /**
   * finds the first vertex with the lowest outDegree - inDegree ratio
   *
   * @return bestVertex
   */
  String getHighestOutDegreeInDegreeRatio(
      SimpleDirectedGraph<String, InformativeEdge> workingGraph) {
    String bestVertex = null;
    Integer bestScore = null;
    for (String vertex : workingGraph.vertexSet()) {
      int inDegree = Graphs.predecessorListOf(workingGraph, vertex).size();
      int outDegree = Graphs.successorListOf(workingGraph, vertex).size();
      int vertexScore = outDegree - inDegree;
      if (bestScore == null || vertexScore > bestScore) {
        bestScore = vertexScore;
        bestVertex = vertex;
      }
    }
    return bestVertex;
  }

  /** @return Set<InformativeEdge> */
  public Set<InformativeEdge> getFeedbackArcSet() {
    // This will always be OK. The compiler just doesn't feel that way.
    @SuppressWarnings("unchecked")
    SimpleDirectedGraph<String, InformativeEdge> workingGraph =
        (SimpleDirectedGraph<String, InformativeEdge>) this.candidateGraph.clone();
    LinkedList<String> stringsOne = new LinkedList<>();
    LinkedList<String> stringsTwo = new LinkedList<>();

    while (!workingGraph.vertexSet().isEmpty()) {
      List<String> listOfSinks;
      while (!(listOfSinks = this.getSinks(workingGraph)).isEmpty()) {
        for (String sink : listOfSinks) {
          stringsTwo.addFirst(sink);
          workingGraph.removeVertex(sink);
          //          System.out.println("Removed sink: " + sink);
        }
      }
      List<String> listOfSources;
      while (!(listOfSources = this.getSources(workingGraph)).isEmpty()) {
        for (String source : listOfSources) {
          stringsOne.addLast(source);
          workingGraph.removeVertex(source);
          //          System.out.println("Removed source: " + source);
        }
      }
      if (!workingGraph.vertexSet().isEmpty()) {
        String vertexToRemove = this.getHighestOutDegreeInDegreeRatio(workingGraph);
        workingGraph.removeVertex(vertexToRemove);
        stringsOne.addLast(vertexToRemove);
        //        System.out.println("Removed calculated Vertex: " + vertexToRemove);
      }
    }
    LinkedList<String> stringsFinal = new LinkedList<>();
    stringsFinal.addAll(stringsOne);
    stringsFinal.addAll(stringsTwo);
    //    System.out.println("Final List: " + stringsFinal);

    List<String> visitedList = new ArrayList<>();
    Set<InformativeEdge> listOfEdgesToRemove = new HashSet<>();
    for (String vertex : stringsFinal) {
      //      System.out.println("Checking: " + vertex);
      List<String> targetList = Graphs.successorListOf(this.candidateGraph, vertex);
      if (!Collections.disjoint(visitedList, targetList)) {
        //        System.out.println("Found arc back");
        for (String target : targetList) {
          if (visitedList.contains(target)) {
            //            System.out.println("Found edge " + vertex + " " + target);
            listOfEdgesToRemove.add(this.candidateGraph.getEdge(vertex, target));
          }
        }
      }
      visitedList.add(vertex);
    }
    return listOfEdgesToRemove;
  }
}
