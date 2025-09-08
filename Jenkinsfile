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
          echo "[BUILD] Compilando módulos..."
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
          echo [WILDFLY] Iniciando en Windows...
          start "" "C:\\wildfly-19.1.0.Final\\bin\\standalone.bat" -b 0.0.0.0
          echo [WILDFLY] Revisa logs en C:\\wildfly-19.1.0.Final\\standalone\\log\\server.log
        '''
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
    success { echo "? Build compilado y WildFly en Windows iniciado automáticamente." }
    failure { echo "? Falló el proceso." }
  }
}
