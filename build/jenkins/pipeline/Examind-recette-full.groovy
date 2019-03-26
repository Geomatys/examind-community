// Define a pod to launch job into. Needs Jenkins Kubernetes plugin
podTemplate(label: 'slave', containers: [
        containerTemplate(name: 'postgres', image: 'postgres:9')
]
) {

    // Launch into a JNLP container
    node('slave') {
        deleteDir()

        // Variables globales
        def projectName    = "RECETTE-Examind-full"
        sh "export CHARSET=UTF-8"

        echo '\n\
        --- PARAMETERS ---------------------------\n\
        GIT_CREDENTIALS : '+GIT_CREDENTIALS+'\n\
        DEPLOY_CREDENTIALS : '+DEPLOY_CREDENTIALS+'\n\
        SHA1_COMMIT_GEOTK : '+SHA1_COMMIT_GEOTK+'\n\
        SHA1_COMMIT_SDK : '+SHA1_COMMIT_SDK+'\n\
        SHA1_COMMIT_SERVER : '+SHA1_COMMIT_SERVER+'\n\
        --- VARIABLES ----------------------------\n\
        Project name : '+projectName+'\n\
        ------------------------------------------\n\
        '

        stage('Checkout') {
            dir('geotk') {
                git branch: 'master', url:'ssh://git@gitlab.geomatys.com:10022/geotoolkit-group/geotoolkit.git', credentialsId:GIT_CREDENTIALS
                sh "git checkout "+SHA1_COMMIT_GEOTK
            }

            dir('sdk') {
                git branch: 'master', url:'ssh://git@gitlab.geomatys.com:10022/geomatys-group/examind-sdk.git', credentialsId:GIT_CREDENTIALS
                sh "git checkout "+SHA1_COMMIT_SDK
            }


            dir('server') {
                git branch: 'master', url:'ssh://git@gitlab.geomatys.com:10022/constellation-enterprise/constellation-ee.git', credentialsId:GIT_CREDENTIALS
                sh "git checkout "+SHA1_COMMIT_SERVER
            }
        }

        // Launch in a postgres container
        container('postgres') {
            stage('Init test database') {
                // Define testing database
                def dbUser = "cstl"
                def dbPassword = "cstl"
                def dbHost = "examind-testdb.jenkins-builds.svc.cluster.local"
                def dbPort = "5432"
                def dbUrl = "postgres://${dbUser}:${dbPassword}@${dbHost}:${dbPort}/postgres"
                def dbName = "release_examind_test_db"

                sh "psql -d ${dbUrl} -c 'DROP DATABASE ${dbName}' -c 'CREATE DATABASE ${dbName}'"

                env.TEST_DATABASE_URL = "${dbUrl}"
            }
        }

        stage('Init workspace') {
            // On force le dossier de la base epsg pour avoir toujours la derniere version
            sh 'pwd > workspace'
            def workspace = readFile('workspace').trim()
            env.SIS_DATA = workspace + "/sisdb"
            sh 'mkdir sisdb'
            // Configuration de constellation
            sh 'mkdir exaconfig'
            env.CSTL_HOME = workspace + "/exaconfig"
        }

        withMaven (jdk:'jdk8_latest', maven:'maven_latest', mavenLocalRepo:'m2rep', mavenSettingsConfig:'5919b553-67ec-4e2e-a9f9-bb0f3d51c8cd', options: [artifactsPublisher(disabled: true)]) {


            /*stage('Build Geotk') {
                echo '-------------------------------'
                sh "mvn -version"
                echo '-------------------------------'
                sh "mvn clean install -B -f geotk/pom.xml -DskipTests=true"
            }

            stage('Build SDK') {
                sh "mvn clean install -B -f sdk/pom.xml -Dgdal.profile=gdal -DskipTests=true"
            }*/

            stage('Build Server') {
                sh "mvn clean install -B -Pfull -f server/pom.xml -DskipTests=true"
            }
        }

        stage('Deployment') {
            sshagent ([DEPLOY_CREDENTIALS]) {
                //ssh -o StrictHostKeyChecking=no core@193.54.123.161 "cd /home/core/docker_containers/examind-recette && /opt/bin/docker-compose stop constellation && sudo rm -rf /home/core/docker_data/examind-recette/mount/webapps/examind*"
                //scp -o StrictHostKeyChecking=no ./server/modules/cstl-bundle/target/examind.war core@193.54.123.161:/home/core/docker_data/examind-recette/mount/webapps/.
                //ssh -o StrictHostKeyChecking=no core@193.54.123.161 "cd /home/core/docker_containers/examind-recette && ./reload-constellation.sh"

                sh """
                ssh -o StrictHostKeyChecking=no core@193.48.189.43 "/opt/bin/kubectl scale --namespace demos --replicas=0 deployment examind-recette-examind && sudo rm -r /mnt/lun3/persistentvolume/examind-recette/webapps/examind.war"
                scp -o StrictHostKeyChecking=no ./server/modules/cstl-bundle/target/examind.war core@193.48.189.43:/mnt/lun3/persistentvolume/examind-recette/webapps/.
                ssh -o StrictHostKeyChecking=no core@193.48.189.43 "/opt/bin/kubectl scale --namespace demos --replicas=1 deployment examind-recette-examind"
                """
            }
        }

        stage('Clean'){
            deleteDir()
        }

        stage('Notification'){
            emailext subject: '[JENKINS] '+projectName+' intégration continue mise à jour du war Examind',
                    body: 'Le serveur de recette Examind http://kube-recette.examind.com a été mis à jour avec un nouveau war buildé à partir du commit/branch '+SHA1_COMMIT_SERVER,
                    to: 'dev@geomatys.com'
        }
    }
}