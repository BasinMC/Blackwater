[![License](https://img.shields.io/github/license/BasinMC/Blackwater.svg?style=flat-square)](https://www.apache.org/licenses/LICENSE-2.0.txt)
[![GitHub Release](https://img.shields.io/github/release/BasinMC/Blackwater.svg?style=flat-square)](https://github.com/BasinMC/Blackwater/releases)
[![CircleCI](https://img.shields.io/circleci/project/github/BasinMC/Blackwater.svg?style=flat-square)](https://circleci.com/gh/BasinMC/Blackwater)

Blackwater
==========

A generic framework for chaining build tasks in various build systems.

# Table of Contents

* [Features](#features)
* [Usage](#usage)
* [Building](#building)
* [Contact](#contact)
* [Issues](#issues)
* [Contributing](#contributing)
* [License](#license)

Features
--------

* Chaining of tasks
* Support for custom task implementations
* Caching (and thus reusing) of task results
* Simple integration with third party tools

Usage
-----

**Artifact Coordinates:** `org.basinmc:blackwater:1.0-SNAPSHOT`

```xml
<repository>
  <id>basin-bintray</id>
  <name>Basin Releases</name>
  <url>https://dl.bintray.com/basin/maven/</url>
</repository>

<!-- ... -->

<dependency>
  <groupId>org.basinmc.blackwater</groupId>
  <artifactId>pipeline</artifactId>
  <version>1.0-SNAPSHOT</version>
</dependency>
```

```java
ArtifactManager manager = ...;
ArtifactReference reference = ...;
Task task = ...;
Path outputPath = ...;

Pipeline pipeline = Pipeline.builder()
  .withArtifactManager(manager) // optional
  .withTask(task)
    .withInputArtifact(reference) // optional
    .withOutputFile(outputPath) // optional
    .register()
  .build();

pipeline.execute();
```

Note that either an input/output artifact or file may be specified for each task (the exact
limitations and additional parameters are specified by the task implementation).

Building
--------

1. Clone this repository via ```git clone https://github.com/BasinMC/Blackwater.git``` or download a [zip](https://github.com/BasinMC/Blackwater/archive/master.zip)
2. Build the modification by running ```mvn clean install```
3. The resulting jars can be found in their respective ```target``` directories as well as your local maven repository

Contact
-------

* [IRC #Basin on EsperNet](http://webchat.esper.net/?channels=Basin)
* [Twitter](https://twitter.com/BasinMC)
* [GitHub](https://github.com/BasinMC/Blackwater)

Issues
------

You encountered problems with the library or have a suggestion? Create an issue!

1. Make sure your issue has not been fixed in a newer version (check the list of [closed issues](https://github.com/BasinMC/Blackwater/issues?q=is%3Aissue+is%3Aclosed)
1. Create [a new issue](https://github.com/BasinMC/Blackwater/issues/new) from the [issues page](https://github.com/BasinMC/Blackwater/issues)
1. Enter your issue's title (something that summarizes your issue) and create a detailed description containing:
   - What is the expected result?
   - What problem occurs?
   - How to reproduce the problem?
   - Crash Log (Please use a [Pastebin](https://gist.github.com) service)
1. Click "Submit" and wait for further instructions

Contributing
------------

Before you add any major changes to the library you may want to discuss them with us (see
[Contact](#contact)) as we may choose to reject your changes for various reasons. All contributions
are applied via [Pull-Requests](https://help.github.com/articles/creating-a-pull-request). Patches
will not be accepted. Also be aware that all of your contributions are made available under the
terms of the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt). Please read
the [Contribution Guidelines](CONTRIBUTING.md) for more information.

License
-------

This project is released under the terms of the
[Apache License](https://www.apache.org/licenses/LICENSE-2.0.txt), Version 2.0.

The following note shall be replicated by all contributors within their respective newly created
files (variables are to be replaced; E-Mail address or URL are optional):

```
Copyright <year> <first name> <surname <[email address/url]>
and other copyright owners as documented in the project's IP log.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

