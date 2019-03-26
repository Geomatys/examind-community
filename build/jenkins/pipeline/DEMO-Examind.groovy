node {
    deleteDir()

    //variables globales
    def projectName    = "DEMO-Examind"
    def repoGitlab     = "gitlab.geomatys.com/constellation-enterprise/constellation-ee.git"

    env.JAVA_HOME      = "${tool 'jdk8u112'}"
    env.PATH           = "${env.JAVA_HOME}/bin:${env.PATH}"
    sh "export CHARSET=UTF-8"

    echo '\n\
    --- PARAMETERS ---------------------------\n\
    GIT_CREDENTIALS : '+GIT_CREDENTIALS+'\n\
    DEPLOY_CREDENTIALS : '+DEPLOY_CREDENTIALS+'\n\
    SHA1_COMMIT : '+SHA1_COMMIT+'\n\
    --- VARIABLES ----------------------------\n\
    Project name : '+projectName+'\n\
    Project git : https://'+repoGitlab+'\n\
    ------------------------------------------\n\
    '

    stage('Checkout'){
        git branch: 'master', url:'https://'+repoGitlab, credentialsId:GIT_CREDENTIALS
        sh "git checkout "+SHA1_COMMIT
    }

    withMaven (jdk:'jdk8_latest', maven:'maven_latest', mavenLocalRepo:'m2rep', mavenSettingsConfig:'5919b553-67ec-4e2e-a9f9-bb0f3d51c8cd') {
        stage('Build') {
            //on force le dossier de la base epsg pour avoir toujours la derniere version
            sh 'pwd > workspace'
            def workspace = readFile('workspace').trim()
            env.SIS_DATA = workspace + "/sisdb"
            sh 'mkdir sisdb'

            //configuration de constellation
            sh 'mkdir exaconfig'
            env.CSTL_HOME = workspace + "/exaconfig"
            env.TEST_DATABASE_URL = "postgres://cstl:cstl@gitlab.geomatys.com:4242/cstl_test_db"

            echo '-------------------------------'
            sh "mvn -version"
            echo '-------------------------------'
            sh "mvn clean install -DskipTests -Pallws,iho,dted,hadoop,autocad,netcdf -B -Dmaven.repo.local=m2rep"
        }
    }

    stage('Deployment'){
        sshagent ([DEPLOY_CREDENTIALS]) {
            sh """
             ssh -o StrictHostKeyChecking=no core@193.54.123.161 "cd /home/core/docker_containers/examind-demo && /opt/bin/docker-compose stop constellation && sudo rm -rf /home/core/docker_data/examind-demo/mount/webapps/examind*"
             scp -o StrictHostKeyChecking=no ./modules/cstl-bundle/target/examind.war core@193.54.123.161:/home/core/docker_data/examind-demo/mount/webapps/.
             ssh -o StrictHostKeyChecking=no core@193.54.123.161 "cd /home/core/docker_containers/examind-demo && ./reload-constellation.sh"
         """
        }
    }

    stage('Clean'){
        deleteDir()
    }

    stage('Notification'){
        emailext subject: '[JENKINS] '+projectName+' intégration continue mise à jour du war Examind',
                body: 'Le serveur de demo Examind http://demo.examind.com a été mis à jour avec un nouveau war buildé à partir du commit/branch '+SHA1_COMMIT,
                to: 'dev@geomatys.com'
    }
}