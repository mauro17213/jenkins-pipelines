pipeline {
  agent none

  tools { maven 'Maven'; jdk 'jdk11' }
  options { timestamps() }

  environment {
    MAVEN_FLAGS = '-B -U -DskipTests'
    EAR_NAME    = 'savia-ear.ear'

    // Windows / WildFly
    WILDFLY_HOME = 'C:\\wildfly-19.1.0.Final'
    DEPLOY_DIR   = 'C:\\wildfly-19.1.0.Final\\standalone\\deployments'
    MGMT_HOST    = '127.0.0.1'
    MGMT_PORT    = '9990'
    HTTP_HOST    = '127.0.0.1'
    HTTP_PORT    = '8080'
    APP_CTX      = 'savia'
    OPEN_BROWSER = 'false'
  }

  stages {
    stage('Checkout (Linux)') {
      agent { label 'Linux' }
      steps { checkout scm }
    }

    stage('Build en Linux (negocio/ejb/web/ear)') {
      agent { label 'Linux' }
      steps {
        sh '''
          set -e
          echo "[BUILD] Compilando módulos en Linux..."
          mvn -f "$WORKSPACE/savia-negocio/pom.xml" clean install  ${MAVEN_FLAGS}
          mvn -f "$WORKSPACE/savia-ejb/pom.xml"     clean install  ${MAVEN_FLAGS}
          mvn -f "$WORKSPACE/savia-web/pom.xml"     clean install  ${MAVEN_FLAGS}
          mvn -f "$WORKSPACE/savia-ear/pom.xml"     clean package  ${MAVEN_FLAGS}

          EAR=$(ls "$WORKSPACE/savia-ear/target/"*.ear | head -n1)
          cp -f "$EAR" "$WORKSPACE/savia-ear/target/${EAR_NAME}"
          echo "[BUILD] EAR listo: ${EAR_NAME}"

          # Deja solo el nombre final para stashear
          find "$WORKSPACE/savia-ear/target" -maxdepth 1 -type f -name '*.ear' ! -name "${EAR_NAME}" -delete
        '''
        stash name: 'EAR', includes: 'savia-ear/target/savia-ear.ear'
      }
    }

    stage('Copiar EAR a deployments (Windows)') {
      agent { label 'Windows' }
      steps {
        checkout scm
        unstash 'EAR'
        bat '''
        @echo on
        setlocal
        if not exist "%DEPLOY_DIR%" mkdir "%DEPLOY_DIR%"

        echo [DEPLOY] Limpiando markers previos...
        del /f /q "%DEPLOY_DIR%\\%EAR_NAME%.deployed" "%DEPLOY_DIR%\\%EAR_NAME%.failed" "%DEPLOY_DIR%\\%EAR_NAME%.undeployed" "%DEPLOY_DIR%\\%EAR_NAME%.dodeploy" 2>nul

        echo [DEPLOY] Copiando EAR al scanner...
        copy /Y "%WORKSPACE%\\savia-ear\\target\\%EAR_NAME%" "%DEPLOY_DIR%\\%EAR_NAME%"
        echo [OK] EAR en: %DEPLOY_DIR%\\%EAR_NAME%
        '''
      }
    }

    stage('Start WildFly (Windows: start-if-stopped)') {
      agent { label 'Windows' }
      steps {
        // Comprueba la interfaz de administración; si NO responde, arranca WildFly y espera a que responda.
        bat '''
        @echo on
        setlocal
        set MGMT_URL=http://%MGMT_HOST%:%MGMT_PORT%/management

        echo [WF] Comprobando interfaz de administración %MGMT_URL% ...
        powershell -NoProfile -Command ^
          "$ok=$false; try { $r=Invoke-WebRequest -Uri $env:MGMT_URL -UseBasicParsing -TimeoutSec 3; if($r.StatusCode -in 200,401){ $ok=$true } } catch{}; if($ok){ exit 0 } else { exit 1 }"
        if %ERRORLEVEL% EQU 0 (
          echo [WF] WildFly YA responde por management. No se arranca de nuevo.
          goto :done
        )

        echo [WF] WildFly no responde. Iniciando...
        pushd "%WILDFLY_HOME%\\bin"
        start "WILDFLY" /MIN standalone.bat -b 0.0.0.0 -Djboss.bind.address.management=%MGMT_HOST%
        popd

        echo [WF] Esperando a que management esté arriba (hasta 90s)...
        powershell -NoProfile -Command ^
          "$u='http://%MGMT_HOST%:%MGMT_PORT%/management'; for($i=1;$i -le 90;$i++){ try { $r=Invoke-WebRequest -Uri $u -UseBasicParsing -TimeoutSec 3; if($r.StatusCode -in 200,401){ Write-Host 'Management UP'; exit 0 } } catch{}; Start-Sleep -Seconds 1 }; exit 1"
        if %ERRORLEVEL% NEQ 0 (
          echo [WF][ERROR] No levantó management en el tiempo esperado.
          powershell -NoProfile -Command "Get-Content -Path '%WILDFLY_HOME%\\standalone\\log\\server.log' -Tail 200"
          exit /b 1
        )
        :done
        '''
      }
    }

    stage('Desplegar por scanner (Windows)') {
      agent { label 'Windows' }
      steps {
        bat '''
        @echo on
        setlocal
        set "APP=%DEPLOY_DIR%\\%EAR_NAME%"

        echo [DEPLOY] Creando marker .dodeploy...
        type nul > "%APP%.dodeploy"

        echo [DEPLOY] Esperando .deployed / .failed ...
        set /a COUNT=0
        :waitloop
        if exist "%APP%.deployed" goto deployed
        if exist "%APP%.failed"  goto failed
        timeout /t 2 >nul
        set /a COUNT+=1
        if %COUNT% GEQ 120 goto timeout
        goto waitloop

        :deployed
        echo [OK] Deploy completado (.deployed)
        exit /b 0

        :failed
        echo [ERROR] Deploy FAILED (.failed). Últimas líneas del server.log:
        powershell -NoProfile -Command "Get-Content -Path '%WILDFLY_HOME%\\standalone\\log\\server.log' -Tail 200"
        exit /b 1

        :timeout
        echo [ERROR] Timeout esperando markers. Últimas líneas del server.log:
        powershell -NoProfile -Command "Get-Content -Path '%WILDFLY_HOME%\\standalone\\log\\server.log' -Tail 200"
        exit /b 1
        '''
      }
    }

    stage('Smoke test (Windows)') {
      agent { label 'Windows' }
      steps {
        bat '''
        @echo off
        set "URL=http://%HTTP_HOST%:%HTTP_PORT%/%APP_CTX%/"
        echo [TEST] GET %URL%
        powershell -NoProfile -Command ^
          "try{ $r=Invoke-WebRequest -Uri $env:URL -UseBasicParsing -TimeoutSec 10; ^
                Write-Host ('[TEST] HTTP ' + $r.StatusCode); if($r.StatusCode -ge 400){ exit 1 } } ^
           catch { Write-Host '[TEST] Error accediendo a la URL'; exit 1 }"

        if /I "%OPEN_BROWSER%"=="true" start "" "%URL%"
        '''
      }
    }
  }

  post {
    success { echo '? Compilado en Linux, WildFly arrancado si hacía falta y EAR desplegado en Windows (8080).' }
    failure { echo '? Falló el proceso. Revisa los stages de Start/Deploy y el server.log.' }
  }
}
