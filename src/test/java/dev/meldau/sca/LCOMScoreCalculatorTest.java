package dev.meldau.sca;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

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

class LCOMScoreCalculatorTest {

  LCOMScoreCalculator lcomScoreCalculator;

  @BeforeEach
  void setUp() throws IOException {
    ClassFileFinder classFileFinder =
        new ClassFileFinder(new File("src/test/resources/ClassesWithCycle/"));
    lcomScoreCalculator = new LCOMScoreCalculator(classFileFinder.getClassFiles());
  }

  @Test
  void getScores() {
     final Map<String, Integer> LCOMScores = lcomScoreCalculator.getLCOMScores();
      assertEquals(2, (int) LCOMScores.get("dev/meldau/myjavamvntest/App"));
      assertEquals(2, (int) LCOMScores.get("dev/meldau/myjavamvntest/SuperThing"));
  }


}
