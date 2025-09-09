pipeline {
  agent { label 'Linux' }   // ? Solo corre en agentes Linux

  tools { maven 'Maven'; jdk 'jdk11' }
  options { timestamps() }

  environment {
    MAVEN_FLAGS = '-B -U -DskipTests'
    EAR_NAME    = 'savia-ear.ear'
    TAR_NAME    = 'savia-build.tar.gz'

    // Ruta montada desde Windows (compartida con Docker/SMB). Debe apuntar al
    // C:\wildfly-19.1.0.Final\standalone\deployments del servidor Windows
    DEPLOY_DIR   = '/opt/deployments'
  }

  stages {
    stage('Checkout & Build') {
      steps {
        checkout scm
        sh '''
          set -e

          echo "[BUILD] Compilando módulos..."
          mvn -f "$WORKSPACE/savia-negocio/pom.xml" clean install  ${MAVEN_FLAGS}
          mvn -f "$WORKSPACE/savia-ejb/pom.xml"     clean install  ${MAVEN_FLAGS}
          mvn -f "$WORKSPACE/savia-web/pom.xml"     clean install  ${MAVEN_FLAGS}
          mvn -f "$WORKSPACE/savia-ear/pom.xml"     clean package  ${MAVEN_FLAGS}

          # Copiar EAR con nombre fijo para empaquetar/publicar
          EAR=$(ls "$WORKSPACE/savia-ear/target/"*.ear | head -n1)
          cp -f "$EAR" "$WORKSPACE/savia-ear/target/${EAR_NAME}"

          echo "[BUILD] Generando TAR con el EAR..."
          cd "$WORKSPACE/savia-ear/target"
          tar -czf "${TAR_NAME}" "${EAR_NAME}"

          echo "[BUILD] Listo: $(pwd)"
          ls -la
        '''
        // Publicar TAR en Jenkins para descarga
        archiveArtifacts artifacts: 'savia-ear/target/savia-build.tar.gz', fingerprint: true
      }
    }

    stage('Copiar a deployments (Windows)') {
      steps {
        sh '''
          set -e

          echo "[DEPLOY] Verificando carpeta de deployments montada: ${DEPLOY_DIR}"
          test -d "${DEPLOY_DIR}" || (echo "No existe ${DEPLOY_DIR}" && exit 1)

          echo "[DEPLOY] Limpiando carpeta de deployments..."
          rm -rf ${DEPLOY_DIR:?}/*

          echo "[DEPLOY] Copiando TAR a deployments..."
          cp "$WORKSPACE/savia-ear/target/${TAR_NAME}" "${DEPLOY_DIR}/${TAR_NAME}"

          echo "[DEPLOY] Descomprimiendo TAR en deployments..."
          tar -xzf "${DEPLOY_DIR}/${TAR_NAME}" -C "${DEPLOY_DIR}"

          echo "[DEPLOY] Eliminando TAR en deployments..."
          rm -f "${DEPLOY_DIR}/${TAR_NAME}"

          echo "[DEPLOY] Contenido final en ${DEPLOY_DIR}:"
          ls -la "${DEPLOY_DIR}"

          # ? Si quieres que WildFly lo tome automáticamente, descomenta:
          # touch "${DEPLOY_DIR}/${EAR_NAME}.dodeploy"
        '''
      }
    }
  }

  post {
    success {
      echo "? Build compilado en Linux, empaquetado en TAR, publicado en Jenkins y copiado a la carpeta de Windows (${env.DEPLOY_DIR})."
    }
    failure {
      echo "? Falló el proceso. Revisa la consola para detalles."
    }
  }
}
