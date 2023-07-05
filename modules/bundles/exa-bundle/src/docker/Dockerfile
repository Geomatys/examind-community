FROM tomcat:10.1.10-jdk17-temurin

LABEL maintainer "robin.gilh@geomatys.com"                                                                                                                                                                                                   
LABEL description "Docker image of Examind Community"

COPY ./examind /usr/local/tomcat/webapps/examind
COPY ./file-system-support/*.jar /usr/local/tomcat/fs/
COPY ./conf/tomcat/bin/setenv.sh /usr/local/tomcat/bin/setenv.sh

ENTRYPOINT ["catalina.sh", "run"]
