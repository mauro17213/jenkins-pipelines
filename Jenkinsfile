pipeline {
  agent { label 'Linux' }   // ? Solo corre en agentes Linux

  tools { maven 'Maven'; jdk 'jdk11' }
  options { timestamps() }

  environment {
    MAVEN_FLAGS = '-B -U -DskipTests'
    EAR_NAME    = 'savia-ear.ear'
    TAR_NAME    = 'savia-build.tar.gz'

    // Ruta montada desde Windows (compartida con Docker)
    DEPLOY_DIR   = '/opt/deployments'
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
        // Publicar TAR en Jenkins
        archiveArtifacts artifacts: 'savia-ear/target/savia-build.tar.gz', fingerprint: true
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
        '''
      }
    }
  }

  post {
    success { echo "? Build compilado en Linux, empaquetado en TAR, publicado en Jenkins y desplegado en WildFly (Windows, vía carpeta compartida)." }
    failure { echo "? Falló el proceso. Revisa logs de compilación o despliegue." }
  }
}
