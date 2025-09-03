pipeline {
  agent none
  options { timestamps() }

  environment {
    EAR_NAME     = 'savia-ear.ear'
    MAVEN_FLAGS  = '-B -U -DskipTests'
    // Windows/WildFly
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
    stage('Checkout SCM') {
      agent { label 'Linux' } // primera mayúscula, como pediste
      steps { checkout scm }
    }

    stage('Build en Linux (negocio/ejb/web/ear)') {
      agent { label 'Linux' }
      tools {
        maven 'Maven'
        jdk   'jdk11'
      }
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

          # Solo stasheamos el EAR final con un nombre estable
          find "$WORKSPACE/savia-ear/target" -maxdepth 1 -type f -name "$EAR_NAME" -printf "%f\n" > ear.list
        '''
        stash name: 'ear', includes: 'savia-ear/target/savia-ear.ear'
      }
    }

    stage('Preparar artefacto en Windows') {
      agent { label 'Windows' }
      tools {
        maven 'Maven'
        jdk   'jdk11'
      }
      steps {
        unstash 'ear'
        bat '''
        @echo on
        if not exist "%DEPLOY_DIR%" mkdir "%DEPLOY_DIR%"
        copy /Y "%WORKSPACE%\\savia-ear\\target\\savia-ear.ear" "%DEPLOY_DIR%\\%EAR_NAME%"
        echo [ARTIFACT] Copiado a %DEPLOY_DIR%\\%EAR_NAME%
        '''
      }
    }

    stage('Start WildFly en Windows') {
      agent { label 'Windows' }
      steps {
        bat '''
        @echo on
        setlocal

        echo [WF] Verificando si el puerto %HTTP_PORT% esta en uso...
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

        echo [WF] Esperando gestion (%MGMT_HOST%:%MGMT_PORT%)...
        powershell -NoProfile -Command ^
          "$u='http://%MGMT_HOST%:%MGMT_PORT%/management'; 1..60 ^| ForEach-Object { ^
             try { $r=Invoke-WebRequest -Uri $u -UseBasicParsing -TimeoutSec 3; ^
                   if($r.StatusCode -in 200,401){ Write-Host 'Management UP'; exit 0 } } catch{}; ^
             Start-Sleep -Seconds 2 }; exit 1"
        '''
      }
    }

    stage('Deploy en Windows (CLI)') {
      agent { label 'Windows' }
      steps {
        // Si tu consola de administración requiere usuario/clave,
        // usa credenciales de Jenkins y pásalas con --user/--password
        // (aquí mostramos el modo sin auth local).
        bat '''
        @echo on
        set "EAR=%DEPLOY_DIR%\\%EAR_NAME%"
        if not exist "%EAR%" (
          echo [DEPLOY] ! No existe %EAR%
          exit /b 1
        )

        echo [DEPLOY] Desplegando por CLI: %EAR%
        "%WILDFLY_HOME%\\bin\\jboss-cli.bat" --connect --controller=%MGMT_HOST%:%MGMT_PORT% --commands="deploy %EAR% --force"

        if errorlevel 1 (
          echo [DEPLOY] ! Error en deploy por CLI
          exit /b 1
        ) else (
          echo [DEPLOY] OK
        )
        '''
      }
    }

    stage('Smoke test (Windows)') {
      agent { label 'Windows' }
      steps {
        bat '''
        @echo on
        set "URL=http://%HTTP_HOST%:%HTTP_PORT%/%APP_CTX%/"
        echo [TEST] Request a %URL%
        powershell -NoProfile -Command "try{ $r=Invoke-WebRequest -Uri '%URL%' -UseBasicParsing -TimeoutSec 15; Write-Host ('[TEST] HTTP ' + $r.StatusCode); if($r.StatusCode -ge 400){ exit 1 } } catch { Write-Host '[TEST] Error accediendo a la URL'; exit 1 }"

        if /I "%OPEN_BROWSER%"=="true" (
          start "" "%URL%"
        )
        '''
      }
    }

    stage('Tail server.log (Windows)') {
      agent { label 'Windows' }
      steps {
        bat '''
        @echo on
        powershell -NoProfile -Command "Get-Content -Path '%WILDFLY_HOME%\\standalone\\log\\server.log' -Tail 120"
        '''
      }
    }
  }

  post {
    failure {
      echo '? Pipeline falló. Revisa los logs anteriores (deploy y server.log).'
    }
  }
}
