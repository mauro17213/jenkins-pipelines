pipeline {
  agent { label 'Linux' }   // Solo corre en agentes Linux

  tools { maven 'Maven'; jdk 'jdk11' }
  options { timestamps() }

  environment {
    MAVEN_FLAGS = '-B -U -DskipTests'
    EAR_NAME    = 'savia-ear.ear'
    TAR_NAME    = 'savia-build.tar.gz'

    // Ruta montada desde Windows (compartida con Docker o SMB)
    DEPLOY_DIR   = '/opt/deployments'

    // Ruta a WildFly en Windows
    WILDFLY_HOME_WIN = 'C:\\wildfly-19.1.0.Final'
    WF_HOST      = 'localhost'
    WF_PORT      = '9990'
  }

  stages {
    stage('Checkout & Build') {
      steps {
        sh '''
          set -e
          echo "[BUILD] Compilando m�dulos..."
          mvn -f "$WORKSPACE/savia-negocio/pom.xml" clean install  ${MAVEN_FLAGS}
          mvn -f "$WORKSPACE/savia-ejb/pom.xml"     clean install  ${MAVEN_FLAGS}
          mvn -f "$WORKSPACE/savia-web/pom.xml"     clean install  ${MAVEN_FLAGS}
          mvn -f "$WORKSPACE/savia-ear/pom.xml"     clean package  ${MAVEN_FLAGS}

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

    stage('Iniciar WildFly en Windows') {
      agent { label 'windows' }
      steps {
        bat '''
          echo [WILDFLY] Matando instancias previas...
          taskkill /F /IM java.exe /T >nul 2>&1 || echo "No hab�a instancias previas"

          echo [WILDFLY] Iniciando standalone en background...
          start /B "" "%WILDFLY_HOME_WIN%\\bin\\standalone.bat" -b 0.0.0.0 -bmanagement 0.0.0.0

          echo [WILDFLY] Esperando que escuche en puerto 9990...
        '''
        // Espera activa para asegurar que WildFly ya arranc�
        timeout(time: 2, unit: 'MINUTES') {
          waitUntil {
            bat(script: '''
              powershell -Command "
                try {
                  $c = New-Object Net.Sockets.TcpClient('localhost', 9990)
                  if ($c.Connected) { $c.Close(); exit 0 } else { exit 1 }
                } catch { exit 1 }
              "
            ''', returnStatus: true) == 0
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

          echo "[DEPLOY] Limpiando TAR despu�s de extraer..."
          rm -f "${DEPLOY_DIR}/${TAR_NAME}"

          echo "[DEPLOY] Forzando scanner de WildFly..."
          touch "${DEPLOY_DIR}/${EAR_NAME}.dodeploy"
        '''
      }
    }
  }

  post {
    success { echo "? Build compilado y desplegado en WildFly (Opci�n 1)." }
    failure { echo "? Fall� el proceso." }
  }
}
