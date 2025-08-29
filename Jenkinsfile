pipeline {
  agent { label 'Linux' }

  tools {
    maven 'Maven'   // nombre de Maven en Jenkins
    jdk   'jdk11'   // o 'jdk17'
  }

  options { timestamps() }

  environment {
    // Cambia si tu WildFly está en otra ruta
    WILDFLY_HOME = '/opt/wildfly-19.1.0.Final'
    DEPLOYMENTS  = "${WILDFLY_HOME}/standalone/deployments"
    EAR_NAME     = 'savia-ear.ear'
    MAVEN_FLAGS  = '-B -U -DskipTests'
    BIND_ADDR    = '0.0.0.0'
  }

  stages {
    stage('Checkout SCM') {
      steps { checkout scm }
    }

    stage('Build Modules (sin POM padre)') {
      steps {
        script {
          sh '''
            set -e
            echo "Compilando modulos..."
            # Orden recomendado por dependencias
            mvn -f "${WORKSPACE}/savia-negocio/pom.xml" clean install ${MAVEN_FLAGS}
            mvn -f "${WORKSPACE}/savia-ejb/pom.xml"     clean install ${MAVEN_FLAGS}
            mvn -f "${WORKSPACE}/savia-web/pom.xml"     clean package ${MAVEN_FLAGS}
            mvn -f "${WORKSPACE}/savia-ear/pom.xml"     clean package ${MAVEN_FLAGS}

            # Renombrar/copiar el EAR generado a nombre fijo
            EAR=$(ls -1 "${WORKSPACE}/savia-ear/target/"*.ear | head -n1 || true)
            if [ -z "$EAR" ]; then
              echo "ERROR: No se encontro EAR en savia-ear/target"
              exit 1
            fi
            cp -f "$EAR" "${WORKSPACE}/savia-ear/target/${EAR_NAME}"
            echo "EAR listo: ${EAR_NAME}"
          '''
        }
      }
    }

    stage('Stop WildFly') {
      steps {
        sh '''
          set +e
          # Intento elegante por CLI
          "${WILDFLY_HOME}/bin/jboss-cli.sh" --connect command=":shutdown" >/dev/null 2>&1
          sleep 3
          # Mata procesos residuales si quedaron
          PIDS=$(pgrep -f "wildfly.*org.jboss.as")
          if [ -n "$PIDS" ]; then
            echo "Deteniendo procesos WildFly residuales: $PIDS"
            kill -9 $PIDS
          fi
          set -e
          echo "WildFly detenido (o ya estaba detenido)."
        '''
      }
    }

    stage('Deploy EAR') {
      steps {
        sh '''
          set -e
          echo "Limpiando carpeta deployments..."
          mkdir -p "${DEPLOYMENTS}"
          rm -f "${DEPLOYMENTS}"/*.failed "${DEPLOYMENTS}"/*.undeployed || true
          rm -f "${DEPLOYMENTS}/${EAR_NAME}" || true

          echo "Copiando EAR..."
          cp -f "${WORKSPACE}/savia-ear/target/${EAR_NAME}" "${DEPLOYMENTS}/${EAR_NAME}"

          # Forzar auto-deploy si aplica
          touch "${DEPLOYMENTS}/${EAR_NAME}.dodeploy" || true
          echo "EAR copiado a ${DEPLOYMENTS}/${EAR_NAME}"
        '''
      }
    }

    stage('Start WildFly') {
      steps {
        sh '''
          set -e
          chmod +x "${WILDFLY_HOME}/bin/"*.sh || true
          nohup "${WILDFLY_HOME}/bin/standalone.sh" -b ${BIND_ADDR} > "${WILDFLY_HOME}/standalone/nohup.out" 2>&1 &
          echo "WildFly iniciandose..."
          sleep 10
        '''
      }
    }

    stage('Verify Deployment') {
      steps {
        script {
          def logPath = "${WILDFLY_HOME}/standalone/log/server.log"
          timeout(time: 120, unit: 'SECONDS') {
            waitUntil {
              def exists = sh(script: "[ -f '${logPath}' ] && echo yes || echo no", returnStdout: true).trim()
              if (exists != 'yes') { sleep 3; return false }

              def tail = sh(script: "tail -n 400 '${logPath}'", returnStdout: true)

              if (tail.contains('.failed')) {
                error "El despliegue de ${env.EAR_NAME} fallo. Revisa ${logPath}."
              }

              if (tail.contains("Deployed \"${env.EAR_NAME}\"") || tail.contains("WFLYSRV0010: Deployed \"${env.EAR_NAME}\"")) {
                echo "EAR desplegado correctamente."
                return true
              }
              sleep 5
              return false
            }
          }
        }
      }
    }
  }

  post {
    always {
      sh 'ls -lh savia-ear/target || true'
      archiveArtifacts artifacts: 'savia-ear/target/*.ear', allowEmptyArchive: true
    }
    failure {
      sh 'tail -n 200 "${WILDFLY_HOME}/standalone/log/server.log" || true'
    }
  }
}
