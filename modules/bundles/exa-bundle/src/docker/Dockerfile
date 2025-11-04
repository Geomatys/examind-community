FROM docker.io/tomcat:10.1.48-jre25-temurin

LABEL maintainer="alexis.manin@geomatys.com"
LABEL description="Docker image of Examind Community"

COPY ./examind /usr/local/tomcat/webapps/examind
COPY ./file-system-support/*.jar /usr/local/tomcat/fs/
COPY ./conf/tomcat/bin/setenv.sh /usr/local/tomcat/bin/setenv.sh
COPY ./conf/tomcat/conf/context.xml /usr/local/tomcat/conf/context.xml
