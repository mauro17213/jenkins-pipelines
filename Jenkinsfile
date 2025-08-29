pipeline {
  agent { label 'Linux' }

  tools {
    maven 'Maven'
    jdk   'jdk11'
  }

  environment {
    WILDFLY_HOME = "${WORKSPACE}/wildfly-19.1.0.Final"
    DEPLOYMENTS  = "${WILDFLY_HOME}/standalone/deployments"
    EAR_NAME     = 'savia-ear.ear'
    ZIP_OUT_DIR  = "${WORKSPACE}/dist"
    ZIP_NAME     = "savia-modulos.zip"
    USE_EXPLODED = 'false'
  }

  stages {
    stage('Checkout SCM') { steps { checkout scm } }

    stage('Build Modules') {
      steps {
        script {
          echo '? Compilando módulos (negocio ? ejb ? web ? ear)...'
          sh "mvn -f \"${WORKSPACE}/savia-negocio/pom.xml\" clean install -DskipTests"
          sh "mvn -f \"${WORKSPACE}/savia-ejb/pom.xml\"     clean install -DskipTests"
          sh "mvn -f \"${WORKSPACE}/savia-web/pom.xml\"     clean install -DskipTests"
          sh "mvn -f \"${WORKSPACE}/savia-ear/pom.xml\"     clean package -DskipTests"

          def generatedEar = sh(script: "ls ${WORKSPACE}/savia-ear/target/*.ear | head -n 1", returnStdout: true).trim()
          if (!generatedEar) { error "No se encontró el EAR en target" }
          sh "mv -f \"${generatedEar}\" \"${WORKSPACE}/savia-ear/target/${EAR_NAME}\""
          echo "? EAR renombrado a ${EAR_NAME}"
        }
      }
    }

    // Empaquetar módulos en un ZIP (usa 'jar' que genera ZIP)
    stage('Package ZIP with modules') {
      steps {
        sh """
          set -e
          rm -rf "${ZIP_OUT_DIR}" && mkdir -p "${ZIP_OUT_DIR}"
          cp -f ${WORKSPACE}/savia-ejb/target/*.jar      "${ZIP_OUT_DIR}/" || true
          cp -f ${WORKSPACE}/savia-negocio/target/*.jar  "${ZIP_OUT_DIR}/" || true
          cp -f ${WORKSPACE}/savia-web/target/*.war      "${ZIP_OUT_DIR}/" || true
          cp -f ${WORKSPACE}/savia-ear/target/*.ear      "${ZIP_OUT_DIR}/" || true
          cd "${ZIP_OUT_DIR}"
          jar -cfM "${ZIP_NAME}" .
          echo "? ZIP generado en: ${ZIP_OUT_DIR}/${ZIP_NAME}"
        """
      }
    }

    stage('Publish ZIP') {
      steps { archiveArtifacts artifacts: 'dist/savia-modulos.zip', fingerprint: true }
    }

    stage('Stop WildFly') {
      steps {
        sh """
          set +e
          ${WILDFLY_HOME}/bin/jboss-cli.sh --connect command=:shutdown 2>/dev/null || true
          sleep 5
          echo '? WildFly detenido (si estaba corriendo).'
        """
      }
    }

    stage('Deploy to WildFly') {
      steps {
        script {
          if (env.USE_EXPLODED.toBoolean()) {
            echo '? Despliegue EXPLODED (carpeta .ear/)'
            sh """
              set -e
              mkdir -p ${DEPLOYMENTS}
              rm -rf "${DEPLOYMENTS}/${EAR_NAME}" "${DEPLOYMENTS}/${EAR_NAME}.dodeploy" "${DEPLOYMENTS}/${EAR_NAME}.deployed" "${DEPLOYMENTS}/${EAR_NAME}.failed" "${DEPLOYMENTS}/${EAR_NAME}.undeployed" || true
              mkdir -p "${DEPLOYMENTS}/${EAR_NAME}"
              if ! command -v unzip >/dev/null 2>&1; then
                if command -v apt-get >/dev/null 2>&1; then sudo apt-get update && sudo apt-get install -y unzip;
                elif command -v yum >/dev/null 2>&1; then sudo yum install -y unzip;
                elif command -v apk >/dev/null 2>&1; then sudo apk add --no-cache unzip;
                else echo "Instala unzip en el agente"; exit 1; fi
              fi
              unzip -q "${WORKSPACE}/savia-ear/target/${EAR_NAME}" -d "${DEPLOYMENTS}/${EAR_NAME}"
              : > "${DEPLOYMENTS}/${EAR_NAME}.dodeploy"
            """
          } else {
            echo '? Despliegue EAR comprimido'
            sh """
              set -e
              mkdir -p ${DEPLOYMENTS}
              rm -rf "${DEPLOYMENTS}/${EAR_NAME}" "${DEPLOYMENTS}/${EAR_NAME}.dodeploy" "${DEPLOYMENTS}/${EAR_NAME}.deployed" "${DEPLOYMENTS}/${EAR_NAME}.failed" "${DEPLOYMENTS}/${EAR_NAME}.undeployed" || true
              cp -f "${WORKSPACE}/savia-ear/target/${EAR_NAME}" "${DEPLOYMENTS}/"
              : > "${DEPLOYMENTS}/${EAR_NAME}.dodeploy"
            """
          }
        }
      }
    }

    stage('Unblock WildFly files') {
      steps {
        sh """
          chmod +x ${WILDFLY_HOME}/bin/*.sh
          echo "? Scripts de WildFly marcados como ejecutables."
        """
      }
    }

    stage('Start WildFly') {
      steps {
        sh """
          nohup ${WILDFLY_HOME}/bin/standalone.sh -b 0.0.0.0 > ${WILDFLY_HOME}/standalone/log/boot.out 2>&1 &
          echo "? WildFly iniciado."
        """
      }
    }

    stage('Verify Deployment') {
      steps {
        script {
          def logPath = "${WILDFLY_HOME}/standalone/log/server.log"
          timeout(time: 150, unit: 'SECONDS') {
            waitUntil {
              def exists = sh(script: "[ -f ${logPath} ] && echo yes || echo no", returnStdout: true).trim()
              if (exists == 'yes') {
                def logContent = sh(script: "tail -n 300 ${logPath}", returnStdout: true).trim()
                if (logContent.contains("Deployed \"${EAR_NAME}\"")) {
                  echo "? EAR desplegado correctamente en WildFly."
                  return true
                }
                if (logContent.contains(".failed")) {
                  error "? El despliegue de ${EAR_NAME} falló. Revisa ${logPath}."
                }
              }
              sleep 5
              return false
            }
          }
        }
      }
    }
  }
}
