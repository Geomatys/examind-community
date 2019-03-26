// Define a pod to launch job into. Needs Jenkins Kubernetes plugin
podTemplate(label: 'slave', containers: [
    containerTemplate(name: 'postgres', image: 'postgres:9')
    ]
) {

    /*
     * FUNCTIONS
     */

    /**
     * Fonction de mise a jour des versions des pom.xml du projet maven
     */
    def updatePomVersion() {
        def oldver = OLD_VERSION;
        def newver = NEW_VERSION;
        def ant = '<?xml version="1.0" encoding="UTF-8"?> \
            <project name="Replace" default="replace"> \
              <target name="replace"> \
                <replace dir="." summary="yes"> \
                  <include name="**/pom.xml"/> \
                  <replacefilter token="'+ oldver +'" value="'+ newver +'"/> \
                </replace> \
              </target> \
            </project>';

        writeFile text: ant, file: 'Replace.xml';
        def antHome = tool 'ant1.9.7';
        sh "${antHome}/bin/ant -f Replace.xml";
    }

    // Launch into a JNLP container
    node('slave') {
        deleteDir()

        //variables globales
        def projectName = "RELEASE-Examind"
        def repoGitlab = "gitlab.geomatys.com/constellation-enterprise/constellation-ee.git"
        def releaseVersion = NEW_VERSION
        env.JAVA_HOME = "${tool 'jdk8u112'}"
        env.PATH = "${env.JAVA_HOME}/bin:${env.PATH}"
        sh "export CHARSET=UTF-8"

        echo '\n\
        --- PARAMETERS ---------------------------\n\
        OLD_VERSION : ' + OLD_VERSION + '\n\
        NEW_VERSION : ' + NEW_VERSION + '\n\
        SHA1_COMMIT : ' + SHA1_COMMIT + '\n\
        NEXUS_DEPLOY_URL : ' + NEXUS_DEPLOY_URL + '\n\
        GIT_CREDENTIALS : ' + GIT_CREDENTIALS + '\n\
        --- VARIABLES ----------------------------\n\
        Project name : ' + projectName + '\n\
        Project release : ' + releaseVersion + '\n\
        Project git : https://' + repoGitlab + '\n\
        ------------------------------------------\n\
        '

        stage('Checkout') {
            git branch: 'master', url: 'https://' + repoGitlab, credentialsId: GIT_CREDENTIALS
            sh "git checkout " + SHA1_COMMIT
            sh 'git config user.name "Jenkins"'
            sh 'git config user.email "admin@geomatys.com"'
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
            //on force le dossier de la base epsg pour avoir toujours la derniere version
            sh 'pwd > workspace'
            def workspace = readFile('workspace').trim()
            env.SIS_DATA = workspace + "/sisdb"
            sh 'mkdir sisdb'

            //configuration de constellation
            sh 'mkdir exaconfig'
            env.CSTL_HOME = workspace + "/exaconfig"
        }

        stage('Change versions') {
            sh "git branch release"
            sh "git checkout release"
            updatePomVersion()
            sh "git commit -m 'chore(*): Release ${NEW_VERSION}' -a"
            sh "git tag "+releaseVersion
        }

        withMaven (jdk:'jdk8_latest', maven:'maven_latest', mavenLocalRepo:'m2rep', mavenSettingsConfig:'5919b553-67ec-4e2e-a9f9-bb0f3d51c8cd') {
            stage('Build') {
                echo '-------------------------------'
                sh "mvn -version"
                echo '-------------------------------'
                sh "mvn clean source:jar javadoc:jar install -B -Pfull -Dmaven.repo.local=m2rep"
            }

            stage('Deployment') {
                sh "mvn source:jar javadoc:jar deploy -B -DskipTests -Dmaven.repo.local=m2rep -DaltDeploymentRepository=nexus::default::" + NEXUS_DEPLOY_URL
                withCredentials([[$class          : 'UsernamePasswordMultiBinding',
                                  credentialsId   : GIT_CREDENTIALS,
                                  usernameVariable: 'GITUSR',
                                  passwordVariable: 'GITPWD']]) {
                    sh "git push https://" + GITUSR + ":" + GITPWD + "@" + repoGitlab + " " + releaseVersion
                }
            }
        }

        stage('Clean') {
            deleteDir()
        }

        stage('Notification') {
            emailext subject: '[JENKINS] Release '+projectName+' '+releaseVersion,
                    body: 'Nouveau tag disponible pour le projet '+projectName+' : '+releaseVersion+ " , sur le repository "+NEXUS_DEPLOY_URL,
                    to: 'dev@geomatys.com'
        }
    }
}