pipeline {
  agent { label 'Linux' }   // ? Solo corre en agentes Linux

  tools { maven 'Maven'; jdk 'jdk11' }
  options { timestamps() }

  environment {
    MAVEN_FLAGS = '-B -U -DskipTests'
    EAR_NAME    = 'savia-ear.ear'
    ZIP_NAME    = 'savia-build.zip'

    // WildFly en Linux
    WILDFLY_HOME = '/opt/wildfly-19.1.0.Final'
    DEPLOY_DIR   = '/opt/wildfly-19.1.0.Final/standalone/deployments'
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

          echo "[BUILD] Generando ZIP..."
          cd "$WORKSPACE/savia-ear/target"
          zip -r "${ZIP_NAME}" "${EAR_NAME}"
        '''
        // Publicar ZIP en Jenkins
        archiveArtifacts artifacts: 'savia-ear/target/savia-build.zip', fingerprint: true
      }
    }

    stage('Desplegar en WildFly') {
      steps {
        sh '''
        set -e

        echo "[DEPLOY] Limpiando carpeta de deployments..."
        rm -rf ${DEPLOY_DIR:?}/*

        echo "[DEPLOY] Copiando ZIP a deployments..."
        cp "$WORKSPACE/savia-ear/target/${ZIP_NAME}" "${DEPLOY_DIR}/${ZIP_NAME}"

        echo "[DEPLOY] Descomprimiendo ZIP en deployments..."
        unzip -o "${DEPLOY_DIR}/${ZIP_NAME}" -d "${DEPLOY_DIR}"

        echo "[DEPLOY] Limpiando ZIP después de extraer..."
        rm -f "${DEPLOY_DIR}/${ZIP_NAME}"
        '''
      }
    }
  }

  post {
    success { echo "? Build compilado en Linux, empaquetado en ZIP, publicado en Jenkins y desplegado en WildFly (Linux)." }
    failure { echo "? Falló el proceso. Revisa logs de compilación o despliegue." }
  }
}
