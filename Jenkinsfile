pipeline {
    agent { label 'Linux' }

    tools {
        maven 'Maven'
        jdk 'jdk11'
    }

    environment {
        WILDFLY_HOME = "${WORKSPACE}/wildfly-19.1.0.Final"
        DEPLOYMENTS  = "${WILDFLY_HOME}/standalone/deployments"
        EAR_NAME     = 'savia-ear.ear'
    }

    stages {
        stage('Checkout SCM') {
            steps {
                checkout scm
            }
        }

        stage('Build Modules') {
            steps {
                script {
                    try {
                        echo '? Compilando módulos...'
                        sh "mvn -f \"${WORKSPACE}/savia-ejb/pom.xml\" clean install -DskipTests"
                        sh "mvn -f \"${WORKSPACE}/savia-negocio/pom.xml\" clean install -DskipTests"
                        sh "mvn -f \"${WORKSPACE}/savia-web/pom.xml\" clean install -DskipTests"
                        sh "mvn -f \"${WORKSPACE}/savia-ear/pom.xml\" clean package -DskipTests"

                        def generatedEar = sh(script: "ls ${WORKSPACE}/savia-ear/target/*.ear | head -n 1", returnStdout: true).trim()
                        if (generatedEar) {
                            sh "mv ${generatedEar} ${WORKSPACE}/savia-ear/target/${EAR_NAME}"
                            echo '? EAR renombrado a ${EAR_NAME}'
                        } else {
                            error "No se encontró el EAR generado en target"
                        }

                        echo '? Compilación completa'
                    } catch (err) {
                        error "? ERROR compilando módulos: ${err}"
                    }
                }
            }
        }

        stage('Stop WildFly') {
            steps {
                script {
                    // Detener WildFly si está corriendo
                    sh """
                        if [ -f "${WILDFLY_HOME}/standalone/tmp" ]; then
                            ${WILDFLY_HOME}/bin/jboss-cli.sh --connect command=:shutdown || true
                            sleep 5
                            echo '? WildFly detenido.'
                        fi
                    """
                }
            }
        }

        stage('Deploy EAR') {
            steps {
                script {
                    try {
                        echo "?? Limpiando carpeta de despliegues..."
                        sh "mkdir -p ${DEPLOYMENTS}"
                        sh "rm -rf ${DEPLOYMENTS}/*"

                        echo '? Copiando EAR generado...'
                        sh """
                            if [ -f "${WORKSPACE}/savia-ear/target/${EAR_NAME}" ]; then
                                cp "${WORKSPACE}/savia-ear/target/${EAR_NAME}" "${DEPLOYMENTS}/"
                                echo '? EAR copiado correctamente.'
                            else
                                echo 'No se encontró el EAR renombrado'
                                exit 1
                            fi
                        """
                    } catch (e) {
                        error "? Error en despliegue: ${e.message}"
                    }
                }
            }
        }

        stage('Unblock WildFly files') {
            steps {
                sh """
                    chmod +x ${WILDFLY_HOME}/bin/*.sh
                    echo '? Scripts de WildFly marcados como ejecutables.'
                """
            }
        }

        stage('Start WildFly') {
            steps {
                sh "nohup ${WILDFLY_HOME}/bin/standalone.sh -b 0.0.0.0 > /dev/null 2>&1 &"
                echo '? WildFly iniciado.'
            }
        }

        stage('Verify Deployment') {
            steps {
                script {
                    def logPath = "${WILDFLY_HOME}/standalone/log/server.log"

                    timeout(time: 120, unit: 'SECONDS') { // aumenta timeout si WildFly es pesado
                        waitUntil {
                            def exists = sh(script: "[ -f ${logPath} ] && echo 'yes' || echo 'no'", returnStdout: true).trim()
                            if (exists == 'yes') {
                                def logContent = sh(script: "tail -n 200 ${logPath}", returnStdout: true).trim()
                                if (logContent.contains("Deployed \"${EAR_NAME}\"")) {
                                    echo '? EAR desplegado correctamente en WildFly.'
                                    return true
                                }
                                if (logContent.contains(".failed")) {
                                    error "? El despliegue de ${EAR_NAME} falló. Revisa server.log."
                                }
                            }
                            sleep 5 // espera 5 segundos antes de volver a comprobar
                            return false
                        }
                    }
                }
            }
        }
    }
}