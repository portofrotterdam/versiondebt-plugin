# versiondebt-plugin

This project contains a simple Maven and SonarQube plugin to measure how much debt your dependency versions have.

Example output of the Maven plugin:

```
    [INFO] --- Dependency:org.apache.maven.plugin-tools : maven-plugin-annotations ---
    [INFO] 
    [INFO] 	Current version: 3.2	Wed Nov 07 15:41:57 CET 2012
    [INFO] 	Latest version: 3.5	Fri Aug 26 23:20:30 CEST 2016
    [INFO] 
    [INFO] 	Artifact debt: 3 years, 293 days, 6 hours
```

As you can see, we are using maven plugin tools 3.2 and this dependency has a whopping 3 years of new development we aren't using at the moment.

## Usage

### Maven plugin

To use the Maven plugin you'll need to do the following.

This plugin hasn't been published to Maven Central (yet?) so in order to use it you'll have to build it yourself.

After building this project you'll be able to do:

```
    mvn clean package versiondebt:generate
```
 
This outputs the version information in the Maven log, but also generates a file called *versiondebt.xml* in the target folder. This XML file contains all 
the information needed for the SonarQube plugin.

### SonarQube plugin

If you have a SonarQube build in your CI environment you can use the SonarQube plugin as well. First you'll have to make sure the Maven plugin is running in your 
build. You can add it in the *pom.xml* file as follows:

```xml
    <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>versiondebt-maven-plugin</artifactId>
        <version>1.0.6</version>
        <executions>
            <execution>
                <id>versiondebt</id>
                <phase>prepare-package</phase>
                <goals>
                    <goal>generate</goal>
                </goals>
            </execution>
        </executions>
     </plugin>
```
     
The plugin isn't very fast because it needs to do a couple of HTTP calls, if you decide to add it to the build, make a specific SonarQube profile in Maven.

Next step is to put the plugin in SonarQube, this can be done by taking the compiled *versiondebt-sonar-plugin-1.0.0-SNAPSHOT.jar* and to put this in the plugins
 folder of your SonarQube installation. 
 
## Contribution
 
 These plugins have been developed during a Port of Rotterdam hackathon and aren't maintained on a regular basis. If you have any contribution, feel free to 
 file a pull request, or develop it further in your own fork.