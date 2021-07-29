# sca-maven-plugin

This Maven plugin does statical program analysis (or "Statische Code Analyse" - hence the name).

It performs these major tasks:
* Finding cyclic dependencies in java code and providing a nearly optimal set of dependencies
  to remove to break the cycles
* Calculating a metric for cohesion
* Calculating two metrics for coupling
* Providing maven site reports for those metrics

## Installation

To install the Plugin, Download the latest release from the Github Releases
Page.
To install the JAR-File to your local maven repository use this command:
```
mvn org.apache.maven.plugins:maven-install-plugin:2.5.2:install-file -Dfile=sca-maven-plugin-VERSION.jar
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
recommended. 

### Usage by integration in pom.xml
The plugin can be directly integrated in the maven default and site lifecycles
by configuring the steps in the respective pom.xml

To automatically run the

To integrate the reporting goals provided by the plugin, add the plugin to
the reporting section in the pom.xml file.
```xml
<reporting>
  <plugins>
    <plugin>
      <groupId>dev.meldau.sca</groupId>
      <artifactId>sca-maven-plugin</artifactId>
      <version>1.0-SNAPSHOT</version>
    </plugin>
  </plugins>
</reporting>
```
This can be limited to specific goals by adding the "goal" tag.