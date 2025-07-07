FROM eclipse-temurin:24.0.1_9-jre-noble

ARG TOMCAT_VERSION="10.1.43"

RUN wget --quiet -O apache-tomcat.tar.gz "https://archive.apache.org/dist/tomcat/tomcat-${TOMCAT_VERSION%%.*}/v${TOMCAT_VERSION}/bin/apache-tomcat-${TOMCAT_VERSION}.tar.gz" \
    && tar -zxf apache-tomcat.tar.gz \
    && mv apache-tomcat-* apache-tomcat \
    && rm apache-tomcat.tar.gz \
    && rm -rf apache-tomcat/webapps/* # Remove default apps

LABEL maintainer="robin.gilh@geomatys.com"
LABEL description="Docker image of Examind Community"

COPY ./examind apache-tomcat/webapps/examind
COPY ./file-system-support/*.jar apache-tomcat/fs/
COPY ./conf/tomcat/bin/setenv.sh apache-tomcat/bin/setenv.sh
COPY ./conf/tomcat/conf/context.xml apache-tomcat/conf/context.xml

ENV CATALINA_HOME apache-tomcat
ENV PATH $CATALINA_HOME/bin:$PATH

ENTRYPOINT ["apache-tomcat/bin/catalina.sh"]
CMD ["run"]
