pipeline {
    agent none
    tools { maven 'Maven'; jdk 'jdk11' }
    options { timestamps() }

    environment {
        MAVEN_FLAGS      = '-B -U -DskipTests'
        EAR_NAME         = 'savia-ear.ear'
        TAR_NAME         = 'savia-build.tar.gz'
        DEPLOY_DIR       = 'C:\\deployments'
        WILDFLY_HOME_WIN = 'C:\\wildfly-19.1.0.Final'
        WF_MANAGEMENT_PORT = 9990
        WILDFLY_LOG      = 'C:\\deployments\\wildfly.log'
    }

    stages {
        stage('Checkout & Build') {
            agent { label 'Linux' }
            steps {
                sh '''
                    set -e
                    echo "[BUILD] Compilando módulos..."
                    mvn -f "$WORKSPACE/savia-negocio/pom.xml" clean install ${MAVEN_FLAGS}
                    mvn -f "$WORKSPACE/savia-ejb/pom.xml"     clean install ${MAVEN_FLAGS}
                    mvn -f "$WORKSPACE/savia-web/pom.xml"     clean install ${MAVEN_FLAGS}
                    mvn -f "$WORKSPACE/savia-ear/pom.xml"     clean package ${MAVEN_FLAGS}

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
            agent { label 'Windows' }
            steps {
                unstash 'ear-tar'

                script {
                    // Detener WildFly si está corriendo
                    bat """
                    echo [DEPLOY] Deteniendo WildFly si está corriendo...
                    @echo off
                    setlocal enabledelayedexpansion
                    set found=0
                    for /F "tokens=5" %%p in ('netstat -ano ^| findstr :${WF_MANAGEMENT_PORT}') do (
                        set found=1
                        echo [WILDFLY] Matando PID %%p...
                        taskkill /F /PID %%p >nul 2>&1 || echo "No se pudo matar PID %%p"
                    )
                    if !found! EQU 0 echo "No había procesos en el puerto ${WF_MANAGEMENT_PORT}"
                    endlocal
                    """

                    // Limpiar carpeta deployments
                    bat """
                    echo [DEPLOY] Limpiando carpeta de deployments...
                    del /Q "${DEPLOY_DIR}\\*" 2>nul || echo "No había archivos"
                    """

                    // Validar que el TAR existe
                    bat """
                    if not exist "%WORKSPACE%\\savia-ear\\target\\${TAR_NAME}" (
                        echo "? TAR no encontrado, abortando..."
                        exit /B 1
                    )
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

                    // Iniciar WildFly en background con log
                    bat """
                    echo [WILDFLY] Iniciando standalone en background...
                    start "" /B "${WILDFLY_HOME_WIN}\\bin\\standalone.bat" -b 0.0.0.0 -bmanagement 0.0.0.0 > "${WILDFLY_LOG}" 2>&1
                    """

                    // Esperar a que WildFly esté listo (timeout 2 min)
                    waitUntil(initialRecurrencePeriod: 5000, timeout: 120000) {
                        bat(
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
        failure { echo "? Falló el proceso. Revisa ${WILDFLY_LOG} para detalles." }
    }
}
