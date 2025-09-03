pipeline {
  agent none

  options { timestamps() }

  parameters {
    choice(name: 'DEPLOY_METHOD', choices: ['scanner', 'cli'], description: 'Método de despliegue en Windows')
    booleanParam(name: 'OPEN_BROWSER', defaultValue: true, description: 'Abrir navegador al final (Windows)')
  }

  tools {
    maven 'Maven'
    jdk   'jdk11'
  }

  environment {
    // Comunes
    EAR_NAME    = 'savia-ear.ear'
    MAVEN_FLAGS = '-B -U -DskipTests'

    // Windows / WildFly destino
    WILDFLY_HOME = 'C:\\wildfly-19.1.0.Final'
    DEPLOY_DIR   = 'C:\\wildfly-19.1.0.Final\\standalone\\deployments'
    MGMT_HOST    = '127.0.0.1'
    MGMT_PORT    = '9990'
    HTTP_HOST    = '127.0.0.1'
    HTTP_PORT    = '8080'
    APP_CTX      = 'savia'
  }

  stages {
    stage('Checkout SCM') {
      agent { label 'Linux' }
      steps { checkout scm }
    }

    stage('Build en Linux (negocio/ejb/web/ear)') {
      agent { label 'Linux' }
      steps {
        sh '''
          set -e
          echo "[BUILD] Compilando módulos en Linux..."
          mvn -f "$WORKSPACE/savia-negocio/pom.xml" clean install ${MAVEN_FLAGS}
          mvn -f "$WORKSPACE/savia-ejb/pom.xml"     clean install ${MAVEN_FLAGS}
          mvn -f "$WORKSPACE/savia-web/pom.xml"     clean install ${MAVEN_FLAGS}
          mvn -f "$WORKSPACE/savia-ear/pom.xml"     clean package ${MAVEN_FLAGS}

          EAR=$(ls "$WORKSPACE/savia-ear/target/"*.ear | head -n1 || true)
          [ -z "$EAR" ] && { echo "[BUILD] ! No se encontró .ear en savia-ear/target"; exit 1; }
          cp -f "$EAR" "$WORKSPACE/savia-ear/target/$EAR_NAME"
          echo "[BUILD] EAR listo: $EAR_NAME"

          # Deja solo el EAR renombrado para simplificar el unstash
          find "$WORKSPACE/savia-ear/target" -maxdepth 1 -type f -name "*.ear" ! -name "$EAR_NAME" -delete
        '''
        stash name: 'ear', includes: 'savia-ear/target/*.ear'
      }
    }

    stage('Preparar artefacto en Windows') {
      agent { label 'Windows' }
      steps {
        // Trae el EAR construido en Linux
        unstash 'ear'
        // Copia al directorio de despliegue con el nombre esperado
        bat """
        @echo on
        if not exist "%DEPLOY_DIR%" mkdir "%DEPLOY_DIR%"
        copy /Y "%WORKSPACE%\\savia-ear\\target\\*.ear" "%DEPLOY_DIR%\\%EAR_NAME%"
        echo [ARTIFACT] Copiado a %DEPLOY_DIR%\\%EAR_NAME%
        """
      }
    }

    stage('Start WildFly en Windows') {
      agent { label 'Windows' }
      steps {
        bat """
        @echo on
        setlocal ENABLEDELAYEDEXPANSION

        echo [WF] Verificando si el puerto %HTTP_PORT% está en uso...
        set PID=
        for /f "tokens=2 delims==" %%a in ('powershell -NoProfile -Command ^
          "$p = Get-NetTCPConnection -State Listen -LocalPort $env:HTTP_PORT -ErrorAction SilentlyContinue ^| Select-Object -First 1 -ExpandProperty OwningProcess; if($p){'PID=' + $p}"') do set PID=%%a

        if defined PID (
          echo [WF] Ya hay un proceso escuchando en %HTTP_PORT% (PID %PID%). Se asume WildFly iniciado.
        ) else (
          echo [WF] Iniciando WildFly en background...
          pushd "%WILDFLY_HOME%\\bin"
          start "WILDFLY" /MIN standalone.bat -b 0.0.0.0 -Djboss.bind.address.management=%MGMT_HOST% -Djboss.http.port=%HTTP_PORT%
          popd
        )

        echo [WF] Esperando interfaz de administración (%MGMT_HOST%:%MGMT_PORT%)...
        powershell -NoProfile -Command ^
          "$u='http://%MGMT_HOST%:%MGMT_PORT%/management'; 1..60 ^| ForEach-Object { ^
             try { $r=Invoke-WebRequest -Uri $u -UseBasicParsing -TimeoutSec 3; ^
                   if($r.StatusCode -in 200,401){ Write-Host 'Management UP'; exit 0 } } catch{}; ^
             Start-Sleep -Seconds 2 }; exit 1"
        """
      }
    }

    stage('Deploy en Windows') {
      agent { label 'Windows' }
      steps {
        script {
          if (params.DEPLOY_METHOD == 'cli') {
            bat """
            @echo on
            echo [DEPLOY-CLI] Desplegando por CLI...
            "%WILDFLY_HOME%\\bin\\jboss-cli.bat" --connect --timeout=60000 --command="deploy --force \\"%DEPLOY_DIR%\\\\%EAR_NAME%\\""
            if errorlevel 1 (
              echo [DEPLOY-CLI] Error en el deploy por CLI.
              powershell -NoProfile -Command "Get-Content -Path '%WILDFLY_HOME%\\standalone\\log\\server.log' -Tail 200"
              exit /b 1
            )
            """
          } else {
            bat """
            @echo on
            set "APP=%DEPLOY_DIR%\\%EAR_NAME%"
            echo [DEPLOY] Limpiando markers previos...
            del /f /q "%APP%.deployed" "%APP%.failed" "%APP%.undeployed" "%APP%.dodeploy" 2>nul

            echo [DEPLOY] Activando deploy (.dodeploy)...
            type nul > "%APP%.dodeploy"

            echo [DEPLOY] Esperando .deployed / .failed ...
            set /a COUNT=0
            :waitloop
            if exist "%APP%.deployed" goto deployed
            if exist "%APP%.failed"  goto failed
            timeout /t 2 >nul
            set /a COUNT+=1
            if %COUNT% GEQ 90 goto timeout
            goto waitloop

            :deployed
            echo [DEPLOY] ? Deploy OK (.deployed)
            exit /b 0

            :failed
            echo [DEPLOY] ? Deploy FAILED (.failed). Últimas líneas:
            powershell -NoProfile -Command "Get-Content -Path '%WILDFLY_HOME%\\standalone\\log\\server.log' -Tail 200"
            exit /b 1

            :timeout
            echo [DEPLOY] ? Timeout esperando markers. Últimas líneas:
            powershell -NoProfile -Command "Get-Content -Path '%WILDFLY_HOME%\\standalone\\log\\server.log' -Tail 200"
            exit /b 1
            """
          }
        }
      }
    }

    stage('Smoke test (Windows)') {
      agent { label 'Windows' }
      steps {
        bat """
        @echo on
        set "URL=http://%HTTP_HOST%:%HTTP_PORT%/%APP_CTX%/"
        echo [TEST] Request a %URL%
        powershell -NoProfile -Command "try{ $r=Invoke-WebRequest -Uri '%URL%' -UseBasicParsing -TimeoutSec 10; Write-Host ('[TEST] HTTP ' + $r.StatusCode); if($r.StatusCode -ge 400){ exit 1 } } catch { Write-Host '[TEST] Error accediendo a la URL'; exit 1 }"

        if /I "${OPEN_BROWSER}"=="true" (
          echo [TEST] Abriendo navegador en %URL%
          start "" "%URL%"
        )
        """
      }
    }

    stage('Mostrar tail de server.log (Windows)') {
      agent { label 'Windows' }
      steps {
        bat """
        @echo on
        powershell -NoProfile -Command "Get-Content -Path '%WILDFLY_HOME%\\standalone\\log\\server.log' -Tail 120"
        """
      }
    }
  }

  post {
    failure {
      echo '? Pipeline falló. Revisa los logs anteriores (deploy y server.log).'
    }
    success {
      echo '? Pipeline OK: compilado en Linux y desplegado en Windows.'
    }
  }
}
