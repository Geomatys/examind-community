try {
    // Variables globales
    def projectName    = "RECETTE-Examind"

    stage ("init job") {
        sh "export CHARSET=UTF-8"
        echo '\n\
        --- PARAMETERS ---------------------------\n\
        gitlabMergeRequestDescription : '+env.gitlabMergeRequestDescription+'\n\
        gitlabMergeRequestLastCommit : '+env.gitlabMergeRequestLastCommit+'\n\
        gitlabSourceBranch : '+env.gitlabSourceBranch+'\n\
        --- VARIABLES ----------------------------\n\
        Project name : '+projectName+'\n\
        ------------------------------------------\n\
        '

        updateGitlabCommitStatus name: 'build', state: 'pending'
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

    withMaven (jdk:'jdk8_latest', maven:'maven_latest', mavenLocalRepo:'m2rep', mavenSettingsConfig:'5919b553-67ec-4e2e-a9f9-bb0f3d51c8cd') {
        stage('Build') {
            echo '-------------------------------'
            sh "mvn -version"
            echo '-------------------------------'
            sh "mvn clean source:jar javadoc:jar install -B -Pfull -Dmaven.repo.local=m2rep"
        }
    }
        
    stage('Clean') {
        deleteDir()
        updateGitlabCommitStatus name: 'build', state: 'success'
        addGitLabMRComment comment: ":thumbsup:"
    }
} catch (err) {
    updateGitlabCommitStatus name: 'build', state: 'failed'
    addGitLabMRComment comment: ":thumbsdown:"
    throw err
}