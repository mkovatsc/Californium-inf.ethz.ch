Californium (Cf) CoAP framework in Java
=======================================

It is recommended to use the latest draft (or eventually RFC) of CoAP.
----------------------------------------------------------------------

coap-08
-------

This branch stopped at draft-ietf-core-coap-08 and is missing later bugfixes!
coap-08 is the first to include proxy and HTTP cross-proxy functionality.

General Information
-------------------

Californium is a Java CoAP implementation targeting back-end services. Thus, the
focus is on usability and features, not on resource-efficiency like for embedded
devices. The Java implementation fosters quick server and client development.

Eclipse
-------

Californium comes with the required project files for Eclipse. Use the EGit
plugin to import the projects from Git (File/Import... > Git/Projects from Git).

Maven
-----

Cf is also a Maven project. Use

	mvn clean install

in the Cf root directory to build everything.
Standalone JARs of the examples will be copied to ./run/.
(For convenience they are directly included in the Git repository.)

The Maven repositories are:
- http://maven.thingml.org/archiva/repository/thingml-release
- http://maven.thingml.org/archiva/repository/thingml-snapshot

Build Status
------------

- https://travis-ci.org/mkovatsc/Californium
- http://build.thingml.org/
