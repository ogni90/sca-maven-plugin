# sca-maven-plugin

* [Cyclic Dependencies](#cyclic-dependencies)
* [Cohesion Metric (LCOM)](#cohesion-metric-lcom)
* [Coupling Metrics](#coupling-metrics)
  * [Coupling Between Objects(CBO)](#coupling-between-objectscbo)
  * [Pair Coupling Between Objects (Pair-CBO)](#pair-coupling-between-objects-pair-cbo)
* [Requirements](#requirements)
* [Installation](#installation)
* [Usage](#usage)
  * [Usage from command line](#usage-from-command-line)
  * [Usage by integration in pom.xml](#usage-by-integration-in-pomxml)
* [Configuration](#configuration)
* [Using the plugin Results](#using-the-plugin-results)
* [JavaDoc documentation](#javadoc-documentation)

This Maven plugin performs statical program analysis on Java projects.
(In German: "Statische Code-Analyse" - hence the name of the plugin).

It performs these major tasks:
* Finding cyclic dependencies in Java code and providing a nearly optimal set of dependencies
  to remove to break the cycles
* Calculating a metric for cohesion
* Calculating two metrics for coupling
* Providing maven site reports for those metrics

## Cyclic Dependencies
The plugin can find cyclic dependencies on class and package level in a Java project.
It accomplishes that task by creating a dependency graph using jdeps and finding strongly connected
components with more than one vertex.
If a cyclic dependency is found, the plugin will suggest a solution by using the Feedback Arc Set Algorithm
to determine a set of edges (dependencies) to remove.

## Cohesion Metric (LCOM)
The plugin calculates a per class score for *L*ack of *CO*hesion in *M*ethods.
This metric is calculated by finding connected sets of methods in classes. Methods are connected if
they share instance variables or call each other. The number of connected sets is the LCOM score.
The lower this number is, the better.

## Coupling Metrics
To be able to calculate coupling metrics, the Plugin builds a graph of all classes (vertices) and
connections between classes (edges) in the tested software.

### Coupling Between Objects(CBO)
To calculate the classic CBO Metric all classes reachable with one edge, starting from the rated class, are added up.
The lower this number is, the better.

### Pair Coupling Between Objects (Pair-CBO)
Since the CBO Metric has its shortcomings it is accompanied by the Pair-CBO metric.
This metric does not count connections from a single class, but counts the connections between
all pairs of Classes. This way it should be easier to find out which classes exactly are coupled too strongly.
Again, the lower this number ist, the better.

## Requirements
The following requirements must be met, to be able to use this plugin:
* Your project needs to be managed in maven (version 3 or higher)
* The computer running maven must have the Java Development Kit (JDK) installed

## Installation

To install the Plugin, download the latest release from the GitHub Releases Page.
To install the JAR-File to your local maven repository use this command:
```
mvn org.apache.maven.plugins:maven-install-plugin:2.5.2:install-file -Dfile=sca-maven-plugin-1.0-SNAPSHOT.jar
```

## Usage

Once installed the plugin can be used in different ways.

### Usage from command line

The goals provided by the plugin can be called directly from the command
line.

```
# Check for cyclic dependencies
mvn dev.meldau.sca:sca-maven-plugin:sca-cohesion
# Calculate cohesion metric
mvn dev.meldau.sca:sca-maven-plugin:sca-cohesion
# Calculate coupling metrics
mvn dev.meldau.sca:sca-maven-plugin:sca-coupling
```

This is theoretically also possible for the reporting goals, but it is not
recommended, since integration with the overall maven site will not work that way. 

### Usage by integration in pom.xml
The plugin can be directly integrated in the maven default and site lifecycles
by configuring the steps in the respective pom.xml file.

To automatically run the desired goals just add the plugin to the build plugins.
```xml
<build>
  <plugins>
    <plugin>
      <groupId>dev.meldau.sca</groupId>
      <artifactId>sca-maven-plugin</artifactId>
      <version>1.0-SNAPSHOT</version>
      <executions>
        <execution>
          <phase>test</phase>
          <goals>
            <goal>sca-cohesion</goal>
          </goals>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```
When you call the test lifecycle using `mvn test` or a step including it like `mvn package` the plugin will be run
automatically.

To integrate the reporting goals provided by the plugin, add the plugin to
the reporting section in the pom.xml file.
```xml
<project>
  <reporting>
    <plugins>
      <plugin>
        <groupId>dev.meldau.sca</groupId>
        <artifactId>sca-maven-plugin</artifactId>
        <version>1.0-SNAPSHOT</version>
      </plugin>
    </plugins>
  </reporting>
</project>
```
This can be limited to specific goals by adding the "goal" tag. The Maven site lifecycle can be called by invoking
`mvn site`.

## Configuration
Some goals can be configured to fail if a certain threshold for a metric is reached. This is done by adding
configuration to the pom.xml file. The possible configurations are shown in the code block below.

```xml
<project>
  <build>
    <pluginManagement>
      <plugins>
        <plugin>
          <groupId>dev.meldau.sca</groupId>
          <artifactId>sca-maven-plugin</artifactId>
          <version>1.0-SNAPSHOT</version>
          <configuration>
            <breakOnCycle>true</breakOnCycle> <!-- break on cyclic dependencies -->
            <breakOnLCOM>10</breakOnLCOM> <!-- break on LCOM higher than value -->
            <breakOnCBO>10</breakOnCBO> <!-- break on CBO higher than value -->
            <breakOnPairCBO>10</breakOnPairCBO> <!-- break on Pair-CBO higher than value -->
          </configuration>
        </plugin>
      </plugins>
    </pluginManagement>
  </build>
</project>
```

## Using the plugin results
The plugin saves all its results in the sca-output directory within the maven target.
Scores are available as JSON-Files while generated Graphs are persisted as DOT-Files and PNG-Files.

The following is an example of how this directory could look like:
```
├── colored_coupling_graph.dot # Coupling graph with color coding
├── coupling_graph.dot # Coupling graph with labels
├── coupling_graph.png # Coupling graph as PNG
├── cycles
│   ├── feedback-arc-set.json # JSON-File containing the set of dependencies to remove, if cycles were found
│   ├── summary.dot # DOT-File summary graph generated by jdeps
│   ├── classes_clean_colored.dot # DOT-File of processed graph with colored dependencies to remove, if cycles were found
│   ├── classes_clean_colored.png # PNG-File of processed graph with colored dependencies to remove, if cycles were found
│   ├── classes_clean.dot # DOT-File of processed graph
│   └── classes.dot # DOT-File graph as generated by jdeps
├── dev_meldau_myjavamvntest_App_lcom_graph.dot # DOT-File of LCOM-graph of specific class
├── dev_meldau_myjavamvntest_App_lcom_graph.png # DOT-File of LCOM-graph of specific class
├── sca-cohesion-results.json # JSON-File listing the LCOM-scores
├── sca-coupling-cbo-results.json # JSON-File listing the CBO-scores
└── sca-coupling-pair-cbo-results.json # JSON-File listing the Pair-CBO-scores
```

## JavaDoc documentation
If you want to program based on this plugin, or should you want to improve it, there is technical documentation 
available. You can find the JavaDoc output of the sca-maven-plugin at https://ogni90.github.io/sca-maven-plugin/site/apidocs