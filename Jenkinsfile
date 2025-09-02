pipeline {
  agent none

  tools {
    maven 'Maven'
    jdk   'jdk11'
  }

  options { timestamps() }

  environment {
    EAR_NAME     = 'savia-ear.ear'
    MAVEN_FLAGS  = '-B -U -DskipTests'

    // Windows (WildFly de destino)
    WILDFLY_HOME = 'C:\\wildfly-19.1.0.Final'
    DEPLOY_DIR   = 'C:\\wildfly-19.1.0.Final\\standalone\\deployments'

    // Puertos/URLs
    MGMT_HOST = '127.0.0.1'
    MGMT_PORT = '9990'
    HTTP_HOST = '127.0.0.1'
    HTTP_PORT = '8080'
    APP_CTX   = 'savia'

    // Abrir browser al final en el agente Windows
    OPEN_BROWSER = 'true'
  }

  stages {
    stage('Checkout SCM') {
      agent { label 'Linux' }
      steps { checkout scm }
    }

    stage('Build en Linux') {
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
          [ -z "$EAR" ] && { echo "[BUILD] ? No se encontró .ear en savia-ear/target"; exit 1; }
          cp -f "$EAR" "$WORKSPACE/savia-ear/target/$EAR_NAME"
          echo "[BUILD] ? EAR listo: $EAR_NAME"
        '''
        stash includes: 'savia-ear/target/savia-ear.ear', name: 'ear'
      }
    }

    stage('Start WildFly (Windows, keep running)') {
      agent { label 'Windows' }
      steps {
        bat '''
          @echo on
          setlocal
          echo [WF] Verificando si el puerto %HTTP_PORT% está en uso...
          for /f "tokens=5" %%a in ('netstat -ano ^| findstr /R /C:":%HTTP_PORT% .*LISTENING"') do set PID=%%a

          if defined PID (
            echo [WF] Ya hay un proceso escuchando en %HTTP_PORT% (PID !PID!). Se asume WildFly iniciado.
          ) else (
            echo [WF] WildFly no está corriendo. Iniciando en background...
            pushd "%WILDFLY_HOME%\\bin"
            start "WILDFLY" /MIN standalone.bat -b 0.0.0.0 -Djboss.bind.address.management=%MGMT_HOST%
            popd
          )

          echo [WF] Esperando interfaz de administración (%MGMT_HOST%:%MGMT_PORT%)...
          powershell -NoProfile -Command "$u='http://%MGMT_HOST%:%MGMT_PORT%/management'; for($i=1;$i -le 60;$i++){ try { $r=Invoke-WebRequest -Uri $u -UseBasicParsing -TimeoutSec 3; if($r.StatusCode -in 200,401){ Write-Host 'Management UP'; exit 0 } } catch{}; Start-Sleep -Seconds 2 } ; exit 1"
        '''
      }
    }

    stage('Deploy via Scanner (Windows)') {
      agent { label 'Windows' }
      steps {
        unstash 'ear'
        bat '''
          @echo on
          setlocal
          set "APP=%DEPLOY_DIR%\\%EAR_NAME%"
          set "SRC=%WORKSPACE%\\savia-ear\\target\\%EAR_NAME%"

          if not exist "%DEPLOY_DIR%" mkdir "%DEPLOY_DIR%"

          echo [DEPLOY] Limpiando markers previos...
          del /f /q "%APP%" "%APP%.deployed" "%APP%.failed" "%APP%.undeployed" "%APP%.dodeploy" 2>nul

          echo [DEPLOY] Copiando EAR al scanner...
          copy /Y "%SRC%" "%APP%"
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
          echo [DEPLOY] ? Deploy FAILED (.failed). Últimas líneas de server.log:
          powershell -NoProfile -Command "Get-Content -Path '%WILDFLY_HOME%\\standalone\\log\\server.log' -Tail 200"
          exit /b 1

          :timeout
          echo [DEPLOY] ? Timeout esperando markers. Últimas líneas de server.log:
          powershell -NoProfile -Command "Get-Content -Path '%WILDFLY_HOME%\\standalone\\log\\server.log' -Tail 200"
          exit /b 1
        '''
      }
    }

    stage('Smoke Test (y abrir navegador opcional)') {
      agent { label 'Windows' }
      steps {
        bat '''
          @echo on
          set "URL=http://%HTTP_HOST%:%HTTP_PORT%/%APP_CTX%/"
          echo [TEST] Haciendo request a %URL%
          powershell -NoProfile -Command "try{ $r=Invoke-WebRequest -Uri '%URL%' -UseBasicParsing -TimeoutSec 10; Write-Host ('[TEST] HTTP ' + $r.StatusCode); if($r.StatusCode -ge 400){ exit 1 } } catch { Write-Host '[TEST] ? Error accediendo a la URL'; exit 1 }"

          if /I "%OPEN_BROWSER%"=="true" (
            echo [TEST] Abriendo navegador en %URL%
            start "" "%URL%"
          )
        '''
      }
    }
  }
 # prueba commit# prueba commit# prueba commit# prueba commit# prueba commit# prueba commitv# prueba commit
  post {
    always {
      echo 'WildFly queda ejecutándose en el agente Windows.'
    }
    failure {
      echo '? Pipeline falló. Revisa los logs anteriores (deploy y server.log).'
    }
  }
}
