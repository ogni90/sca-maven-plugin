package dev.meldau.sca;

import org.apache.commons.collections4.CollectionUtils;
import org.jgrapht.graph.DirectedMultigraph;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.io.*;
import java.util.ArrayList;

public class CouplingMultiGraphGenerator {

  ArrayList<File> classFiles;

  public CouplingMultiGraphGenerator(File classDir) throws FileNotFoundException {

    ClassFileFinder classFileFinder = new ClassFileFinder(classDir);
    classFiles = classFileFinder.getClassFiles();
  }

  private String cleanInternalName(String toClean) {
    String cleanString = toClean.replaceAll("^\\[*[A-Z]", "");
    cleanString = cleanString.replaceAll("$.*", "");
    cleanString = cleanString.replaceAll(";$", "");
    return cleanString;
  }

  public DirectedMultigraph<String, LabeledEdge> getGraph() throws IOException {

    DirectedMultigraph<String, LabeledEdge> couplingGraph =
        new DirectedMultigraph<>(LabeledEdge.class);

    for (File classFile : classFiles) {
      InputStream classFileIS = new FileInputStream(classFile);
      ClassNode myClassNode = new ClassNode();
      ClassReader myClassReader = new ClassReader(classFileIS);
      myClassReader.accept(myClassNode, 0);

      // Check if classfile is a module-info file in this case => skip
      if (myClassReader.getClassName().equals("module-info")
          || myClassReader.getClassName().equals("")) {
        continue;
      }

      // Add classname as vertex to graph
      if (!couplingGraph.vertexSet().contains(myClassReader.getClassName())) {
        couplingGraph.addVertex(myClassReader.getClassName());
      }

      // myLog.debug("Looking at Class: " + myClassNode.name);

      // Check for superclass
      // myLog.debug("SuperClass: " + myClassNode.superName);
      if (myClassNode.superName != null
          && !myClassNode.superName.equals("java/lang/Object")
          && !myClassNode.superName.equals("")) {
        if (!couplingGraph.vertexSet().contains(myClassNode.superName)) {
          couplingGraph.addVertex(myClassNode.superName);
        }
        couplingGraph.addEdge(
            cleanInternalName(myClassNode.name),
            cleanInternalName(myClassNode.superName),
            new LabeledEdge(LabeledEdge.ConnectionType.SUPERCLASS));
      }

      // check for instance variables
      for (FieldNode field : CollectionUtils.emptyIfNull(myClassNode.fields)) {
        // myLog.debug("Field Name: " + field.name);
        // myLog.debug("Field Type: " + cleanInternalName(field.desc));
        if (!cleanInternalName(field.desc).startsWith("java/")
            && !myClassNode.name.equals(cleanInternalName(field.desc))) {
          if (!couplingGraph.vertexSet().contains(cleanInternalName(field.desc))) {
            couplingGraph.addVertex(cleanInternalName(field.desc));
          }
          couplingGraph.addEdge(
              myClassNode.name,
              cleanInternalName(field.desc),
              new LabeledEdge(LabeledEdge.ConnectionType.INSTANCE_VARIABLE));
        }
      }

      // check for method calls
      for (MethodNode method : CollectionUtils.emptyIfNull(myClassNode.methods)) {
        // myLog.debug("Looking at " + method.name + " in " + myClassReader.getClassName());
        for (AbstractInsnNode ain : method.instructions.toArray()) {
          if (ain.getType() == AbstractInsnNode.METHOD_INSN) {
            MethodInsnNode methCall = (MethodInsnNode) ain;
            if (!cleanInternalName(methCall.owner).equals(myClassNode.name)
                && !methCall.owner.startsWith("java/")) {
              /*
              myLog.debug(
                      "Found call from "
                              + method.name
                              + " to "
                              + methCall.owner
                              + " -> "
                              + methCall.name);
               */
              if (!couplingGraph.vertexSet().contains(methCall.owner)) {
                couplingGraph.addVertex(methCall.owner);
              }
              couplingGraph.addEdge(
                  myClassNode.name,
                  cleanInternalName(methCall.owner),
                  new LabeledEdge(LabeledEdge.ConnectionType.CALLS_METHOD));
            }
          }

          // Check for use of public variables
          if (ain.getType() == AbstractInsnNode.FIELD_INSN) {
            FieldInsnNode fieldInsnNode = (FieldInsnNode) ain;
            if (!cleanInternalName(fieldInsnNode.owner).equals(myClassNode.name)
                && !fieldInsnNode.owner.startsWith("java/")) {
              /*
              myLog.debug(
                      "Found use of public Variable of other Class in "
                              + method.name
                              + " Class "
                              + fieldInsnNode.owner
                              + " name "
                              + fieldInsnNode.name);
              */
              if (!couplingGraph.vertexSet().contains(fieldInsnNode.owner)) {
                couplingGraph.addVertex(fieldInsnNode.owner);
              }
              couplingGraph.addEdge(
                  cleanInternalName(myClassNode.name),
                  cleanInternalName(fieldInsnNode.owner),
                  new LabeledEdge(LabeledEdge.ConnectionType.ACCESS_PUBLIC_VARIABLE));
            }
          }
        }
        // check for local Variables
        for (LocalVariableNode myLocalVariableNode : CollectionUtils.emptyIfNull(method.localVariables)) {
          // myLog.debug("Local Variable Name: " + myLocalVariableNode.name);
          // myLog.debug("Local Variable Type: " + cleanInternalName(myLocalVariableNode.desc));
          if (!cleanInternalName(myLocalVariableNode.desc).startsWith("java/")
              && !cleanInternalName(myLocalVariableNode.desc).equals(myClassNode.name)) {
            if (!couplingGraph.vertexSet().contains(cleanInternalName(myLocalVariableNode.desc))) {
              couplingGraph.addVertex(cleanInternalName(myLocalVariableNode.desc));
            }
            couplingGraph.addEdge(
                myClassNode.name,
                cleanInternalName(myLocalVariableNode.desc),
                new LabeledEdge(LabeledEdge.ConnectionType.LOCAL_VARIABLE));
          }
        }

        // check for method Parameter Types
        Type[] parameterTypes = Type.getArgumentTypes(method.desc);
        for (Type parameterType : parameterTypes) {
          // myLog.debug(
          //        "Parameter internalName: " +
          // cleanInternalName(parameterType.getInternalName()));

          if (!cleanInternalName(parameterType.getInternalName()).equals(myClassNode.name)
              && !cleanInternalName(parameterType.getInternalName()).startsWith("java/")) {
            if (!couplingGraph
                .vertexSet()
                .contains(cleanInternalName(parameterType.getInternalName()))) {
              couplingGraph.addVertex(cleanInternalName(parameterType.getInternalName()));
            }
            couplingGraph.addEdge(
                myClassNode.name,
                cleanInternalName(parameterType.getInternalName()),
                new LabeledEdge(LabeledEdge.ConnectionType.PARAMETER_TYPE));
          }
        }
      }
    }

    return couplingGraph;
  }
}
