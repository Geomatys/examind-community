FROM openjdk:22

ADD https://dlcdn.apache.org/tomcat/tomcat-10/v10.1.34/bin/apache-tomcat-10.1.34.tar.gz apache-tomcat.tar.gz
RUN tar -zxf apache-tomcat.tar.gz && mv apache-tomcat-* apache-tomcat && rm apache-tomcat.tar.gz
# Remove default apps
RUN rm -rf apache-tomcat/webapps/*

LABEL maintainer "robin.gilh@geomatys.com"                                                                                                                                                                                                   
LABEL description "Docker image of Examind Community"

COPY ./examind apache-tomcat/webapps/examind
COPY ./file-system-support/*.jar apache-tomcat/fs/
COPY ./conf/tomcat/bin/setenv.sh apache-tomcat/bin/setenv.sh
COPY ./conf/tomcat/conf/context.xml apache-tomcat/conf/context.xml

ENV CATALINA_HOME apache-tomcat
ENV PATH $CATALINA_HOME/bin:$PATH

ENTRYPOINT ["apache-tomcat/bin/catalina.sh"]
CMD ["run"]
