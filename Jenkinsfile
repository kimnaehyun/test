pipeline {
    agent any
    stages {
        stage('Checkout') {
            steps { checkout scm }
        }
        stage('Build') {
            steps {
                sh 'chmod +x gradlew'
                sh './gradlew clean build -x test'
            }
        }
        stage('Docker Deploy') {
            steps {
                sh 'docker-compose up -d --build'
            }
        }
    }
}
sadfsadf
sadfsadf
asdf
asdf
safd
