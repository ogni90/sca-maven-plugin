package dev.meldau.sca;

import org.jgrapht.graph.DirectedMultigraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CouplingMultiGraphGeneratorTest {

  File classesWithCycle;

  @BeforeEach
  void setUp() {
    classesWithCycle = new File("src/test/resources/ClassesWithoutCycle/");
  }

  @Test
  void getGraph() throws IOException {

    CouplingMultiGraphGenerator couplingMultiGraphGenerator =
        new CouplingMultiGraphGenerator(classesWithCycle);
    DirectedMultigraph<String, LabeledEdge> couplingMultiGraph =
        couplingMultiGraphGenerator.getGraph();

    Set<LabeledEdge> labeledEdges = couplingMultiGraph.edgeSet();
    int edgeCounter = 0;
    for (LabeledEdge labeledEdge : labeledEdges) {
      System.out.println(labeledEdge);
      if (labeledEdge.getSource().equals("dev/meldau/myjavamvntest/App")
          && labeledEdge.getTarget().equals("dev/meldau/myjavamvntest/SuperThing")
          && labeledEdge.getConnectionType() == LabeledEdge.ConnectionType.LOCAL_VARIABLE) {
        edgeCounter++;
      }
    }
    assertEquals(3, edgeCounter);
  }
}
