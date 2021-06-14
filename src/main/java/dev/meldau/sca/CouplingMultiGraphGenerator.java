package dev.meldau.sca;

import guru.nidi.graphviz.attribute.Color;
import guru.nidi.graphviz.attribute.Label;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.parse.Parser;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.maven.plugin.logging.Log;
import org.jgrapht.graph.DirectedMultigraph;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.DefaultAttribute;
import org.jgrapht.nio.dot.DOTExporter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class CouplingMultiGraphGenerator {

  ArrayList<File> classFiles;

  DirectedMultigraph<String, LabeledEdge> couplingGraph;

  Log myLog;

  public CouplingMultiGraphGenerator(File classDir, Log myLog) throws IOException {
    this.myLog = myLog;
    ClassFileFinder classFileFinder = new ClassFileFinder(classDir);
    classFiles = classFileFinder.getClassFiles();
    generateGraph();
  }

  public CouplingMultiGraphGenerator(File classDir) throws IOException {

    ClassFileFinder classFileFinder = new ClassFileFinder(classDir);
    classFiles = classFileFinder.getClassFiles();
    generateGraph();
  }

  private void generateGraph() throws IOException {

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
        couplingGraph.addVertex(cleanInternalName(myClassNode.name));
      }

      if (myLog != null) {
        myLog.debug("Looking at Class: " + myClassNode.name);
      }

      // Check for superclass
      if (myLog != null) {
        myLog.debug("SuperClass: " + myClassNode.superName);
      }
      if (myClassNode.superName != null
          && !myClassNode.superName.equals("java/lang/Object")
          && !myClassNode.superName.equals("")) {
        if (!couplingGraph.vertexSet().contains(cleanInternalName(myClassNode.superName))) {
          couplingGraph.addVertex(cleanInternalName(myClassNode.superName));
        }
        couplingGraph.addEdge(
            cleanInternalName(myClassNode.name),
            cleanInternalName(myClassNode.superName),
            new LabeledEdge(LabeledEdge.ConnectionType.SUPERCLASS));
      }

      // check for instance variables
      for (FieldNode field : CollectionUtils.emptyIfNull(myClassNode.fields)) {
        if (myLog != null) {
          myLog.debug("Field Name: " + field.name);
          myLog.debug("Field Type: " + field.desc + "(" + cleanInternalName(field.desc) + ")");
        }
        if (!cleanInternalName(field.desc).startsWith("java/")
            && !cleanInternalName(field.desc).equals("")
            && !cleanInternalName(myClassNode.name).equals(cleanInternalName(field.desc))) {
          if (!couplingGraph.vertexSet().contains(cleanInternalName(field.desc))) {
            couplingGraph.addVertex(cleanInternalName(field.desc));
          }
          couplingGraph.addEdge(
              cleanInternalName(myClassNode.name),
              cleanInternalName(field.desc),
              new LabeledEdge(LabeledEdge.ConnectionType.INSTANCE_VARIABLE));
        }
      }

      // check for method calls
      for (MethodNode method : CollectionUtils.emptyIfNull(myClassNode.methods)) {
        if (myLog != null) {
          myLog.debug("Looking at " + method.name + " in " + myClassReader.getClassName());
        }
        for (AbstractInsnNode ain : method.instructions.toArray()) {
          if (ain.getType() == AbstractInsnNode.METHOD_INSN) {
            MethodInsnNode methCall = (MethodInsnNode) ain;
            if (!cleanInternalName(methCall.owner).equals(cleanInternalName(myClassNode.name))
                && !cleanInternalName(methCall.owner).equals("")
                && !methCall.owner.startsWith("java/")) {
              if (myLog != null) {
                myLog.debug(
                    "Found call from "
                        + method.name
                        + " to "
                        + methCall.owner
                        + " -> "
                        + methCall.name);
              }
              if (!couplingGraph.vertexSet().contains(cleanInternalName(methCall.owner))) {
                couplingGraph.addVertex(cleanInternalName(methCall.owner));
              }
              couplingGraph.addEdge(
                  cleanInternalName(myClassNode.name),
                  cleanInternalName(methCall.owner),
                  new LabeledEdge(LabeledEdge.ConnectionType.CALLS_METHOD));
            }
          }

          // Check for use of public variables
          if (ain.getType() == AbstractInsnNode.FIELD_INSN) {
            FieldInsnNode fieldInsnNode = (FieldInsnNode) ain;
            if (!cleanInternalName(fieldInsnNode.owner).equals(cleanInternalName(myClassNode.name))
                && !cleanInternalName(fieldInsnNode.owner).equals("")
                && !fieldInsnNode.owner.startsWith("java/")) {
              if (myLog != null) {
                myLog.debug(
                    "Found use of public Variable of other Class in "
                        + method.name
                        + " Class "
                        + fieldInsnNode.owner
                        + " name "
                        + fieldInsnNode.name);
              }
              if (!couplingGraph.vertexSet().contains(cleanInternalName(fieldInsnNode.owner))) {
                couplingGraph.addVertex(cleanInternalName(fieldInsnNode.owner));
              }
              couplingGraph.addEdge(
                  cleanInternalName(myClassNode.name),
                  cleanInternalName(fieldInsnNode.owner),
                  new LabeledEdge(LabeledEdge.ConnectionType.ACCESS_PUBLIC_VARIABLE));
            }
          }
        }
        // check for local Variables
        for (LocalVariableNode myLocalVariableNode :
            CollectionUtils.emptyIfNull(method.localVariables)) {
          if (myLog != null) {
            myLog.debug("Local Variable Name: " + myLocalVariableNode.name);
            myLog.debug("Local Variable Type: " + cleanInternalName(myLocalVariableNode.desc));
          }
          if (!cleanInternalName(myLocalVariableNode.desc).startsWith("java/")
              && !cleanInternalName(myLocalVariableNode.desc).equals("")
              && !cleanInternalName(myLocalVariableNode.desc)
                  .equals(cleanInternalName(myClassNode.name))) {
            if (!couplingGraph.vertexSet().contains(cleanInternalName(myLocalVariableNode.desc))) {
              couplingGraph.addVertex(cleanInternalName(myLocalVariableNode.desc));
            }
            couplingGraph.addEdge(
                cleanInternalName(myClassNode.name),
                cleanInternalName(myLocalVariableNode.desc),
                new LabeledEdge(LabeledEdge.ConnectionType.LOCAL_VARIABLE));
          }
        }

        // check for method Parameter Types
        Type[] parameterTypes = Type.getArgumentTypes(method.desc);
        for (Type parameterType : parameterTypes) {
          if (myLog != null) {
            myLog.debug(
                "Parameter internalName: " + cleanInternalName(parameterType.getInternalName()));
          }

          if (!cleanInternalName(parameterType.getInternalName())
                  .equals(cleanInternalName(myClassNode.name))
              && !cleanInternalName(parameterType.getInternalName()).equals("")
              && !cleanInternalName(parameterType.getInternalName()).startsWith("java/")) {
            if (!couplingGraph
                .vertexSet()
                .contains(cleanInternalName(parameterType.getInternalName()))) {
              couplingGraph.addVertex(cleanInternalName(parameterType.getInternalName()));
            }
            couplingGraph.addEdge(
                cleanInternalName(myClassNode.name),
                cleanInternalName(parameterType.getInternalName()),
                new LabeledEdge(LabeledEdge.ConnectionType.PARAMETER_TYPE));
          }
        }
      }
    }

    this.couplingGraph = couplingGraph;
  }

  private String cleanInternalName(String toClean) {
    String cleanString = toClean.replaceAll("^\\[*[A-Z]", "");
    cleanString = cleanString.replaceAll("\\$", "_");
    cleanString = cleanString.replaceAll(";$", "");
    return cleanString;
  }

  public void saveGraph(File targetDir) throws IOException {
    DOTExporter<String, LabeledEdge> dotExporter = new DOTExporter<>(v -> v.replace("/", "_"));
    dotExporter.setEdgeAttributeProvider(
        labeledEdge -> {
          Map<String, Attribute> map = new LinkedHashMap<>();
          map.put("label", DefaultAttribute.createAttribute("" + labeledEdge.getConnectionType()));
          return map;
        });
    dotExporter.exportGraph(
        couplingGraph, new FileWriter(targetDir.getAbsolutePath() + "/coupling_graph.dot"));

    try (InputStream dot =
        new FileInputStream(targetDir.getAbsolutePath() + "/coupling_graph.dot")) {
      MutableGraph g = new Parser().read(dot);

      // "Why don't you use g.edges() from the library?" you might ask. Because it is not working
      // for multigraphs is why...

      // Color Edges by their Link Type
      g.nodes()
          .forEach(
              node ->
                  node.links()
                      .forEach(
                          link -> link.add(getLinkColor(link.attrs().get("label").toString()))));

      // Attempt to replace label with xlabel to avoid overlapping in labels - makes the output
      // worse
      // g.nodes().forEach(node -> node.links().forEach(link ->
      // link.add(Label.of(link.get("label").toString().toLowerCase()).external())));

      // Remove Label-Text for better viewabilty
      g.nodes().forEach(node -> node.links().forEach(link -> link.add(Label.of(""))));

      Graphviz.fromGraph(g)
          .render(Format.PNG)
          .toFile(new File(targetDir.getAbsolutePath() + "/coupling_graph.png"));
      Graphviz.fromGraph(g)
          .render(Format.DOT)
          .toFile(new File(targetDir.getAbsolutePath() + "/colored_coupling_graph.dot"));
    }
  }

  private Color getLinkColor(String connectionType) {
    switch (connectionType) {
      case "SUPERCLASS":
        return Color.VIOLET;
      case "INSTANCE_VARIABLE":
        return Color.CHOCOLATE;
      case "CALLS_METHOD":
        return Color.BLUE;
      case "LOCAL_VARIABLE":
        return Color.ORANGE;
      case "PARAMETER_TYPE":
        return Color.GREEN;
      case "ACCESS_PUBLIC_VARIABLE":
        return Color.RED;
      default:
        System.out.println(connectionType);
        return Color.BLACK;
    }
  }

  public DirectedMultigraph<String, LabeledEdge> getGraph() throws IOException {
    return couplingGraph;
  }
}
