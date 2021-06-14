package dev.meldau.sca;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LCOMScoreCalculatorTest {

  LCOMScoreCalculator lcomScoreCalculator;

  @BeforeEach
  void setUp() throws IOException {
    ClassFileFinder classFileFinder =
        new ClassFileFinder(new File("src/test/resources/ClassesWithCycle/"));
    lcomScoreCalculator = new LCOMScoreCalculator(classFileFinder.getClassFiles());
  }

  @Test
  void getScores() throws IOException {
     final Map<String, Integer> LCOMScores = lcomScoreCalculator.getLCOMScores();
      assertEquals(2, (int) LCOMScores.get("dev/meldau/myjavamvntest/App"));
      assertEquals(2, (int) LCOMScores.get("dev/meldau/myjavamvntest/SuperThing"));
  }


}
