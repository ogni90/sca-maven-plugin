package dev.meldau.sca;

import org.apache.maven.plugin.MojoExecutionException;
import org.jgrapht.Graph;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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

public class LCOMScoreCalculator {

    ArrayList<File> classFiles;

    public LCOMScoreCalculator(ArrayList<File> classFiles) {
        this.classFiles = classFiles;
    }

    Map<String, Integer> getScores()
            throws IOException {

        // Create Hashmap to return
        Map<String, Integer> myLCOMScores = new HashMap<>();

        for (File classFile : classFiles) {

            // Initialize jGraphT Graph to Calculate LCOM on
            Graph<String, DefaultEdge> myLCOMGraph = new SimpleGraph<>(DefaultEdge.class);

            // Maps for which method uses which fields and which method calls which other methods
            Map<String, List<String>> methodUsesMap = new HashMap<>();
            Map<String, List<String>> methodCallsMap = new HashMap<>();

      /*
       Get Class Object of Main Class in ASM
       TODO: What about more than one Class in a classfile?!
      */
            InputStream classFileIS = new FileInputStream(classFile);
            ClassNode myClassNode = new ClassNode();


            ClassReader myClassReader = new ClassReader(classFileIS);
            myClassReader.accept(myClassNode, 0);


//            System.out.println("LCOM Calculation for Class " + myClassNode.name );

            // Iterate over Methods in Class
            for (MethodNode method : myClassNode.methods) {
//               System.out.println("Looking at " + method.name + " in " + myClassNode.name);
                // Add vertex for method
                myLCOMGraph.addVertex(method.name);
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
//                        System.out.println("Registering field " + fin.name + " for method " + method.name);
                        myMethodFields.add(fin.name);

                    } else if (ain.getType() == AbstractInsnNode.METHOD_INSN) {
                        MethodInsnNode methCall = (MethodInsnNode) ain;
                        if (methCall.owner.equals(myClassNode.name)) { // CHECK!
                            myMethodCalls.add(methCall.name);
                        }
                    }
                }
                // Add Lists to HashMap for all methods
                methodUsesMap.put(method.name, myMethodFields);
                methodCallsMap.put(method.name, myMethodCalls);
            }
            // Iterate over found methods find edges for graph using mutual used fields
            for (String method : myLCOMGraph.vertexSet()) {
                for (String usedField : methodUsesMap.get(method)) {
                    for (String secondMethod : myLCOMGraph.vertexSet()) {
                        if (!method.equals(secondMethod)) {

/*
                            System.out.println(
                                    "Looking for Edges from "
                                            + method
                                            + " to "
                                            + secondMethod
                                            + " for field "
                                            + usedField);
*/

                            if (!methodUsesMap.get(secondMethod).isEmpty()
                                    && methodUsesMap.get(secondMethod).contains(usedField)) {
//                              System.out.println("Adding edge from " + method + " to " + secondMethod);
                                myLCOMGraph.addEdge(method, secondMethod);
                            }
                        }
                    }
                }
            }
            // Iterate over found methods find edges for graph using calls
            for (String method : myLCOMGraph.vertexSet()) {
                for (String secondMethod : methodCallsMap.get(method)) {
                    if (!secondMethod.equals(method) && myLCOMGraph.vertexSet().contains(secondMethod)) {
                        myLCOMGraph.addEdge(method, secondMethod);
                    }
                }
            }

            // Create ConnectivityInspector to find connected components of graph
            ConnectivityInspector<String, DefaultEdge> connectedComponentIns =
                    new ConnectivityInspector<>(myLCOMGraph);
            List<Set<String>> connectedComponents = connectedComponentIns.connectedSets();

// TODO:       saveResultImage(myLCOMGraph, myClassReader.getClassName().replace("/", "_"));

            myLCOMScores.put(myClassNode.name, connectedComponents.size());
        }

// TODO:    saveResultJSON(myLCOMScores);

        return myLCOMScores;
    }
}
