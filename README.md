# Deprecated

### This repository is frozen, as the project is continued at the [Eclipse Foundation](http://www.eclipse.org/californium). The new repository is also on GitHub and can be found at https://github.com/eclipse/californium.core. The overall project is split into multiple modules that can be found [here](https://github.com/eclipse/?query=californium).
---

Californium (Cf) CoAP framework
===============================

Implements [RFC 7252](http://tools.ietf.org/html/rfc7252) (CoAP Standard)

[![Build Status](https://api.travis-ci.org/mkovatsc/Californium.png?branch=coap-18)](https://travis-ci.org/mkovatsc/Californium)

Californium is a Java CoAP implementation for IoT Cloud services. Thus, the focus
is on scalability and usability instead of resource-efficiency like for embedded
devices.

Maven
-----

Use `mvn clean install` in the Cf root directory to build everything.
Standalone JARs of the examples will be copied to ./run/.
(For convenience they are directly included in the Git repository.)

The artifacts are hosted at [Maven Central](http://search.maven.org/#search|ga|1|ch.ethz.inf.vs).
Thus, it is enough to include the dependency on `californium` in the pom.xml of your project:

```xml
  <dependencies>
    ...
    <dependency>
      <groupId>ch.ethz.inf.vs</groupId>
      <artifactId>californium</artifactId>
      <version>0.18.7-final</version>
    </dependency>
    ...
  </dependencies>
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
clone and import the [element-connector](https://github.com/mkovatsc/element-connector)
and [Scandium (Sc)](https://github.com/mkovatsc/Scandium)
Add these project to *[Properties &raquo; Java Build Path &raquo; Projects]*.

Interop Server
--------------

A test server is running at [coap://vs0.inf.ethz.ch:5683/](coap://vs0.inf.ethz.ch:5683/).
The root resource responds with its current version. More information
can be found at [http://vs0.inf.ethz.ch/](http://vs0.inf.ethz.ch/).

Another interop server with a different implementation can be found at
[coap://coap.me:5683/](coap://coap.me:5683/).
More information
can be found at [http://coap.me/](http://coap.me/).
