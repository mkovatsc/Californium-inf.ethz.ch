Californium (Cf) CoAP framework
===============================

Implements [draft-ietf-core-coap-13](http://tools.ietf.org/html/draft-ietf-core-coap-13)

[![Build Status](https://api.travis-ci.org/mkovatsc/Californium.png?branch=coap-13)](https://travis-ci.org/mkovatsc/Californium)

Californium is a Java CoAP implementation targeting back-end services. Thus, the
focus is on usability and features, not on resource-efficiency like for embedded
devices. The Java implementation fosters quick server and client development.

Maven
-----

Use `mvn clean install` in the Cf root directory to build everything.
Standalone JARs of the examples will be copied to ./run/.
(For convenience they are directly included in the Git repository.)

The Maven repositories are:

* [https://github.com/mkovatsc/maven/raw/master/releases/](https://github.com/mkovatsc/maven/raw/master/releases/)
* [https://github.com/mkovatsc/maven/raw/master/snapshots/](https://github.com/mkovatsc/maven/raw/master/snapshots/)

Installation using Maven
------------------------

Just add the following items to your pom.xml:

```xml
  <dependencies>
    ...
    <dependency>
            <groupId>ch.ethz.inf.vs</groupId>
            <artifactId>californium</artifactId>
            <version>0.13.6</version>
    </dependency>
    ...
  </dependencies>
  
  <repositories>
    ...
    <repository>
            <id>mkovatsc-github-releases</id>
            <name>mkovatsc-github</name>
            <url>https://github.com/mkovatsc/maven/raw/master/releases/</url>
    </repository>
    <repository>
            <id>mkovatsc-github-snapshots</id>
            <name>mkovatsc-github</name>
            <url>https://github.com/mkovatsc/maven/raw/master/snapshots/</url>
            <snapshots>
            <enabled>true</enabled>
            </snapshots>
    </repository>
    <repository>
            <id>mkovatsc-github-thirdparty</id>
            <name>mkovatsc-github</name>
            <url>https://github.com/mkovatsc/maven/raw/master/thirdparty/</url>
    </repository>
    ...
  </repositories>
```

Eclipse
-------

The project also includes the files for Eclipse. Best use
[EGit](http://www.eclipse.org/egit/) and choose
*[Import... &raquo; Git &raquo; Projects from Git &raquo; Local]*
to import Californium into Eclipse.

Interop Server
--------------

A test server is running at [coap://vs0.inf.ethz.ch:5683/](coap://vs0.inf.ethz.ch:5683/).
The root resource responds with its current version. More information
can be found at [http://vs0.inf.ethz.ch/](http://vs0.inf.ethz.ch/).
