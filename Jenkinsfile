pipeline {
    agent { label 'linux && docker' }

    options {
        timestamps()
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '20'))
        disableConcurrentBuilds()
    }

    stages {
        stage('Info') {
            steps {
                sh '''
                    echo "Nhanh: ${BRANCH_NAME:-khong-ro} | Commit: ${GIT_COMMIT} | Build: ${BUILD_NUMBER}"
                    ./mvnw -v
                '''
            }
        }

        stage('Build & Test') {
            steps {
                sh './mvnw -B -ntp clean verify'
            }
            post {
                always {
                    junit allowEmptyResults: true,
                          testResults: 'target/surefire-reports/*.xml, target/failsafe-reports/*.xml'
                }
            }
        }

        stage('Archive') {
            when { branch 'main' }
            steps {
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }
    }

    post {
        success {
            echo "Build #${env.BUILD_NUMBER} thanh cong"
        }
        failure {
            echo 'Build that bai: xem Console Output va Test Result'
        }
        cleanup {
            deleteDir()
        }
    }
}
