pipeline {
    agent any

    environment {
        GOOGLE_APPLICATION_CREDENTIALS = credentials('gcp-service-account-key')
        PROJECT_ID = 'ds-ms-microservices'
        IMAGE_NAME = 'my-spring-boot-app'
        DOCKERHUB_USERNAME = 'ganshekar'
        DOCKERHUB_CREDENTIALS_ID = 'dockerhub-credentials'
        INSTANCE_NAME = 'instance-2'
        ZONE = 'us-central1-c'
        PORT = '8080'
        LOCAL_IMAGE_PATH = 'my-spring-boot-app.tar'
        REMOTE_IMAGE_PATH = '/tmp/my-spring-boot-app.tar'
        GCR_IMAGE = "gcr.io/${PROJECT_ID}/${IMAGE_NAME}"
    }

    stages {
        stage('Checkout') {
            steps {
                script {
                    def gitRepoUrl = 'https://github.com/raajh/my-hello-springboot-app.git'
                    bat "curl --head ${gitRepoUrl} | findstr /R /C:\"HTTP/\""
                    git url: gitRepoUrl, branch: 'master'
                }
            }
        }

        stage('Build') {
            steps {
                script {
                    bat 'mvn clean package'
                }
            }
        }

        stage('Test') {
            steps {
                script {
                    bat 'mvn test'
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    retry(3) {
                        try {
                            bat 'echo Building Docker image...'
                            bat "docker build --network=host -t ${GCR_IMAGE}:latest ."
                        } catch (Exception e) {
                            error "Docker build failed: ${e.getMessage()}"
                        }
                    }
                }
            }
        }

        stage('Push Docker Image to GCR') {
            steps {
                script {
                    try {
                        bat "docker push ${GCR_IMAGE}:latest"
                        echo 'Docker image pushed to GCR'
                    } catch (Exception e) {
                        error "Pushing Docker image to GCR failed: ${e.getMessage()}"
                    }
                }
            }
        }

        stage('Login to GCP') {
            steps {
                script {
                    try {
                        bat 'gcloud auth activate-service-account --key-file=%GOOGLE_APPLICATION_CREDENTIALS%'
                        bat 'gcloud config set project %PROJECT_ID%'
                        bat 'gcloud auth configure-docker'
                        echo 'Authenticated with GCP and configured Docker for GCR'
                    } catch (Exception e) {
                        error "GCP authentication failed: ${e.getMessage()}"
                    }
                }
            }
        }

        stage('Deploy Docker Image on GCE') {
            steps {
                script {
                    try {
                        bat '''
                            gcloud compute ssh %INSTANCE_NAME% --zone=%ZONE% --command "sudo docker pull %GCR_IMAGE%:latest"
                            gcloud compute ssh %INSTANCE_NAME% --zone=%ZONE% --command "sudo docker run -d -p %PORT%:%PORT% %GCR_IMAGE%:latest"
                        '''
                        echo 'Deployment to GCE completed'
                    } catch (Exception e) {
                        error "GCE deployment failed: ${e.getMessage()}"
                    }
                }
            }
        }
    }

    post {
        success {
            echo 'Pipeline completed successfully!'
        }
        failure {
            echo 'Pipeline failed.'
        }
        always {
            cleanWs()
        }
    }
}
