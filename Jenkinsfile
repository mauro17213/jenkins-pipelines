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
    }
 
stage('Deploy & Start WildFly') {
    steps {
        withEnv(['PATH+MAVEN=C:\\Program Files\\Apache\\maven\\bin']) {

            // Detener WildFly si est� corriendo
            bat """
            echo [DEPLOY] Deteniendo WildFly si est� corriendo...
            netstat -ano | findstr ${WF_MANAGEMENT_PORT} >nul && (
                for /f "tokens=5" %%a in ('netstat -ano ^| findstr ${WF_MANAGEMENT_PORT}') do taskkill /PID %%a /F
            ) || echo "No hab�a procesos en el puerto ${WF_MANAGEMENT_PORT}"
            """

            // Limpiar carpeta de deployments
            bat """
            echo [DEPLOY] Limpiando carpeta de deployments...
            del /Q "${DEPLOY_DIR}\\*" 2>nul || echo "No hab�a archivos"
            """

            // Copiar TAR a deployments
            bat """
            echo [DEPLOY] Copiando TAR a deployments...
            copy /Y "C:\\Users\\stive\\Documents\\nodeWindosw\\workspace\\prueba\\savia-ear\\target\\savia-build.tar.gz" "${DEPLOY_DIR}\\savia-build.tar.gz"
            """

            // Descomprimir TAR
            bat """
            echo [DEPLOY] Descomprimiendo TAR en deployments...
            tar -xzf "${DEPLOY_DIR}\\savia-build.tar.gz" -C "${DEPLOY_DIR}"
            del /Q "${DEPLOY_DIR}\\savia-build.tar.gz"
            """

            // Iniciar WildFly en background
            bat """
            echo [WILDFLY] Iniciando standalone en background...
            start "" /B "${WILDFLY_HOME_WIN}\\bin\\standalone.bat" -b 0.0.0.0 -bmanagement 0.0.0.0
            """

            // Esperar a que WildFly est� listo usando solo bat
            bat """
            echo [DEPLOY] Esperando a que WildFly est� listo en el puerto ${WF_MANAGEMENT_PORT}...
            set /a count=0
            :checkPort
            for /f "tokens=5" %%a in ('netstat -ano ^| findstr ${WF_MANAGEMENT_PORT}') do (
                echo [DEPLOY] WildFly est� corriendo, PID=%%a
                goto :ready
            )
            set /a count+=1
            if %count% GEQ 60 (
                echo [ERROR] WildFly no arranc� despu�s de 5 minutos
                exit /b 1
            )
            timeout /t 5 >nul
            goto :checkPort
            :ready
            echo [DEPLOY] WildFly listo.
            """

            echo "[DEPLOY] WildFly listo y desplegado correctamente."
        }
    }
}


    }

    post {
        success { echo "? Build compilado y desplegado en WildFly, servidor levantado correctamente." }
        failure { echo "? Fall� el proceso." }
    }
}
