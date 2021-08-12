package dev.meldau.sca;

import org.jgrapht.graph.DirectedMultigraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
      if (labeledEdge.getSource().equals("dev/meldau/myjavamvntest/App")
          && labeledEdge.getTarget().equals("dev/meldau/myjavamvntest/SuperThing")
          && labeledEdge.getConnectionType() == ConnectionType.LOCAL_VARIABLE) {
        edgeCounter++;
      }
    }
    assertEquals(3, edgeCounter);
  }
}
