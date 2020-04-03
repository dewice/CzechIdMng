# CzechIdM Tool

## Features
- Release product version - release product under final version, new development version will be set, tag will be prepared.
- Release module version - release module under final version, new development version will be set, tag will be prepared.
- Change product version - set version for all modules.
- Get product version - for test reasons only.
- Build product version - for test reasons only.
- Build project - use released product and install additional released modules and libraries.

## Requirements

- Install ``maven`` - at least version ``3.1`` is required (Configure ``MAVEN_HOME`` environment property or use ``--maven-home`` IdM tool argument). If libraries for maven build are downloaded automatically, then maven repositories (e.g. https://repo1.maven.org/maven2/, https://nexus.bcvsolutions.eu/repository/maven-public-releases) has to be available. Libraries have to be prepared in local maven repository otherwise.
- Node and npm:
  - are downloaded, extracted and put into a node folder created in your installation directory (``target``) automatically (we are using [frontend-maven-plugin](https://github.com/eirslett/frontend-maven-plugin#frontend-maven-plugin)). Node repositories (e.g. https://nodejs.org/dist) has to be available. Node/npm will be "installed" locally to your project, it will not be installed globally on the whole system (and it will not interfere with any Node/npm installations already present).
  - or can be installed globally and used by ``--node-home`` IdM tool argument.
  - third party node modules are downloaded automatically from public node repository.
- ``Java 1.8`` usage is required by conventions. Artefacts have to be built under ``Java 1.8`` version. Java 11 is supported @since version 10.0.0., but we decide to not use it - artefact will run on environment with ``Java 1.8`` version.

## How to get the tool

Executable tool is available in idm.war artefact on path ``<idm.war>/WEB-INF/idm-tool.jar``. If tool is copied into another place, then **don't forget to copy whole ``lib`` folder together with tool**. Tool itself reusing CzechIdM libraries and product modules.

## How to build the tool from source

Standalone ``idm-tool.jar`` can be build under ``dist`` profile from the product sources (this repository). In the tool module folder (checkout master or develop branch is needed):

```bash
mvn clean package -Pdist -DskipTests
```

Executable ``jar`` will contain all required libraries (all-in-one-jar).

Commands examples expects ``idm-tool.jar`` is placed in the same parent folder, where the product or module root folder is.

## Tool arguments

Available tool parameters:

```bash
java -jar idm-tool.jar -h
```

### Command arguments

Available tool commands:

| Argument | Value | Description | Default  |
| --- | :--- | :--- | :--- |
| --release | | Release product or module under ``--release-version`` argument. New development version will be set as ``--develop-version`` argument. | |
| --publish | |  Push prepared development, production and tags into origin repository. | |
| --release-publish | | Release and publish shortcut as one command, taking the same arguments as ``--release`` command. | |
| --build | | Build project or build product only (under current develop version in develop branch). <br /> Maven 'install' command is used for product build, artifact will be installed into the local maven repository (=> usable as dependency for other module). Use ``-p`` argument to project build.| |
| --get-version | | Get current product version (on the development branch or set '--development-branch' argument). | |
| --set-version | | Set current product version (on the development branch - development branch can be changed only). | |
| --revert-version | | Changed versions by release command can be reverted if needed (before commit, usable after change product version only). | |
| -h,--help | | Print tool help. | |
| -v,--version | | Print tool version. | |


### Additional arguments

Additional / optional arguments can be combined with commands above:

| Argument | Value | Description | Default  |
| --- | :--- | :--- | :--- |
| --develop-branch | branchName | Branch with feature - working branch. | ``develop`` |
| --develop-version | snapshot version | Usable with ``--release`` command. After release this version will be used in development branch. | Next minor snapshot semantic version will be used as default (=> current minor snapshot version + 1).<br />See ``--major``, ``--minor``, ``--patch``, ``--hotfix`` argument if different version is needed. |
| --force | | Count of files changed by release command will not be checked. Limit of changed files is [30]. | |
| --hotfix | | Next develop version will be hotfix, e.g. release 1.2.3 => develop 1.2.3.1-SNAPSHOT. | |
| -m,--module | moduleId | Switch to module release / build. | |
| --major | |  Next develop version will be major, e.g. release 1.2.3 => develop 2.0.0-SNAPSHOT. | |
| --master-branch | <masterBranch> | Branch for releases - where feature has to be merged after release.<br />``none`` can be given - merge don't be executed (e.g. when some old hotfix branch is released).| ``master`` |
| --maven-home | path | Maven home directory.<br />Maven directory should contain command ``<maven-home>/bin/mvn``.|  ``MAVEN_HOME`` system property will be used as default. |
| --minor | | Next develop version will be minor, e.g. release 1.2.3 => develop 1.3.0-SNAPSHOT. | |
| -p,--project | | Switch to project build. | |
 --password | git password / token / ssh passphrase  | **If ssh repository is used / cloned, then passphrase for ssh key is needed only**.<br /> **If https repository is used / cloned, then git username and password is needed**.<br />If two-factor authntication is enabled for <username>, then token has to be given (see git documentation, how to generate authentication token for developers).<br />If ssh key is used, then put passphrase for ssh key (It loads the known hosts and private keys from their default locations (identity, id_rsa and id_dsa) in the user’s .ssh directory.).| |
| --patch | | Next develop version will be patch, e.g. release 1.2.3 => develop 1.2.4-SNAPSHOT. | |
| -r,--repository-location | path | Repository root folder - should contain all modules (<repository-location>/Realization/...). | Folder ``CzechIdMng`` in the same folder as ``idm-tool.jar`` will be used as default for product. Folder ``<module>`` in the same folder as ``idm-tool.jar`` will be used as default for module. |
| --release-version | version | Usable with ``--release`` command. Release will be create under this version. | Stable semantic version will be used as default (=> current no snapshot version). |
| --username | git username | Git username, if https repitory is used. When ssh repository is used, then passphrase for ssh key is needed only.| |

## Tool return codes

| Value | Description  |
| :---: | :--- |
| 0 | everything successful |
| 1 | unexpected exception |
| 2 | wrong arguments given into command line |
| 3 | command failed (build or release failed) |

## Tool external configuration

### Configuration file

External ``application.properties`` configuration can be used, placed in the same folder as executable ``idm-tool.jar`` with content:

```properties
## CzechIdM tool external properties
# password is supported only

## Git password / token / ssh passphrase
# If ssh repository is used / cloned, then passphrase for ssh key is needed only
# If https repository is used / cloned, then git username and password is needed.
# If two-factor authntication is enabled for <username>, then token has to be given (see git documentation, how to generate authentication token for developers).
# If ssh key is used, then put passphrase for ssh key (It loads the known hosts and private keys from their
# default locations (identity, id_rsa and id_dsa) in the user’s .ssh directory.).
idm.sec.tool.password=xxxxx
```

When **password is set in property file, then no password is needed as tool argument** (prevent to have password in bash history).

**Password is required for ``release-publish`` or ``--publish`` command. If password is not given as argument nor property file, then console prompt for enter password will be shown.**

### Logging

Tool log file ``spring.log`` is placed by default in system temp directory.
We are using ``logback`` library for logging. External logback configuration can be given to change logs location, logger levels etc.

You may specify the location of the logback configuration file with a system property named ``logback.configurationFile``. The value of this property can be a URL, a resource on the class path or a path to a file external to the application. Example with ``logback.xml`` placed in the same folder as executable ``idm-tool.jar``:

```bash
java -Dlogback.configurationFile=./logback.xml -jar idm-tool.jar --build -p
```

Example ``logback.xml`` configuration file content:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!-- https://springframework.guru/using-logback-spring-boot/ -->
<!-- http://logback.qos.ch/manual/appenders.html -->
<!DOCTYPE configuration>
<configuration>
	<include resource="org/springframework/boot/logging/logback/base.xml"/>

	<appender name="TOOL-LOG-FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">

	  <file>./logs/tool.log</file>

	  <encoder class="ch.qos.logback.classic.encoder.PatternLayoutEncoder">
	    <Pattern>
	      %d{dd-MM-yyyy HH:mm:ss.SSS} [%thread] %-5level %logger{36}.%M - %msg%n
	    </Pattern>
	  </encoder>

	  <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
	    <fileNamePattern>
	    	./logs/tool_%d{dd-MM-yyyy}_%i.log
	    </fileNamePattern>
	    <maxFileSize>10MB</maxFileSize>
	    <maxHistory>10</maxHistory>
	    <totalSizeCap>100MB</totalSizeCap>
	  </rollingPolicy>

	</appender>

    <logger name="eu.bcvsolutions.idm.tool" level="DEBUG">
    	<appender-ref ref="TOOL-LOG-FILE" />
    </logger>
    <logger name="org.eclipse.jgit" level="INFO">
    	<appender-ref ref="TOOL-LOG-FILE" />
    </logger>
</configuration>
```

### Release product

Usable for CzechIdM 10.x version and for LTS 9.7.x version.

```bash
java -jar idm-tool.jar --release --release-version 10.0.0 --develop-version 10.1.0-SNAPSHOT
```

Release product under ``--release-version`` argument.
New development version will be set as ``--develop-version`` argument.

**Local commits in master and develop branches are prepared only.** Local tag with release version is prepared too. Publish release into origin repository (after personal verification, if everything looks great :)):

```bash
java -jar idm-tool.jar --publish --username <git username> --password <ssh public key passphrase or git password or developer token>
```

**Nexus credential has to be configured in maven** for deploy artefacts into our private ``maven-releases`` repository.

Example of ``settings.xml``:

```xml
<settings>
  <servers>
    <!-- Nexus servers -->
    <server>
      <id>maven-releases/id>
      <username>username</username>
      <password>password</password>
    </server>    <server>
      <id>maven-releases</id>
      <username>username</username>
      <password>password</password>
    </server>
    <server>
      <id>nexus</id>
      <username>username</username>
      <password>password</password>
    </server>
    <server>
      <id>nexus-public</id>
      <username>username</username>
      <password>password</password>
    </server>
  </servers>

  <pluginGroups>
     <pluginGroup>external.atlassian.jgitflow</pluginGroup>
  </pluginGroups>

  <profiles>
    <profile>
      <id>nexus-repo</id>
      <repositories>
        <repository>
          <id>maven-snapshots</id>
          <url>https://nexus.bcvsolutions.eu/repository/maven-snapshots/</url>
          <releases><enabled>false</enabled></releases>
          <snapshots><enabled>true</enabled></snapshots>
        </repository>
        <repository>
          <id>maven-release</id>
          <url>https://nexus.bcvsolutions.eu/repository/maven-releases/</url>
          <releases><enabled>true</enabled></releases>
          <snapshots><enabled>false</enabled></snapshots>
        </repository>
        <repository>
          <id>archetype</id>
          <url>https://nexus.bcvsolutions.eu/repository/maven-public-releases/</url>
          <releases><enabled>true</enabled></releases>
          <snapshots><enabled>false</enabled></snapshots>
        </repository>
      </repositories>
    </profile>
  </profiles>
  <activeProfiles>
    <activeProfile>nexus-repo</activeProfile>
  </activeProfiles>
</settings>
```

### Get product version

Get current product version in develop branch.

```bash
java -jar idm-tool.jar --get-version
```

### Change product version

Change product version for all product modules (FE + BE).
Versions are changes only in develop branch (without commit) => changed versions in modules can be re

```bash
java -jar idm-tool.jar --set-version --develop-version 10.1.0-SNAPSHOT
```

### Revert product version

Changed versions by previous command in develop branch can be reverted if needed (before commit, usable after change product version only). Module descriptors (pom.xml, package.json) are reverted only.

```bash
java -jar idm-tool.jar --revert-version
```

### Build product

Available @since version 10.1.0.

Build product only under current develop version in develop branch.

```bash
java -jar idm-tool.jar --build
```

Artefact ``idm.war`` with all product modules and frontend included is available in backend ``app`` module ``target`` folder.

Maven ``install`` command is used under ``-Prelease`` profile, artifact will be installed into the local maven repository (=> usable as dependency for other module).

**Build can be used before release for test purpose.** Target artefact will be the same a can be deployed to tomcat for test if needed.

### Release module

Available @since version 10.1.0.

All commands above are available for standalone module too. The only difference is **added parameter** with **module identifier**:

```bash
java -jar idm-tool.jar --module idm-rec --release-publish --release-version 2.0.0 --develop-version 2.1.0-SNAPSHOT --username <git username> --password <git password or developer token>
```

**Module identifier has to fit the folder name, where module sources are** (by default).  Use argument ``--repository-location`` otherwise.

The command above expects ``idm-tool.jar`` is placed in the same folder, where the module root (repository) folder is and has the same name as module identifier. In example above is used recertification module identifier.

Module can contains one frontend module and one or more backend modules (e.g. parent + api + impl). Module structure has respect module archetype generator (``Realization`` folders, frontend module with ``czechidm-`` prefix etc.).

Standalone modules are most probably in private repository, so credentials are needed.

### Build project

Available @since version 10.1.0.

Usable for build project with CzechIdM dependency ``>= 10.1.0`` or ``>= 9.7.14``.

> Note for **module developer**:  CzechIdM product dependency is / should be defined for project by ``czechidm-version`` maven property by conventions.

Build features:
- Uses released product in defined version and install [released] project modules with backend and frontend is included.
- Checks installation of duplicate modules in different version (e.g. scim module in 1.2.3 and 2.0.0 version placed in ``modules`` folder is not valid => scim module cannot be installed twice).
- Installed module frontend and backend versions have to fit (backend ``pom.xml`` and frontend ``package.json`` version has to be the same).

 [IdM Tool](#how-to-get-the-tool) can be placed into folder structure:

```
./
 ├── tool                           ⟵ [required] Here is idm-tool.jar (+lib or one-fat-jar).
 ├── product                        ⟵ [required] Here is product .war artefact (e.g. idm-9.7.14.war, idm-10.1.0.war or extracted folder idm-10.1.0). Can be downloaded from our nexus.
 ├── modules                        ⟵ [optional] Here are project modules, third party libraries, connectors. Lookout: all dependencies have to be here (third party libraries are not resolved automatically for now).
 ├── frontend                       ⟵ [optional] Here can be custom frontend files - czechidm-app frontend module, localization can be overriden, e.t.c
 |   ├── config                     ⟵ frontend configuration (by profile, stage - see https://github.com/bcvsolutions/CzechIdMng/blob/develop/Realization/frontend/czechidm-app/config/README.md)
 |   ├── czechidm-modules           ⟵ additional frontend modules (or overriden core module files, e.g. localization)
 ├── dist                           ⟵ [optional] Folder will be created automatically by tool usage - it will contain output artefacts ready for deploy (e.g. idm.war).
 ├── target                         ⟵ [optional] Folder will be created automatically by tool usage - used by tool internally for build.
```

After executing command in ``tool`` folder:

```bash
java -jar idm-tool.jar -p --build
```

will be deployable ``idm.war`` artefact (contains installed modules) ready in ``dist`` folder.

Usable additional tool argument:

| Argument | Value | Description | Default  |
| --- | :--- | :--- | :--- |
| --node-home | path | Node home directory for build a project.<br />Global node instalation directory should contain executable node command.<br />For Windows <node-home>/node/node.exe.<br />For Linux <node-home>/node | Node and npm will be dowloaded and installed localy automaticaly into tool target folder (``<target>/npm``) by default |
| -c,--clean | | Clean up dowloaded frontend libraries in node_modules. |  |

## Future development

- add module dependency descriptor with compatible product version list (e.g. scim module 1.2.3 is compatible / tested with product 9.7.14, 10.0.0, 10.1.0)

## License

[MIT License](../../../LICENSE)
