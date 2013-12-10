Californium (Cf) CoAP framework
===============================

Implements [draft-ietf-core-coap-18](http://tools.ietf.org/html/draft-ietf-core-coap-18) (Proposed Standard)

[![Build Status](https://api.travis-ci.org/mkovatsc/Californium.png?branch=coap-18)](https://travis-ci.org/mkovatsc/Californium)

Californium is a Java CoAP implementation for IoT Cloud services. Thus, the focus
is on scalability and usability instead of resource-efficiency like for embedded
devices.

Maven
-----

Use `mvn clean install` in the Cf root directory to build everything.
Standalone JARs of the examples will be copied to ./run/.
(For convenience they are directly included in the Git repository.)

The Maven repositories are:

* [https://github.com/mkovatsc/maven/raw/master/releases/](https://github.com/mkovatsc/maven/raw/master/releases/)
* [https://github.com/mkovatsc/maven/raw/master/snapshots/](https://github.com/mkovatsc/maven/raw/master/snapshots/)

### Installation Using Maven

Just add the following items to your pom.xml:

```xml
  <dependencies>
    ...
    <dependency>
            <groupId>ch.ethz.inf.vs</groupId>
            <artifactId>californium</artifactId>
            <version>0.18.1-SNAPSHOT</version>
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

The project also includes the project files for Eclipse. Make sure to have the
following before importing the Californium (Cf) projects:

* [Eclipse EGit](http://www.eclipse.org/egit/)
* [m2e - Maven Integration for Eclipse](http://www.eclipse.org/m2e/)
* UTF-8 workspace text file encoding (Preferences &raquo; General &raquo; Workspace)

Then choose *[Import... &raquo; Git &raquo; Projects from Git &raquo; Local]*
to import Californium into Eclipse.

### Without Any Maven Support

In case you are using plain Eclipse projects without Maven, you also need to
clone and import the [element-connector](https://github.com/mkovatsc/element-connector).
Add this project to Properties &raquo; Java Build Path &raquo; Projects.

Interop Server
--------------

A test server is running at [coap://vs0.inf.ethz.ch:5683/](coap://vs0.inf.ethz.ch:5683/).
The root resource responds with its current version. More information
can be found at [http://vs0.inf.ethz.ch/](http://vs0.inf.ethz.ch/).

Another interop server with a different implementation can be found at
[coap://coap.me:5683/](coap://coap.me:5683/).
More information
can be found at [http://coap.me/](http://coap.me/).
