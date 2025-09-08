pipeline {
    agent none
    tools { maven 'Maven'; jdk 'jdk11' }
    options { timestamps() }

    environment {
        MAVEN_FLAGS      = '-B -U -DskipTests'
        EAR_NAME         = 'savia-ear.ear'
        TAR_NAME         = 'savia-build.tar.gz'
        DEPLOY_DIR       = 'C:\\deployments'            // Carpeta de despliegue en Windows
        WILDFLY_HOME_WIN = 'C:\\wildfly-19.1.0.Final'
        WF_MANAGEMENT_PORT = 9990
    }

    stages {
        stage('Checkout & Build') {
            agent { label 'linux' }
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
                stash includes: 'savia-ear/target/savia-build.tar.gz', name: 'ear-tar'
                archiveArtifacts artifacts: 'savia-ear/target/savia-build.tar.gz', fingerprint: true
            }
        }

        stage('Deploy & Start WildFly') {
            agent { label 'windows' }
            steps {
                unstash 'ear-tar'

                script {
                    // Detener WildFly si está corriendo
                    bat """
                    echo [DEPLOY] Deteniendo WildFly si está corriendo...
                    for /F "tokens=5" %p in ('netstat -ano ^| findstr :${WF_MANAGEMENT_PORT}') do (
                        echo [WILDFLY] Matando PID %p...
                        taskkill /F /PID %p
                    )
                    """

                    // Limpiar carpeta deployments
                    bat """
                    echo [DEPLOY] Limpiando carpeta de deployments...
                    del /Q "${DEPLOY_DIR}\\*" 2>nul || echo "No había archivos"
                    """

                    // Copiar TAR
                    bat """
                    echo [DEPLOY] Copiando TAR a deployments...
                    copy /Y "%WORKSPACE%\\savia-ear\\target\\${TAR_NAME}" "${DEPLOY_DIR}\\${TAR_NAME}"
                    """

                    // Descomprimir TAR
                    bat """
                    echo [DEPLOY] Descomprimiendo TAR en deployments...
                    powershell -Command "Expand-Archive -Force '${DEPLOY_DIR}\\${TAR_NAME}' -DestinationPath '${DEPLOY_DIR}'"
                    del /Q "${DEPLOY_DIR}\\${TAR_NAME}"
                    """

                    // Iniciar WildFly en background
                    bat """
                    echo [WILDFLY] Iniciando standalone en background...
                    start "" /B "${WILDFLY_HOME_WIN}\\bin\\standalone.bat" -b 0.0.0.0 -bmanagement 0.0.0.0
                    """

                    // Esperar a que WildFly esté listo en puerto de management
                    waitUntil {
                        return bat(
                            script: """
                            powershell -Command "
                            try {
                                \$c = New-Object Net.Sockets.TcpClient('localhost', ${WF_MANAGEMENT_PORT})
                                if (\$c.Connected) { \$c.Close(); exit 0 } else { exit 1 }
                            } catch { exit 1 }"
                            """,
                            returnStatus: true
                        ) == 0
                    }

                    // Tocar archivo .dodeploy para que WildFly despliegue
                    bat """
                    echo [DEPLOY] Forzando despliegue...
                    copy /Y "${DEPLOY_DIR}\\${EAR_NAME}" "${DEPLOY_DIR}\\${EAR_NAME}.dodeploy" >nul
                    """
                }
            }
        }
    }

    post {
        success { echo "? Build compilado y desplegado en WildFly, servidor levantado correctamente." }
        failure { echo "? Falló el proceso." }
    }
}
