def setGitHubStatus(String state, String description) {
    String commitSha = env.GIT_COMMIT?.trim()
    if (!commitSha) {
        commitSha = sh(returnStdout: true, script: 'git rev-parse HEAD').trim()
    }

    String repoUrl = sh(returnStdout: true, script: 'git config --get remote.origin.url').trim()
    if (!repoUrl) {
        echo 'Skipping GitHub status update: could not resolve remote.origin.url'
        return
    }

    step([
        $class: 'GitHubCommitStatusSetter',
        reposSource: [$class: 'ManuallyEnteredRepositorySource', url: repoUrl],
        commitShaSource: [$class: 'ManuallyEnteredShaSource', sha: commitSha],
        contextSource: [$class: 'ManuallyEnteredCommitContextSource', context: 'jenkins/rune-ci'],
        statusBackrefSource: [$class: 'ManuallyEnteredBackrefSource', backref: env.BUILD_URL ?: ''],
        errorHandlers: [[$class: 'ShallowAnyErrorHandler']],
        statusResultSource: [
            $class: 'ConditionalStatusResultSource',
            results: [[$class: 'AnyBuildResult', state: state, message: description]]
        ]
    ])
}

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
                script {
                    setGitHubStatus('PENDING', 'Build started')
                }
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
            script {
                setGitHubStatus('SUCCESS', 'Build succeeded')
            }
            echo 'Build, tests, packaging completed successfully.'
        }
        unstable {
            script {
                setGitHubStatus('FAILURE', 'Build unstable')
            }
            echo 'Pipeline is unstable. Check test reports.'
        }
        aborted {
            script {
                setGitHubStatus('ERROR', 'Build aborted')
            }
            echo 'Pipeline was aborted.'
        }
        failure {
            script {
                setGitHubStatus('FAILURE', 'Build failed')
            }
            echo 'Pipeline failed. Check console log and test reports.'
        }
        always {
            cleanWs()
        }
    }
}
