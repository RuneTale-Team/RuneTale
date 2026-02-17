pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    environment {
        GRADLE_OPTS = '-Dorg.gradle.daemon=false'
    }

    stages {
        stage('Set GitHub Status: Pending') {
            steps {
                githubNotify context: 'jenkins/rune-ci', status: 'PENDING', description: 'Build started'
            }
        }

        stage('Build, Test, Package') {
            steps {
                sh 'chmod +x ./gradlew'
                sh './gradlew --no-daemon clean test build bundlePluginJars'
            }
        }

        stage('Archive Reports & Artifacts') {
            steps {
                junit testResults: '**/build/test-results/test/*.xml', allowEmptyResults: true
                archiveArtifacts artifacts: 'plugins/*/build/distributions/*.jar,build/distributions/*plugin-jars-*.zip', fingerprint: true
            }
        }

    }

    post {
        success {
            githubNotify context: 'jenkins/rune-ci', status: 'SUCCESS', description: 'Build succeeded'
            echo 'Build, tests, packaging completed successfully.'
        }
        unstable {
            githubNotify context: 'jenkins/rune-ci', status: 'FAILURE', description: 'Build unstable'
            echo 'Pipeline is unstable. Check test reports.'
        }
        aborted {
            githubNotify context: 'jenkins/rune-ci', status: 'FAILURE', description: 'Build aborted'
            echo 'Pipeline was aborted.'
        }
        failure {
            githubNotify context: 'jenkins/rune-ci', status: 'FAILURE', description: 'Build failed'
            echo 'Pipeline failed. Check console log and test reports.'
        }
        always {
            cleanWs()
        }
    }
}
