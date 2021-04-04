package dev.meldau.sca;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FeedbackArcSetFinderTest {

  SimpleDirectedGraph<String, DefaultEdge> simpleTestGraph;
  SimpleDirectedGraph<String, DefaultEdge> loopTestGraph;

  FeedbackArcSetFinder feedbackArcSetFinderSimple;
  FeedbackArcSetFinder feedbackArcSetFinderLoop;

  @BeforeEach
  void setUp() {

    /* Create Simple Graph to Check sinks and sources
     *                src1       src2
     *                /  \       /
     *               V   V      V
     *     middlepart1   middlepart2
     *        /     \         |
     *       V      V         V
     *    sink1    sink2     sink3
     */
    simpleTestGraph = new SimpleDirectedGraph<>(DefaultEdge.class);
    simpleTestGraph.addVertex("src1");
    simpleTestGraph.addVertex("src2");
    simpleTestGraph.addVertex("middlepart1");
    simpleTestGraph.addVertex("middlepart2");
    simpleTestGraph.addVertex("sink1");
    simpleTestGraph.addVertex("sink2");
    simpleTestGraph.addVertex("sink3");
    simpleTestGraph.addEdge("src1", "middlepart1");
    simpleTestGraph.addEdge("src1", "middlepart2");
    simpleTestGraph.addEdge("src2", "middlepart2");
    simpleTestGraph.addEdge("middlepart1", "sink1");
    simpleTestGraph.addEdge("middlepart1", "sink2");
    simpleTestGraph.addEdge("middlepart2", "sink3");

    feedbackArcSetFinderSimple = new FeedbackArcSetFinder(simpleTestGraph);

    /* Create Loop Graph to check if the correct edge (A2 => A3) to remove is identified
     *
     *      loopA1 -> loopA2 <- loopB1
     *        ^          |         ^
     *        |          V         |
     *      loopA4 <- loopA3 -> loopB2
     *
     */

    loopTestGraph = new SimpleDirectedGraph<>(DefaultEdge.class);
    loopTestGraph.addVertex("loopA1");
    loopTestGraph.addVertex("loopA2");
    loopTestGraph.addVertex("loopA3");
    loopTestGraph.addVertex("loopA4");
    loopTestGraph.addVertex("loopB1");
    loopTestGraph.addVertex("loopB2");
    loopTestGraph.addEdge("loopA1", "loopA2");
    loopTestGraph.addEdge("loopA2", "loopA3");
    loopTestGraph.addEdge("loopA3", "loopA4");
    loopTestGraph.addEdge("loopA3", "loopB2");
    loopTestGraph.addEdge("loopA4", "loopA1");
    loopTestGraph.addEdge("loopB2", "loopB1");
    loopTestGraph.addEdge("loopB1", "loopA2");

    feedbackArcSetFinderLoop = new FeedbackArcSetFinder(loopTestGraph);
  }

  @AfterEach
  void tearDown() {}

  @Test
  void getSinks() {
    List<String> sinks = new ArrayList<>();
    sinks.add("sink1");
    sinks.add("sink2");
    sinks.add("sink3");
    assertEquals(sinks, feedbackArcSetFinderSimple.getSinks(simpleTestGraph));
  }

  @Test
  void getSources() {
    List<String> sources = new ArrayList<>();
    sources.add("src1");
    sources.add("src2");
    assertEquals(sources, feedbackArcSetFinderSimple.getSources(simpleTestGraph));
  }

  @Test
  void getHighestOutDegreeInDegreeRatio() {
    assertEquals("loopA3", feedbackArcSetFinderLoop.getHighestOutDegreeInDegreeRatio(loopTestGraph));
  }

  @Test
  void getFeedbackArcSet() {
    Set<DefaultEdge> feedbackArcSet = new HashSet<>();
    feedbackArcSet.add(loopTestGraph.getEdge("loopA2", "loopA3"));
    assertEquals(
        feedbackArcSet, feedbackArcSetFinderLoop.getFeedbackArcSet());
  }
}
