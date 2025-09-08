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

    // Ajusta si usas jboss-cli (opcional)
    WILDFLY_HOME = '/opt/wildfly'       // ruta binarios WildFly en Linux
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

          echo "[DEPLOY] Forzando scanner de WildFly..."
          touch "${DEPLOY_DIR}/${EAR_NAME}.dodeploy"
        '''
      }
    }

    stage('Deploy via jboss-cli (opcional)') {
      when { expression { return fileExists("${env.WILDFLY_HOME}/bin/jboss-cli.sh") } }
      steps {
        withCredentials([usernamePassword(credentialsId: 'wildfly-admin', usernameVariable: 'WF_USER', passwordVariable: 'WF_PASS')]) {
          sh '''
            echo "[DEPLOY] Ejecutando jboss-cli..."
            ${WILDFLY_HOME}/bin/jboss-cli.sh \
              --connect --controller=${WF_HOST}:${WF_PORT} \
              --user=${WF_USER} --password=${WF_PASS} \
              --command="deploy ${DEPLOY_DIR}/${EAR_NAME} --force"
          '''
        }
      }
    }
  }

  post {
    success { echo "? Build compilado en Linux, empaquetado en TAR, publicado en Jenkins y desplegado en WildFly." }
    failure { echo "? Falló el proceso. Revisa logs de compilación o despliegue." }
  }
}
