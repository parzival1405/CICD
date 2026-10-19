pipeline {
    agent { label 'linux && docker' }

    options {
        timestamps()
        timeout(time: 40, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '20'))
        disableConcurrentBuilds()
    }

    parameters {
        choice(name: 'ACTION', choices: ['build', 'rollback'],
               description: 'build: build + kiem tra (+ deploy o Bai 04); rollback: quay ve phien ban truoc')
        string(name: 'ROLLBACK_TAG', defaultValue: '',
               description: 'Tag can rollback; bo trong = previous_tag tren server')
        booleanParam(name: 'SKIP_DEPLOY', defaultValue: false, description: 'Chi build, khong deploy')
    }

    environment {
        IMAGE = 'ghcr.io/parzival1405/wallet-service'
    }

    stages {
        stage('Info') {
            steps {
                script {
                    env.IMAGE_TAG = 'jenkins-' + sh(script: 'git rev-parse --short=7 HEAD', returnStdout: true).trim()
                }
                echo "ACTION=${params.ACTION} | IMAGE=${env.IMAGE}:${env.IMAGE_TAG} | Nhanh=${env.BRANCH_NAME}"
            }
        }

        stage('Build & Unit test') {
            when { expression { params.ACTION == 'build' } }
            steps {
                sh './mvnw -B -ntp clean package'
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Verify') {
            when { expression { params.ACTION == 'build' } }
            parallel {
                stage('Integration test') {
                    steps {
                        sh './mvnw -B -ntp failsafe:integration-test failsafe:verify'
                    }
                    post {
                        always {
                            junit allowEmptyResults: true, testResults: 'target/failsafe-reports/*.xml'
                        }
                    }
                }
                stage('Secret scan') {
                    steps {
                        sh 'docker run --rm -v "$PWD:/repo:ro" zricethezav/gitleaks:v8.18.4 detect --no-git --source /repo --no-banner --redact -v'
                    }
                }
                stage('Docker build') {
                    steps {
                        sh 'docker build -t "$IMAGE:$IMAGE_TAG" .'
                    }
                }
            }
        }

        stage('Push image') {
            when {
                allOf {
                    branch 'main'
                    expression { params.ACTION == 'build' }
                }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: 'ghcr-cred',
                                                  usernameVariable: 'REG_USER',
                                                  passwordVariable: 'REG_TOKEN')]) {
                    sh '''
                        echo "$REG_TOKEN" | docker login ghcr.io -u "$REG_USER" --password-stdin
                        docker push "$IMAGE:$IMAGE_TAG"
                    '''
                }
            }
            post {
                always {
                    sh 'docker logout ghcr.io || true'
                }
            }
        }
    }

    post {
        cleanup {
            sh 'docker image rm "$IMAGE:$IMAGE_TAG" 2>/dev/null || true'
            deleteDir()
        }
    }
}
