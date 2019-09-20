export CSTL_HOME="$HOME/.exa-boot";

export DATABASE_URL="postgres://cstl:admin@localhost:5432/exa-boot"
export EPSG_DATABASE_URL="$DATABASE_URL"

export CSTL_URL="http://localhost:9000"

export CSTL_SERVICE_URL="http://localhost:9000/WS"

java -jar -Xdebug -Xrunjdwp:server=y,transport=dt_socket,address=8001,suspend=n target/spring-boot-1.0-SNAPSHOT.jar
