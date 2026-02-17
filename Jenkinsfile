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
            echo 'Build, tests, packaging completed successfully.'
        }
        failure {
            echo 'Pipeline failed. Check console log and test reports.'
        }
        always {
            cleanWs()
        }
    }
}
