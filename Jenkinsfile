pipeline {
    agent { label 'Linux' }   // Solo corre en agentes Linux

    tools { maven 'Maven'; jdk 'jdk11' }
    options { timestamps() }

    environment {
        MAVEN_FLAGS       = '-B -U -DskipTests'
        EAR_NAME          = 'savia-ear.ear'
        TAR_NAME          = 'savia-build.tar.gz'
        DEPLOY_DIR        = '/opt/deployments'               // Carpeta compartida con Windows
        WILDFLY_HOME_WIN  = 'C:\\wildfly-19.1.0.Final'
        WF_MANAGEMENT_PORT = '9990'                          // Puerto de administración para check
    }

    stages {
        stage('Checkout & Build') {
            steps {
                sh '''
                    set -e
                    echo "[BUILD] Compilando módulos..."
                    mvn -f "$WORKSPACE/savia-negocio/pom.xml" clean install ${MAVEN_FLAGS}
                    mvn -f "$WORKSPACE/savia-ejb/pom.xml"     clean install ${MAVEN_FLAGS}
                    mvn -f "$WORKSPACE/savia-web/pom.xml"     clean install ${MAVEN_FLAGS}
                    mvn -f "$WORKSPACE/savia-ear/pom.xml"     clean package ${MAVEN_FLAGS}

                    # Copiar EAR con nombre fijo
                    EAR=$(ls "$WORKSPACE/savia-ear/target/"*.ear | head -n1)
                    cp -f "$EAR" "$WORKSPACE/savia-ear/target/${EAR_NAME}"

                    echo "[BUILD] Generando TAR..."
                    cd "$WORKSPACE/savia-ear/target"
                    tar -czf "${TAR_NAME}" "${EAR_NAME}"
                '''
                archiveArtifacts artifacts: 'savia-ear/target/savia-build.tar.gz', fingerprint: true
            }
        }

        stage('Detener WildFly si está corriendo en Windows') {
            agent { label 'windows' }
            steps {
                bat """
                    echo [WILDFLY] Buscando PID de WildFly en puerto ${WF_MANAGEMENT_PORT}...
                    for /f "tokens=5" %%p in ('netstat -ano ^| findstr :${WF_MANAGEMENT_PORT}') do (
                        echo [WILDFLY] Matando PID %%p...
                        taskkill /F /PID %%p
                    )
                """
            }
        }

        stage('Iniciar WildFly en Windows') {
            agent { label 'windows' }
            steps {
                bat """
                    echo [WILDFLY] Iniciando standalone en background...
                    start "" /B "${WILDFLY_HOME_WIN}\\bin\\standalone.bat" -b 0.0.0.0 -bmanagement 0.0.0.0
                """

                script {
                    // Espera hasta que WildFly esté listo en puerto de management
                    waitUntil {
                        bat returnStatus: true, script: """
                            powershell -Command "
                            try {
                                \$c = New-Object Net.Sockets.TcpClient('localhost', ${WF_MANAGEMENT_PORT})
                                if (\$c.Connected) { \$c.Close(); exit 0 } else { exit 1 }
                            } catch { exit 1 }
                            "
                        """ == 0
                    }
                }
            }
        }

        stage('Desplegar en WildFly') {
            steps {
                sh '''
                    set -e
                    echo "[DEPLOY] Limpiando carpeta de deployments..."
                    rm -rf ${DEPLOY_DIR:?}/*

                    echo "[DEPLOY] Copiando TAR a deployments..."
                    cp "$WORKSPACE/savia-ear/target/${TAR_NAME}" "${DEPLOY_DIR}/${TAR_NAME}"

                    echo "[DEPLOY] Descomprimiendo TAR en deployments..."
                    tar -xzf "${DEPLOY_DIR}/${TAR_NAME}" -C "${DEPLOY_DIR}"

                    echo "[DEPLOY] Limpiando TAR después de extraer..."
                    rm -f "${DEPLOY_DIR}/${TAR_NAME}"

                    echo "[DEPLOY] Forzando scanner de WildFly..."
                    touch "${DEPLOY_DIR}/${EAR_NAME}.dodeploy"
                '''
            }
        }
    }

    post {
        success { echo "? Build compilado y desplegado en WildFly (WildFly corriendo en background)." }
        failure { echo "? Falló el proceso." }
    }
}
