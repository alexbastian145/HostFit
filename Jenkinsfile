// Declarative pipeline: Maven build -> JUnit -> SonarQube -> Docker image.
// Jenkins needs: JDK 17 + Maven tools, the SonarQube Scanner plugin with a server named "SonarQube",
// and Docker available on the agent.
pipeline {
    agent any

    tools {
        maven 'Maven-3.9'
        jdk 'JDK-17'
    }

    options {
        timestamps()
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '15'))
    }

    environment {
        IMAGE = "hostfit"
        TAG   = "${env.BRANCH_NAME ?: 'local'}-${env.BUILD_NUMBER}".replaceAll('[^A-Za-z0-9_.-]', '-')
    }

    stages {
        stage('Checkout') {
            steps { checkout scm }
        }

        stage('Maven build') {
            steps { sh 'mvn -B -DskipTests clean package' }
        }

        stage('JUnit tests') {
            steps { sh 'mvn -B test' }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                    archiveArtifacts artifacts: 'target/site/jacoco/**', allowEmptyArchive: true
                }
            }
        }

        stage('SonarQube analysis') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh 'mvn -B sonar:sonar'
                }
            }
        }

        stage('Quality gate') {
            steps {
                timeout(time: 10, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Docker image') {
            steps {
                sh 'docker build -t ${IMAGE}:${TAG} -t ${IMAGE}:latest .'
            }
        }

        stage('Deploy (main only)') {
            when { branch 'main' }
            steps {
                sh '''
                    docker compose -p hostfit down || true
                    docker compose -p hostfit up -d --build
                '''
            }
        }
    }

    post {
        failure { echo 'Build failed. Check the failing stage above.' }
        cleanup { cleanWs() }
    }
}
