if [ -r "$CATALINA_BASE/fs/geomatys-ftp.jar" ]; then
    export CLASSPATH="$CLASSPATH":"$CATALINA_HOME"/fs/geomatys-ftp.jar
fi
if [ -r "$CATALINA_BASE/fs/geomatys-s3.jar" ]; then
    export CLASSPATH="$CLASSPATH":"$CATALINA_HOME"/fs/geomatys-s3.jar
fi
if [ -r "$CATALINA_BASE/fs/geomatys-smb.jar" ]; then
    export CLASSPATH="$CLASSPATH":"$CATALINA_HOME"/fs/geomatys-smb.jar
fi
if [ -r "$CATALINA_BASE/fs/geomatys-hdfs.jar" ]; then
    export CLASSPATH="$CLASSPATH":"$CATALINA_HOME"/fs/geomatys-hdfs.jar
fi