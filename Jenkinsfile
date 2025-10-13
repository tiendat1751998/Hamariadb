pipeline {
    agent any // Jenkins agent cần có Docker daemon

    options {
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    environment {
        // --- Project Info ---
        APP_PROJECT = 'hamariadb'
        APP_CODE = 'Hamariadb'
        APP_REPO_URL = 'https://github.com/tiendat1751998/Hamariadb.git'
        APP_REPO_BRANCH = 'datdt'

        // --- Deploy Config ---
        DEPLOY_ENV = 'dev'
        DOCKER_NETWORK = 'root_default'
        SERVICE_PORT_PUBLISH = '9101'
        SERVICE_PORT_LOCAL = '8080'
        SERVICE_NAME = "${DEPLOY_ENV}-${APP_PROJECT}"

        SERVICE_ARGS = """
            -e SPRING_PROFILES_ACTIVE=${DEPLOY_ENV} 
            -e SERVER_PORT=${SERVICE_PORT_LOCAL} 
            -e SERVER_CONTEXT_PATH=/api
            -e DB_URL=\${DB_URL}
            -e DB_USERNAME=\${DB_USERNAME}
            -e DB_PASSWORD=\${DB_PASSWORD}
            -e JPA_DDL_AUTO=update
            -e JWT_SECRET=\${JWT_SECRET}
            -e SERVER_PRIVATE_KEY=\${SERVER_PRIVATE_KEY}
            -e SERVER_PUBLIC_KEY=\${SERVER_PUBLIC_KEY}
            -e TELEGRAM_BOT_TOKEN=\${TELEGRAM_BOT_TOKEN}
            -e TELEGRAM_ADMIN_CHAT_ID=\${TELEGRAM_ADMIN_CHAT_ID}
        """

        // --- Build Tool ---
        BUILD_IMAGE = 'maven:3.9-eclipse-temurin-17'
        BUILD_COMMAND = 'mvn clean package -DskipTests'
        TEST_COMMAND = 'mvn test'
        BUILD_CACHE = "devops-cache-maven-${APP_PROJECT}:/root/.m2"
        BUILD_CHECK_CMD = 'ls target/*.jar'

        HEALTH_CHECK_CMD = 'curl -f http://localhost:8080/api/actuator/health || exit 1'
    }

    stages {
        stage('Checkout') {
            steps {
                echo "📦 Checking out ${APP_REPO_BRANCH} from ${APP_REPO_URL}"
                checkout([
                    $class: 'GitSCM',
                    userRemoteConfigs: [[
                        url: env.APP_REPO_URL
                    ]],
                    branches: [[
                        name: "*/${env.APP_REPO_BRANCH}"
                    ]]
                ])
                sh 'ls -la && echo "--- Found POM:" && find . -name pom.xml'
            }
        }

        stage('Setup') {
            steps {
                script {
                    env.GIT_COMMIT_APP = sh(script: 'git rev-parse HEAD', returnStdout: true).trim()
                    env.GIT_SHORT_COMMIT_APP = env.GIT_COMMIT_APP[0..6]
                    env.DOCKER_IMAGE = "${env.APP_PROJECT}/${env.APP_CODE}".toLowerCase()
                    env.DOCKER_TAG = "${env.DEPLOY_ENV}-${env.GIT_SHORT_COMMIT_APP}"
                    env.DOCKER_BUILDER_NAME = "builder-${env.SERVICE_NAME}-${BUILD_NUMBER}"
                    currentBuild.displayName = "#${BUILD_NUMBER} - ${env.GIT_SHORT_COMMIT_APP}"
                }
            }
        }

        stage('Build Code') {
            steps {
                milestone(label: "Milestone: Build")
                timeout(time: 15, unit: 'MINUTES') {
                    echo "⚙️ Building with Maven inside Docker..."
                    sh """
                        docker run --rm --name ${env.DOCKER_BUILDER_NAME} \
                            -v "${WORKSPACE}:/app" \
                            -v ${env.BUILD_CACHE} \
                            -w /app \
                            ${env.BUILD_IMAGE} ${env.BUILD_COMMAND}
                    """

                    echo "🔍 Checking for build artifact..."
                    sh """
                        docker run --rm --name ${env.DOCKER_BUILDER_NAME}-check \
                            -v "${WORKSPACE}:/app" \
                            -w /app \
                            ${env.BUILD_IMAGE} ${env.BUILD_CHECK_CMD}
                    """
                }
            }
        }

        stage('Analysis') {
            parallel {
                stage('Unit Tests') {
                    steps {
                        timeout(time: 10, unit: 'MINUTES') {
                            echo "🧪 Running unit tests..."
                            sh """
                                docker run --rm --name ${env.DOCKER_BUILDER_NAME}-test \
                                    -v "${WORKSPACE}:/app" \
                                    -v ${env.BUILD_CACHE} \
                                    -w /app \
                                    ${env.BUILD_IMAGE} ${env.TEST_COMMAND}
                            """
                        }
                    }
                }
            }
        }

        stage('Build Container') {
            steps {
                milestone(label: "Milestone: Docker Build")
                timeout(time: 10, unit: 'MINUTES') {
                    echo "🐳 Building Docker image ${env.DOCKER_IMAGE}:${env.DOCKER_TAG}"
                    sh "docker build -t ${env.DOCKER_IMAGE}:${env.DOCKER_TAG} -t ${env.DOCKER_IMAGE}:latest-${env.DEPLOY_ENV} ."
                }
            }
        }

        stage('Deploy') {
            steps {
                script {
                    withCredentials([
                        string(credentialsId: 'db-url-dev', variable: 'DB_URL'),
                        string(credentialsId: 'db-username-dev', variable: 'DB_USERNAME'),
                        string(credentialsId: 'db-password-dev', variable: 'DB_PASSWORD'),
                        string(credentialsId: 'jwt-secret-dev', variable: 'JWT_SECRET'),
                        string(credentialsId: 'server-private-key-dev', variable: 'SERVER_PRIVATE_KEY'),
                        string(credentialsId: 'server-public-key-dev', variable: 'SERVER_PUBLIC_KEY'),
                        string(credentialsId: 'telegram-bot-token-dev', variable: 'TELEGRAM_BOT_TOKEN'),
                        string(credentialsId: 'telegram-admin-chat-id-dev', variable: 'TELEGRAM_ADMIN_CHAT_ID')
                    ]) {
                        lock(resource: "deploy-${env.SERVICE_NAME}", inversePrecedence: true) {
                            milestone(label: "Milestone: Deploy")
                            echo "🚀 Deploying ${env.DOCKER_IMAGE}:${env.DOCKER_TAG}"
                            timeout(time: 5, unit: 'MINUTES') {
                                sh "docker stop ${env.SERVICE_NAME} || true"
                                sh "docker rm ${env.SERVICE_NAME} || true"
                                sh """
                                    docker run -d --network ${env.DOCKER_NETWORK} \
                                        --name ${env.SERVICE_NAME} \
                                        -p ${env.SERVICE_PORT_PUBLISH}:${env.SERVICE_PORT_LOCAL} \
                                        ${env.SERVICE_ARGS} \
                                        ${env.DOCKER_IMAGE}:${env.DOCKER_TAG}
                                """
                            }
                        }
                    }
                }
            }
        }

        stage('Health Check') {
            steps {
                timeout(time: 3, unit: 'MINUTES') {
                    waitUntil(initialRecurrencePeriod: 5000, quiet: false) {
                        script {
                            echo "🔎 Performing health check..."
                            def result = sh(script: "docker exec ${env.SERVICE_NAME} bash -c '${HEALTH_CHECK_CMD}'", returnStatus: true)
                            return result == 0
                        }
                    }
                    echo "✅ Health check passed!"
                }
            }
        }
    }

    post {
        always {
            echo '🏁 Pipeline finished.'
            cleanWs()
        }
        success {
            echo '✅ SUCCESS!'
        }
        failure {
            echo '❌ FAILURE!'
        }
    }
}
