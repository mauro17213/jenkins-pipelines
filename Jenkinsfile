pipeline {
  agent none
  options { timestamps() }

  environment {
    MAVEN_FLAGS = '-B -U -DskipTests'
    DIST_DIR    = 'dist'
    TAR_NAME    = 'savia-build.tar.gz'
    WAIT_LOOPS  = '180'   // 180 * 2s = 6 min de espera a .deployed
  }

  stages {
    // ------------------ BUILD EN LINUX ------------------
    stage('Build (Linux)') {
      agent { label 'Linux' }
      tools { maven 'Maven'; jdk 'jdk11' }
      steps {
        checkout scm
        sh '''
          #!/bin/sh
          set -eu
          echo "[BUILD] Compilando módulos..."
          mvn -f "$WORKSPACE/savia-negocio/pom.xml" clean install  ${MAVEN_FLAGS}
          mvn -f "$WORKSPACE/savia-ejb/pom.xml"     clean install  ${MAVEN_FLAGS}
          mvn -f "$WORKSPACE/savia-web/pom.xml"     clean package  ${MAVEN_FLAGS}
          mvn -f "$WORKSPACE/savia-ear/pom.xml"     clean package  ${MAVEN_FLAGS}

          echo "[BUILD] Detectando artefactos..."
          EAR=$(ls "$WORKSPACE/savia-ear/target/"*.ear | head -n1)
          WAR=$(ls "$WORKSPACE/savia-web/target/"*.war | head -n1)

          mkdir -p "$WORKSPACE/${DIST_DIR}"
          cp -f "$EAR" "$WORKSPACE/${DIST_DIR}/$(basename "$EAR")"
          cp -f "$WAR" "$WORKSPACE/${DIST_DIR}/$(basename "$WAR")"
          (cd "$WORKSPACE/${DIST_DIR}" && tar -czf "${TAR_NAME}" "$(basename "$EAR")" "$(basename "$WAR")") || true

          echo "[BUILD] Contenido de ${DIST_DIR}:"
          ls -la "$WORKSPACE/${DIST_DIR}"
        '''
        archiveArtifacts artifacts: "${env.DIST_DIR}/*", fingerprint: true
        stash name: 'dist', includes: "${env.DIST_DIR}/**"
      }
    }

    // ------------------ ARRANCAR WILDFLY EN WINDOWS ------------------
    stage('Start WildFly (Windows)') {
      agent { label 'windows' }
      environment {
        WF_HOME       = 'C:\\wildfly-19.1.0.Final'
        WF_DEPLOY_DIR = 'C:\\wildfly-19.1.0.Final\\standalone\\deployments'
        WF_HOST       = '127.0.0.1'
        WF_MGMT_PORT  = '9990'
      }
      steps {
        powershell '''
          $wfHome     = $env:WF_HOME
          $mgmtPort   = [int]$env:WF_MGMT_PORT
          $isUp = Test-NetConnection -ComputerName 127.0.0.1 -Port $mgmtPort -InformationLevel Quiet

          if (-not $isUp) {
            Write-Host "[WF] Iniciando WildFly..."
            Start-Process -FilePath "$wfHome\\bin\\standalone.bat" `
                          -ArgumentList "-b 0.0.0.0 -c standalone-full.xml" `
                          -WindowStyle Hidden

            # Espera a que levante el puerto de management
            $limit = 60
            for ($i=0; $i -lt $limit; $i++) {
              Start-Sleep -Seconds 2
              if (Test-NetConnection -ComputerName 127.0.0.1 -Port $mgmtPort -InformationLevel Quiet) {
                Write-Host "[WF] Arriba."
                break
              }
            }
            if (-not (Test-NetConnection -ComputerName 127.0.0.1 -Port $mgmtPort -InformationLevel Quiet)) {
              throw "WildFly no levantó (puerto $mgmtPort)."
            }
          } else {
            Write-Host "[WF] Ya está arriba."
          }
        '''
      }
    }

    // ------------------ DEPLOY + RUN EN WINDOWS ------------------
    stage('Deploy & Run (Windows)') {
      agent { label 'windows' }
      environment {
        WF_HOME       = 'C:\\wildfly-19.1.0.Final'
        WF_DEPLOY_DIR = 'C:\\wildfly-19.1.0.Final\\standalone\\deployments'
      }
      steps {
        unstash 'dist'
        powershell '''
          $deployDir = $env:WF_DEPLOY_DIR
          $dist      = Join-Path $env:WORKSPACE 'dist'
          $waitLoops = [int]$env:WAIT_LOOPS

          if (-not (Test-Path $deployDir)) { throw "No existe $deployDir" }

          $ear = Get-ChildItem -Path $dist -Filter *.ear | Select-Object -First 1
          $war = Get-ChildItem -Path $dist -Filter *.war | Select-Object -First 1
          if (-not $ear) { throw "No se encontró EAR en $dist" }
          if (-not $war) { throw "No se encontró WAR en $dist" }

          # Limpia restos previos de esta app
          Remove-Item -Force -Recurse -ErrorAction SilentlyContinue `
            "$deployDir\\$($ear.Name)*","$deployDir\\$($war.Name)*"

          Write-Host "[DEPLOY] Copiando artefactos..."
          Copy-Item -Force $ear.FullName "$deployDir\\$($ear.Name)"
          Copy-Item -Force $war.FullName "$deployDir\\$($war.Name)"

          # Dispara el scanner (equivalente a Run)
          New-Item -ItemType File -Path "$deployDir\\$($ear.Name).dodeploy" -Force | Out-Null
          New-Item -ItemType File -Path "$deployDir\\$($war.Name).dodeploy" -Force | Out-Null

          function Wait-Deployed([string]$name) {
            $deadline = (Get-Date).AddSeconds($waitLoops * 2)
            while (Get-Date -lt $deadline) {
              if (Test-Path "$deployDir\\$name.deployed") { return }
              if (Test-Path "$deployDir\\$name.failed") {
                Write-Host "---- $name.failed ----"
                Get-Content "$deployDir\\$name.failed" | Write-Host
                throw "$name FAILED"
              }
              Start-Sleep -Seconds 2
            }
            throw "Timeout esperando $name"
          }

          Write-Host "[RUN] Esperando despliegues..."
          Wait-Deployed $ear.Name
          Wait-Deployed $war.Name

          if (Test-Path "$deployDir\\$($ear.Name)") { Write-Host "OK: $($ear.Name) EXPLOTADO a carpeta." } else { Write-Host "ADVERTENCIA: $($ear.Name) no se explotó (auto-extract=true)." }
          if (Test-Path "$deployDir\\$($war.Name)") { Write-Host "OK: $($war.Name) EXPLOTADO a carpeta." } else { Write-Host "ADVERTENCIA: $($war.Name) no se explotó (auto-extract=true)." }

          Write-Host "[DEPLOY] Contenido final de deployments:"
          Get-ChildItem $deployDir | Format-Table -AutoSize
        '''
      }
    }
  }

  post {
    success { echo '? Build en Linux, luego start WildFly + deploy & RUN en Windows.' }
    failure { echo "? Revisa la consola y los *.failed en el deployments de Windows." }
  }
}
