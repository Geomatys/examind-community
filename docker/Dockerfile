FROM tomcat:7-jre8

LABEL maintainer "robin.gilh@geomatys.com"                                                                                                                                                                                                   
LABEL description "Docker image of Examind Community"

ADD ./mount/examind/tomcat/webapps/ /usr/local/tomcat/webapps/
ADD ./mount/examind/tomcat/fs/ /usr/local/tomcat/fs/
ADD ./mount/examind/tomcat/bin/setenv.sh /usr/local/tomcat/bin/setenv.sh
ADD ./mount/examind/tomcat/conf/server.xml /usr/local/tomcat/conf/server.xml

ENTRYPOINT ["catalina.sh", "run"]
