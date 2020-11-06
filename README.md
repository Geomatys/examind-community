# EXAMIND Community

Examind Community makes it possible to easily create a complete Spatial Data Infrastructure, from cataloging geographic
resources to operate a platform of sensors that feeds back information in real time.

[https://www.examind.com](https://www.examind.com)

#### Available [OGC web services](http://www.opengeospatial.org/standards)
* **WMS** : 1.1.1 and 1.3.0 (INSPIRE-compliant)
* **WMTS** : 1.0.0
* **CSW** : 2.0.0, 2.0.2 and 3.0.0 (INSPIRE-compliant)
* **SOS** : 1.0.0 and 2.0.0 (need PostGIS database)
* **WFS** : 1.1.0 and  2.0.0 (INSPIRE-compliant)
* **WPS** : 1.0.0
* **WCS** : 1.0.0

#### Supported input data
* Vector :
  * Shapefiles
  * GeoJSON
  * KML
  * GPX
  * GML
  * CSV (with geometry in WKT)
  * MapInfo MIF/MID format
  * PostGIS database
* Raster :
  * Geotiff
  * NetCDF/NetCDF+NCML
  * Grib
  * Images with .tfw and .prj files for projection and transformation informations

## Get started

### Build from sources

#### Requirements
* **JDK 8+** from Oracle. Can be downloaded [here](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) for your platform.
* **Maven 3.x** found [here](https://maven.apache.org/download.cgi)

#### Procedure
```sh
git clone https://github.com/Geomatys/examind-community.git
cd examind-community
mvn install -DskipTests
```
Note 1 : for smaller download without git history: `git clone --depth 1 https://github.com/Geomatys/examind-community.git`

Note 2 : if you want to build with tests, you'll need a test database named `cstl-test` owned by role:password `cstl:admin`.

### Deploy using Docker
If not already done, build sources as stated in previous section.

Build the `examind-community:latest` Docker image
```
cd <base directory>/modules/bundles/exa-bundle
mvn clean install dockerfile:build -Ddocker.tag=latest
``` 
Go to docker folder
```
cd <base directory>/docker
```
then type the command
```
./run.sh

or

docker-compose up -d
```

the web application will be available at http://localhost:8080/examind
you can authenticate with user = admin and password = admin.

### Deploy on Tomcat

#### Requirements

To run Examind, you'll need :
* **JDK 8**. Can be downloaded [here](https://adoptopenjdk.net/) for your platform.
* **PostgreSQL 9.x** (found [here](http://www.postgresql.org/download/)) with a database named `constellation` owned by role:password `cstl:admin`
* **Apache Tomcat 7.0.47+** with support of websockets found [here](http://tomcat.apache.org/download-70.cgi)
or
* **Apache Tomcat 8.0.39+** with support of websockets found [here](http://tomcat.apache.org/download-80.cgi)

#### Tomcat configuration
Create a **setenv.sh** executable file in **bin/** folder of Tomcat with :

```
export CSTL_OPTS="$CSTL_OPTS -Dspring.profiles.active=standard"
CATALINA_OPTS="$CATALINA_OPTS -Dfile.encoding=UTF8 -Xmx1024m -XX:MaxPermSize=128m -Dgeotk.image.cache.size=128m -XX:-HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=./constellation.hprof"
JAVA_HOME=<PATH_TO_JDK>
JRE_HOME=<PATH_TO_JDK>/jre
```
On tomcat 8 add the following property
```
CATALINA_OPTS="$CATALINA_OPTS -Dorg.apache.catalina.core.ApplicationContext.GET_RESOURCE_REQUIRE_SLASH=true"
```

Tomcat startup :
```
<PATH_TO_TOMCAT>/bin/startup.sh
```
Tomcat shutdown :
```
<PATH_TO_TOMCAT>/bin/shutdown.sh
```

### Run with Jetty
Examind can also be started with embedded jetty maven plug-in.
```
mvn jetty:run-war -DMAVEN_OPTS="-Xmx1G -XX:MaxPermSize=256m"
```

### Usage
Browse  [http://localhost:8080/examind](http://localhost:8080/examind) and authenticate with user `admin` and password `admin`.


### Configuration
Examind retrieve his configuration through various inputs using following priority  :
1. System environment variables following standard naming convention
2. Startup options (`-Dproperty=value`) following standard java properties naming convention
3. External configuration file (referenced with `-Dcstl.config=/path/to/config.properties` option)
4. Default embedded configuration

For example, database configuration can be specified from environment variable `DATABASE_URL` or startup/external property `database.url`.

#### Available configuration properties
* **database.url** : application database URL in Hiroku like format. Default value `postgres://cstl:admin@localhost:5432/constellation`
* **epsg.database.url** : EPSG database URL. Default value same as **database.url**
* **test.database.url** : testing database URL. Default value `postgres://test:test@localhost:5432/cstl-test`
* **cstl.config** : Path to application external configuration properties file. Optional, default null.
* **cstl.url** : Examind application URL. Used by Examind to generate resources URLs.
* **cstl.home** : Application home directory, used by Examind to store logs, indexes, ... . By default, Examind will create a `.constellation` directory in current user home folder.
* **cstl.data** : Application data directory, used by Examind to store integrated data and some configurations ... .  By default, Examind will create a `data` directory relative to `cstl.home` property.

SMTP server configuration (used to re-initialize user password) :
* **cstl.mail.smtp.from** : Default value `no-reply@localhost`
* **cstl.mail.smtp.host** : Default value `localhost`
* **cstl.mail.smtp.port** : Default value `25`
* **cstl.mail.smtp.username** : Default value `no-reply@localhost`
* **cstl.mail.smtp.password** : Default value `mypassword`
* **cstl.mail.smtp.ssl** : Default value `false`

#### Database configuration syntax

It is recommended to use [standard jdbc urls](https://docs.oracle.com/javase/tutorial/jdbc/basics/connecting.html#db_connection_url) when specifying database url. A custom syntax is allowed, but not recommended anymore.

## Contribute

### Activate Git hooks
Examind use Git hooks to standardize commits message format.

```shell
rm .git/hooks/commit-msg
ln -s githook/* .git/hooks/
```
