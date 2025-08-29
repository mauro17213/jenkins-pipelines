pipeline {
  agent { label 'Linux' }

  tools {
    maven 'Maven'   // Ajusta al nombre real en Manage Jenkins > Global Tool Configuration
    jdk   'jdk11'   // Cambia a 'jdk17' si ya migraste
  }

  options { timestamps(); ansiColor('xterm') }

  environment {
    // Ajusta esta ruta si WildFly está en otro lugar
    WILDFLY_HOME = "${WORKSPACE}/wildfly-19.1.0.Final"
    DEPLOYMENTS  = "${WILDFLY_HOME}/standalone/deployments"

    // Nombres fijos con los que se copiarán los artefactos al deployments/
    EAR_NAME     = 'savia-ear.ear'
    WEB_NAME     = 'savia-web.war'

    // Flags maven
    MAVEN_FLAGS  = '-B -U -DskipTests'

    // Bind address del server
    BIND_ADDR    = '0.0.0.0'
  }

  stages {

    stage('Checkout SCM') {
      steps { checkout scm }
    }

    // Compilación sin POM padre, módulo por módulo, en orden explícito
    stage('Build savia-negocio') {
      steps {
        sh """
          echo '? Compilando savia-negocio...'
          mvn -f "${WORKSPACE}/savia-negocio/pom.xml" clean install ${MAVEN_FLAGS}
        """
      }
    }

    stage('Build savia-ejb') {
      steps {
        sh """
          echo '? Compilando savia-ejb...'
          mvn -f "${WORKSPACE}/savia-ejb/pom.xml" clean install ${MAVEN_FLAGS}
        """
      }
    }

    stage('Package savia-ear') {
      steps {
        sh """
          echo '? Empaquetando savia-ear...'
          mvn -f "${WORKSPACE}/savia-ear/pom.xml" clean package ${MAVEN_FLAGS}
        """
      }
    }

    stage('Build/Package savia-web') {
      steps {
        sh """
          echo '? Empaquetando savia-web...'
          mvn -f "${WORKSPACE}/savia-web/pom.xml" clean package ${MAVEN_FLAGS}
        """
      }
    }

    stage('Resolver artefactos') {
      steps {
        script {
          // Detecta el .ear y .war generados para copiarlos con nombres fijos
          def earPath = sh(script: "ls -1 \"${WORKSPACE}/savia-ear/target\"/*.ear | head -n1", returnStdout: true).trim()
          if (!earPath) { error "No se encontró un .ear en savia-ear/target" }

          def warPath = sh(script: "ls -1 \"${WORKSPACE}/savia-web/target\"/*.war | head -n1", returnStdout: true).trim()
          if (!warPath) { error "No se encontró un .war en savia-web/target" }

          echo "EAR detectado: ${earPath}"
          echo "WAR detectado: ${warPath}"

          // Copia con nombres consistentes para fácil verificación en logs
          sh """
            mkdir -p "${WORKSPACE}/dist"
            cp -f "${earPath}" "${WORKSPACE}/dist/${EAR_NAME}"
            cp -f "${warPath}" "${WORKSPACE}/dist/${WEB_NAME}"
          """
        }
      }
    }

    stage('Stop WildFly (si está corriendo)') {
      steps {
        sh """
          set +e
          "${WILDFLY_HOME}/bin/jboss-cli.sh" --connect command=:shutdown
          sleep 5
          set -e
          echo '? WildFly detenido (o no estaba corriendo).'
        """
      }
    }

    stage('Deploy EAR y WAR a deployments/') {
      steps {
        sh """
          echo '? Limpiando deployments previos...'
          mkdir -p "${DEPLOYMENTS}"
          rm -f "${DEPLOYMENTS}"/*.failed "${DEPLOYMENTS}"/*.undeployed || true
          rm -f "${DEPLOYMENTS}/${EAR_NAME}" "${DEPLOYMENTS}/${WEB_NAME}" || true

          echo '? Copiando artefactos...'
          cp -f "${WORKSPACE}/dist/${EAR_NAME}" "${DEPLOYMENTS}/${EAR_NAME}"
          cp -f "${WORKSPACE}/dist/${WEB_NAME}" "${DEPLOYMENTS}/${WEB_NAME}"

          # Marcadores para forzar auto-deploy si la instancia lo requiere
          touch "${DEPLOYMENTS}/${EAR_NAME}.dodeploy" || true
          touch "${DEPLOYMENTS}/${WEB_NAME}.dodeploy" || true
        """
      }
    }

    stage('Start WildFly') {
      steps {
        sh """
          chmod +x "${WILDFLY_HOME}/bin/"*.sh || true
          nohup "${WILDFLY_HOME}/bin/standalone.sh" -b ${BIND_ADDR} > "${WILDFLY_HOME}/standalone/nohup.out" 2>&1 &
          echo '? WildFly iniciándose en background...'
          # Da tiempo a que arranque el proceso y empiece a escribir logs
          sleep 10
        """
      }
    }

    stage('Verificar despliegue EAR y WEB') {
      steps {
        script {
          def logPath = "${WILDFLY_HOME}/standalone/log/server.log"
          timeout(time: 180, unit: 'SECONDS') {
            waitUntil {
              // Espera a que exista el log
              def exists = sh(script: "[ -f '${logPath}' ] && echo yes || echo no", returnStdout: true).trim()
              if (exists != 'yes') { sleep 3; return false }

              def tail = sh(script: "tail -n 800 '${logPath}'", returnStdout: true)

              // Si el servidor marca fallos de despliegue, aborta con mensaje claro
              if (tail.contains('.failed')) {
                error "? WildFly reporta .failed en deployments. Revisa ${logPath}."
              }

              // Chequea que ambos artefactos se hayan desplegado
              def earOk = tail.contains("Deployed \"${EAR_NAME}\"") || tail.contains("WFLYSRV0010: Deployed \"${EAR_NAME}\"")
              def webOk = tail.contains("Deployed \"${WEB_NAME}\"") || tail.contains("WFLYSRV0010: Deployed \"${WEB_NAME}\"")

              if (earOk && webOk) {
                echo '? Despliegue OK de EAR y WEB.'
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
      echo '? Archivos generados:'
      sh 'ls -lh savia-ear/target || true'
      sh 'ls -lh savia-web/target || true'
      archiveArtifacts artifacts: 'savia-ear/target/*.ear, savia-web/target/*.war, dist/*', allowEmptyArchive: true
    }
    failure {
      echo '?? Últimas líneas del server.log para diagnosticar:'
      sh 'tail -n 200 "${WILDFLY_HOME}/standalone/log/server.log" || true'
    }
  }
}
