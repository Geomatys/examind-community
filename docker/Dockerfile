FROM tomcat:7-jre8

LABEL maintainer "robin.gilh@geomatys.com"                                                                                                                                                                                                   
LABEL description "Docker image of Examind Community"

ENV SPRING_PROFILES_ACTIVE "standard"
ENV CSTL_SERVICE_URL "http://localhost:8080/examind/WS"
ENV CSTL_URL "http://localhost:8080/examind"
ENV CSTL_HOME "/var/examind/"
ENV CATALINA_OPTS "-Xms512m -Xmx4G -Dfile.encoding=UTF-8"
ENV JPDA_ADDRESS "8000"
ENV JPDA_TRANSPORT "dt_socket"

ADD ./mount/examind/tomcat/webapps/ /usr/local/tomcat/webapps/
ADD ./mount/examind/tomcat/fs/ /usr/local/tomcat/fs/
ADD ./mount/examind/tomcat/bin/setenv.sh /usr/local/tomcat/bin/setenv.sh
ADD ./mount/examind/tomcat/conf/server.xml /usr/local/tomcat/conf/server.xml

ENTRYPOINT ["catalina.sh", "jpda", "run"]
