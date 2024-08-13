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
        PORT = '4444'
        LOCAL_IMAGE_PATH = 'my-spring-boot-app.tar'
        REMOTE_IMAGE_PATH = '/tmp/my-spring-boot-app.tar'
        PUBLIC_IP = '34.172.201.30'
        GITHUB_CREDENTIALS_ID = 'github-pat'
    }
    stages {
        stage('Checkout') {
            steps {
                retry(3) {
                    script {
                        def gitRepoUrl = 'https://github.com/raajh/my-hello-springboot-app.git'
                        sh "curl --head ${gitRepoUrl} | grep 'HTTP/'"
                        git url: gitRepoUrl, branch: 'master', credentialsId: "${GITHUB_CREDENTIALS_ID}"
                        sh 'git rev-parse HEAD'
                    }
                }
            }
        }
 
        stage('List Files') {
            steps {
                script {
                    sh 'ls -l'
                }
            }
        }
 
        stage('Build') {
            steps {
                script {
                    sh 'mvn clean package'
                }
            }
        }
 
        stage('Test') {
            steps {
                script {
                    sh 'mvn test'
                }
            }
        }
 
        stage('Build Docker Image') {
            steps {
                script {
                    retry(3) {
                        try {
                            sh 'echo Building Docker image...'
                            sh "docker build --network=host -t ${IMAGE_NAME}:latest ."
                            sh "docker images ${IMAGE_NAME} --format '{{.Tag}}'"
                        } catch (Exception e) {
                            error "Docker build failed: ${e.getMessage()}"
                        }
                    }
                }
            }
        }
 
        stage('Save Docker Image') {
            steps {
                script {
                    try {
                        sh "docker save -o ${LOCAL_IMAGE_PATH} ${IMAGE_NAME}:latest"
                        echo 'Docker image saved'
                    } catch (Exception e) {
                        error "Saving Docker image failed: ${e.getMessage()}"
                    }
                }
            }
        }
 
        stage('Login to GCP') {
            steps {
                script {
                    try {
                        sh 'gcloud auth activate-service-account --key-file=$GOOGLE_APPLICATION_CREDENTIALS'
                        sh 'gcloud config set project $PROJECT_ID'
                        echo 'Authenticated with GCP'
                    } catch (Exception e) {
                        error "GCP authentication failed: ${e.getMessage()}"
                    }
                }
            }
        }
 
        stage('Ensure VM Exists') {
            steps {
                script {
                    try {
                        def instanceExists = sh(
                            script: "gcloud compute instances describe ${INSTANCE_NAME} --zone=${ZONE} --project=${PROJECT_ID}",
                            returnStatus: true
                        )
                        if (instanceExists != 0) {
                            echo 'VM instance does not exist. Creating VM instance...'
                            sh """
                                gcloud compute instances create ${INSTANCE_NAME} \
                                    --zone=${ZONE} \
                                    --project=${PROJECT_ID} \
                                    --machine-type=e2-medium \
                                    --image-family=debian-10 \
                                    --image-project=debian-cloud
                            """
                            sh """
                                gcloud compute firewall-rules create allow-${PORT} \
                                    --allow tcp:${PORT} \
                                    --network default \
                                    --source-ranges=0.0.0.0/0 \
                                    --description="Allow port ${PORT} access"
                            """
                        } else {
                            echo 'VM instance already exists.'
                        }
                    } catch (Exception e) {
                        error "VM creation failed: ${e.getMessage()}"
                    }
                }
            }
        }
 
        stage('Transfer Docker Image to GCE') {
            steps {
                script {
                    try {
                        sh """
                            export CLOUDSDK_CORE_HTTP_TIMEOUT=600
                            gcloud compute scp ${LOCAL_IMAGE_PATH} ${INSTANCE_NAME}:${REMOTE_IMAGE_PATH} --zone=${ZONE} --project=${PROJECT_ID}
                        """
                        echo 'Docker image transferred to GCE VM'
                    } catch (Exception e) {
                        error "Image transfer to GCE failed: ${e.getMessage()}"
                    }
                }
            }
        }
 
        stage('Deploy Docker Image on GCE') {
            steps {
                script {
                    try {
                        // Load Docker image
                        def loadImageOutput = sh(script: "gcloud compute ssh ${INSTANCE_NAME} --zone=${ZONE} --command \"sudo docker load -i ${REMOTE_IMAGE_PATH}\"", returnStdout: true).trim()
                        echo "Docker image load output:\n${loadImageOutput}"
 
                        // Stop existing containers
                        def stopContainersOutput = sh(script: "gcloud compute ssh ${INSTANCE_NAME} --zone=${ZONE} --command \"sudo docker stop \$(sudo docker ps -q) || true\"", returnStdout: true).trim()
                        echo "Stop containers output:\n${stopContainersOutput}"
 
                        // Remove stopped containers
                        def removeContainersOutput = sh(script: "gcloud compute ssh ${INSTANCE_NAME} --zone=${ZONE} --command \"sudo docker rm \$(sudo docker ps -a -q) || true\"", returnStdout: true).trim()
                        echo "Remove containers output:\n${removeContainersOutput}"
 
                        // Run new container
                        def runContainerOutput = sh(script: "gcloud compute ssh ${INSTANCE_NAME} --zone=${ZONE} --command \"sudo docker run -d -p ${PORT}:${PORT} ${IMAGE_NAME}:latest\"", returnStdout: true).trim()
                        echo "Run container output:\n${runContainerOutput}"
 
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
