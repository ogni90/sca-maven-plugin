package dev.meldau.sca;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class JdepsGraphCreatorTest {

  JdepsGraphCreator jdepsGraphCreatorWithLoop;
  JdepsGraphCreator jdepsGraphCreatorWithoutLoop;
  File classesWithCycle;
  File classesWithoutCycle;

  @BeforeEach
  void setUp() {
    classesWithCycle = new File("src/test/resources/ClassesWithCycle");
    jdepsGraphCreatorWithLoop = new JdepsGraphCreator(classesWithCycle, classesWithCycle);
    classesWithoutCycle = new File("src/test/resources/ClassesWithoutCycle");
    jdepsGraphCreatorWithoutLoop = new JdepsGraphCreator(classesWithoutCycle, classesWithoutCycle);
  }

  @SuppressFBWarnings({"NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", "RV_RETURN_VALUE_IGNORED_BAD_PRACTICE"})
  @SuppressWarnings("ResultOfMethodCallIgnored")
  @AfterEach
  void tearDown() {
    for (File candidate : Objects.requireNonNull(classesWithCycle.listFiles())) {
      if (candidate.getName().endsWith(".dot")) {
        candidate.delete();
      }
    }
    for (File candidate : Objects.requireNonNull(classesWithoutCycle.listFiles())) {
      if (candidate.getName().endsWith(".dot")) {
        candidate.delete();
      }
    }
  }

  @Test
  void getCycleGraph() {
    SimpleDirectedGraph<String, InformativeEdge> loopGraph = jdepsGraphCreatorWithLoop.getCycleGraph();
    assertTrue(
        loopGraph.containsEdge(
            "dev.meldau.myjavamvntest.SuperThing", "dev.meldau.myjavamvntest.App"));
    assertTrue(
        loopGraph.containsEdge(
            "dev.meldau.myjavamvntest.App", "dev.meldau.myjavamvntest.SuperThing"));
    SimpleDirectedGraph<String, InformativeEdge> looplessGraph =
        jdepsGraphCreatorWithoutLoop.getCycleGraph();
    assertTrue(
        looplessGraph.containsEdge(
            "dev.meldau.myjavamvntest.App", "dev.meldau.myjavamvntest.SuperThing"));
    assertFalse(
        looplessGraph.containsEdge(
            "dev.meldau.myjavamvntest.SuperThing", "dev.meldau.myjavamvntest.App"));
  }

  @Test
  void hasCycles() {
    assertTrue(jdepsGraphCreatorWithLoop.hasCycles());
    assertFalse(jdepsGraphCreatorWithoutLoop.hasCycles());
  }

  @Test
  void createDotFiles() {
    File dotFile = new File("src/test/resources/ClassesWithCycle/ClassesWithCycle.dot");
    File dotFile2 = new File("src/test/resources/ClassesWithoutCycle/ClassesWithoutCycle.dot");
    assertTrue(dotFile.exists());
    assertTrue(dotFile2.exists());
  }

  @Test
  void createGraph() {
    assertNotNull (jdepsGraphCreatorWithLoop.getCycleGraph());
    assertNotNull (jdepsGraphCreatorWithoutLoop.getCycleGraph());
  }
}