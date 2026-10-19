@Library('wallet-lib') _

pipeline {
    agent { label 'linux && docker' }

    options {
        timestamps()
        timeout(time: 60, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '20'))
        disableConcurrentBuilds()
    }

    parameters {
        choice(name: 'ACTION', choices: ['build', 'rollback'],
               description: 'build: build + deploy jenkins-staging; rollback: quay ve phien ban truoc')
        string(name: 'ROLLBACK_TAG', defaultValue: '',
               description: 'Tag can rollback; bo trong = previous_tag tren server')
        booleanParam(name: 'SKIP_DEPLOY', defaultValue: false, description: 'Chi build, khong deploy')
    }

    environment {
        IMAGE      = 'ghcr.io/parzival1405/wallet-service'
        DEPLOY_DIR = '/opt/wallet/jenkins-staging'
        APP_URL    = 'http://localhost:8083'
    }

    stages {
        stage('Info') {
            steps {
                script {
                    env.IMAGE_TAG = 'jenkins-' + sh(script: 'git rev-parse --short=7 HEAD', returnStdout: true).trim()
                }
                echo "ACTION=${params.ACTION} | ${env.IMAGE}:${env.IMAGE_TAG} | nhanh ${env.BRANCH_NAME}"
            }
        }

        stage('Build & Unit test') {
            when { expression { params.ACTION == 'build' } }
            steps {
                mavenBuild(goals: 'clean package')
            }
        }

        stage('Verify') {
            when { expression { params.ACTION == 'build' } }
            parallel {
                stage('Integration test') {
                    steps {
                        mavenBuild(goals: 'failsafe:integration-test failsafe:verify')
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
            when { allOf { branch 'main'; expression { params.ACTION == 'build' } } }
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
            post { always { sh 'docker logout ghcr.io || true' } }
        }

        stage('Approve') {
            when { allOf { branch 'main'; expression { params.ACTION == 'build' && !params.SKIP_DEPLOY } } }
            steps {
                timeout(time: 15, unit: 'MINUTES') {
                    input message: "Deploy ${env.IMAGE_TAG} len jenkins-staging?", ok: 'Deploy'
                }
            }
        }

        stage('Deploy jenkins-staging') {
            when { allOf { branch 'main'; expression { params.ACTION == 'build' && !params.SKIP_DEPLOY } } }
            steps {
                deployWithScript(credentialsId: 'deploy-ssh', host: 'localhost',
                                 dir: env.DEPLOY_DIR, tag: env.IMAGE_TAG)
            }
        }

        stage('Rollback') {
            when { allOf { branch 'main'; expression { params.ACTION == 'rollback' } } }
            steps {
                rollbackWithScript(credentialsId: 'deploy-ssh', host: 'localhost',
                                   dir: env.DEPLOY_DIR, tag: params.ROLLBACK_TAG)
            }
        }

        stage('Smoke test') {
            when {
                allOf {
                    branch 'main'
                    expression { params.ACTION == 'rollback' || !params.SKIP_DEPLOY }
                }
            }
            steps {
                sh './scripts/smoke-test.sh "$APP_URL"'
            }
            post {
                failure {
                    echo 'Smoke test fail -> rollback'
                    rollbackWithScript(credentialsId: 'deploy-ssh', host: 'localhost', dir: env.DEPLOY_DIR)
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
