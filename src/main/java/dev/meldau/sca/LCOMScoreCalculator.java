package dev.meldau.sca;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.parse.Parser;
import org.jgrapht.Graph;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.jgrapht.nio.dot.DOTExporter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;

import java.io.*;
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
 * Implements LCOM Score Calculation (Lack Of Cohesion in Methods) as proposed by Hitz and Montazeri
 * in 1995
 * https://www.researchgate.net/profile/Martin-Hitz/publication/2765140_Measuring_Product_Attributes_of_Object-Oriented_Systems/links/0912f51091f5fa52aa000000/Measuring-Product-Attributes-of-Object-Oriented-Systems.pdf
 *
 * @author Ingo Meldau
 */
@SuppressFBWarnings("DM_DEFAULT_ENCODING")
public class LCOMScoreCalculator {

  final ArrayList<File> CLASS_FILES;
  Map<String, Integer> LCOMScores;
  HashMap<String, Graph<String, DefaultEdge>> LCOMGraph;

  public LCOMScoreCalculator(ArrayList<File> classFiles) throws IOException {
    this.CLASS_FILES = classFiles;
    calculateScores();
  }

  public Map<String, Integer> getLCOMScores() {
    return LCOMScores;
  }

  /** Calculate LCOM Scores in all CLASS_FILES and write them to LCOMScores */
  private void calculateScores() throws IOException {

    // Create Hashmap to return
    LCOMScores = new HashMap<>();
    LCOMGraph = new HashMap<>();

    for (File classFile : CLASS_FILES) {

      // Maps for which method uses which fields and which method calls which other methods
      Map<String, List<String>> methodUsesMap = new HashMap<>();
      Map<String, List<String>> methodCallsMap = new HashMap<>();

      /*
       Get Class Object of Main Class in ASM
       TODO: What about more than one Class in a classfile?!
      */
      ClassNode myClassNode;
      ClassReader myClassReader;
      try (InputStream classFileIS = new FileInputStream(classFile)) {
        myClassNode = new ClassNode();

        myClassReader = new ClassReader(classFileIS);
      }
      myClassReader.accept(myClassNode, 0);

      //            System.out.println("LCOM Calculation for Class " + myClassNode.name );
      // Initialize jGraphT Graph to Calculate LCOM on
      LCOMGraph.put(cleanInternalName(myClassNode.name), new SimpleGraph<>(DefaultEdge.class));

      // Iterate over Methods in Class
      for (MethodNode method : myClassNode.methods) {
        //               System.out.println("Looking at " + cleanInternalName(method.name) + " in "
        // +
        // myClassNode.name);
        // Add vertex for method
        LCOMGraph.get(cleanInternalName(myClassNode.name))
            .addVertex(cleanInternalName(method.name));
        // List of fields used in method
        List<String> myMethodFields = new ArrayList<>();
        // List of methods  called in Method
        List<String> myMethodCalls = new ArrayList<>();
        // Iterate over instructions
        for (AbstractInsnNode ain : method.instructions.toArray()) {
          /*
           If ain is instance variable, write to list of fields used Else If it is a method call,
           write to list of methods called
          */
          if (ain.getType() == AbstractInsnNode.FIELD_INSN) {
            FieldInsnNode fin = (FieldInsnNode) ain;
            //                        System.out.println("Registering field " + fin.name + " for
            // method " + cleanInternalName(method.name));
            myMethodFields.add(fin.name);

          } else if (ain.getType() == AbstractInsnNode.METHOD_INSN) {
            MethodInsnNode methCall = (MethodInsnNode) ain;
            if (methCall.owner.equals(cleanInternalName(myClassNode.name))) { // CHECK!
              myMethodCalls.add(methCall.name);
            }
          }
        }
        // Add Lists to HashMap for all methods
        methodUsesMap.put(cleanInternalName(method.name), myMethodFields);
        methodCallsMap.put(cleanInternalName(method.name), myMethodCalls);
      }
      // Iterate over found methods and add edges to LCOM graph using mutual used fields
      for (String method : LCOMGraph.get(cleanInternalName(myClassNode.name)).vertexSet()) {
        for (String usedField : methodUsesMap.get(method)) {
          for (String secondMethod :
              LCOMGraph.get(cleanInternalName(myClassNode.name)).vertexSet()) {
            if (!method.equals(secondMethod)) {
              // System.out.println("Looking for Edges from " + method + " to " + secondMethod + "
              // for field " + usedField);
              if (!methodUsesMap.get(secondMethod).isEmpty()
                  && methodUsesMap.get(secondMethod).contains(usedField)) {
                // System.out.println("Adding edge from " + method + "
                // to " + secondMethod);
                LCOMGraph.get(cleanInternalName(myClassNode.name)).addEdge(method, secondMethod);
              }
            }
          }
        }
      }
      // Iterate over found methods and add edges to LCOM graph using calls
      for (String method : LCOMGraph.get(cleanInternalName(myClassNode.name)).vertexSet()) {
        for (String secondMethod : methodCallsMap.get(method)) {
          if (!secondMethod.equals(method)
              && LCOMGraph.get(cleanInternalName(myClassNode.name))
                  .vertexSet()
                  .contains(secondMethod)) {
            LCOMGraph.get(cleanInternalName(myClassNode.name)).addEdge(method, secondMethod);
          }
        }
      }

      // Create ConnectivityInspector to find connected components of graph
      ConnectivityInspector<String, DefaultEdge> connectedComponentIns =
          new ConnectivityInspector<>(LCOMGraph.get(cleanInternalName(myClassNode.name)));
      List<Set<String>> connectedComponents = connectedComponentIns.connectedSets();

      LCOMScores.put(cleanInternalName(myClassNode.name), connectedComponents.size());
    }

    // TODO:    saveResultJSON(myLCOMScores);
  }

  /** Save LCOM graph as DOT and PNG */
  public void saveGraph(File targetDir) throws IOException {
    DOTExporter<String, DefaultEdge> dotExporter = new DOTExporter<>(v -> v.replace("/", "_"));
    for (HashMap.Entry<String, Graph<String, DefaultEdge>> graphEntry : LCOMGraph.entrySet()) {
      dotExporter.exportGraph(
          graphEntry.getValue(),
          new FileWriter(
              targetDir.getAbsolutePath()
                  + "/"
                  + graphEntry.getKey().replace("/", "_")
                  + "_lcom_graph.dot"));
      try (InputStream dot =
          new FileInputStream(
              targetDir.getAbsolutePath()
                  + "/"
                  + graphEntry.getKey().replace("/", "_")
                  + "_lcom_graph.dot")) {
        MutableGraph g = new Parser().read(dot);
        Graphviz.fromGraph(g)
            .render(Format.PNG)
            .toFile(
                new File(
                    targetDir.getAbsolutePath()
                        + "/"
                        + graphEntry.getKey().replace("/", "_")
                        + "_lcom_graph.png"));
      }
    }
  }

  private String cleanInternalName(String toClean) {
    String cleanString = toClean.replaceAll("^\\[*[A-Z]", "");
    cleanString = cleanString.replaceAll("\\$.*", "");
    cleanString = cleanString.replaceAll(";$", "");
    return cleanString;
  }
}
