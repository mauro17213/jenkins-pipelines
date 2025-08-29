pipeline {
  agent { label 'Linux' }

  tools {
    maven 'Maven'   // Cambia al nombre configurado en Jenkins
    jdk   'jdk11'   // O 'jdk17'
  }

  options { timestamps() }

  environment {
    WILDFLY_HOME = "${WORKSPACE}/wildfly-19.1.0.Final"
    DEPLOYMENTS  = "${WILDFLY_HOME}/standalone/deployments"
    EAR_NAME     = "savia-ear.ear"
    WEB_NAME     = "savia-web.war"
    MAVEN_FLAGS  = "-B -U -DskipTests"

    // Datos DB por defecto (ajusta)
    DB_HOST = "127.0.0.1"
    DB_PORT = "3306"
    DB_NAME = "system_savia"

    // JNDI y nombre lógico del datasource
    DS_JNDI = "java:/jboss/datasources/system_savia"
    DS_NAME = "system_savia"

    BIND_ADDR = "0.0.0.0"
  }

  stages {
    stage('Checkout SCM') { steps { checkout scm } }

    stage('Bootstrap parent POM (si EJB lo exige)') {
      steps {
        sh '''
          set -e
          EJB_POM="${WORKSPACE}/savia-ejb/pom.xml"
          if [ -f "$EJB_POM" ] && grep -q "<parent>" "$EJB_POM"; then
            REL=$(sed -n 's#.*<relativePath>\\(.*\\)</relativePath>.*#\\1#p' "$EJB_POM" | head -n1)
            [ -z "$REL" ] && REL="../pom.xml"
            PARENT_POM="$(dirname "$EJB_POM")/$REL"
            if [ -f "$PARENT_POM" ]; then
              echo "[parent] Instalando POM padre encontrado en: $PARENT_POM"
              mvn -f "$PARENT_POM" clean install -DskipTests -B -U
            else
              echo "[parent] ERROR: El EJB declara un POM padre pero no existe en la ruta relativa: $REL"
              echo "        Opciones: (a) commitear ese POM padre en el repo; (b) quitar <parent> del EJB e independizar versiones."
              exit 1
            fi
          else
            echo "[parent] EJB sin bloque <parent> o POM no encontrado; continuo."
          fi
        '''
      }
    }

    stage('Build savia-negocio') {
      steps {
        sh 'mvn -f "${WORKSPACE}/savia-negocio/pom.xml" clean install ${MAVEN_FLAGS}'
      }
    }

    stage('Build savia-ejb') {
      steps {
        sh 'mvn -f "${WORKSPACE}/savia-ejb/pom.xml" clean install ${MAVEN_FLAGS}'
      }
    }

    stage('Package savia-ear') {
      steps {
        sh 'mvn -f "${WORKSPACE}/savia-ear/pom.xml" clean package ${MAVEN_FLAGS}'
      }
    }

    stage('Package savia-web') {
      steps {
        sh 'mvn -f "${WORKSPACE}/savia-web/pom.xml" clean package ${MAVEN_FLAGS}'
      }
    }

    stage('Resolver artefactos (.ear/.war)') {
      steps {
        script {
          def earPath = sh(script: 'ls -1 "${WORKSPACE}/savia-ear/target"/*.ear | head -n1', returnStdout: true).trim()
          if (!earPath) { error "No .ear en savia-ear/target" }
          def warPath = sh(script: 'ls -1 "${WORKSPACE}/savia-web/target"/*.war | head -n1', returnStdout: true).trim()
          if (!warPath) { error "No .war en savia-web/target" }
          sh '''
            mkdir -p "${WORKSPACE}/dist"
            cp -f "${WORKSPACE}/savia-ear/target"/*.ear "${WORKSPACE}/dist/${EAR_NAME}"
            cp -f "${WORKSPACE}/savia-web/target"/*.war "${WORKSPACE}/dist/${WEB_NAME}"
          '''
        }
      }
    }

    stage('Start WildFly') {
      steps {
        sh '''
          set +e
          "${WILDFLY_HOME}/bin/jboss-cli.sh" --connect command=:shutdown >/dev/null 2>&1
          set -e
          chmod +x "${WILDFLY_HOME}/bin/"*.sh || true
          nohup "${WILDFLY_HOME}/bin/standalone.sh" -b ${BIND_ADDR} > "${WILDFLY_HOME}/standalone/nohup.out" 2>&1 &
          sleep 10
        '''
      }
    }

    stage('Configurar DataSource system_savia') {
      steps {
        withCredentials([usernamePassword(credentialsId: 'mysql-cred', usernameVariable: 'DB_USER', passwordVariable: 'DB_PASS')]) {
          sh '''
            set -e
            # Detectar driver mysql disponible (deployment o module)
            DRIVER_NAME=$("${WILDFLY_HOME}/bin/jboss-cli.sh" --connect '/subsystem=datasources:read-children-names(child-type=jdbc-driver)' \
              | tr -d '\\r' | grep -i mysql | head -n1 | tr -d '[]" ,')

            if [ -z "$DRIVER_NAME" ]; then
              echo "[ds] WARNING: No se detecto driver mysql en WildFly. Intentare usar 'mysql'."
              DRIVER_NAME="mysql"
            fi

            # Crear/asegurar datasource
            DS_EXISTS=$("${WILDFLY_HOME}/bin/jboss-cli.sh" --connect "/subsystem=datasources/data-source=${DS_NAME}:read-resource" 2>/dev/null; echo $?)
            if [ "$DS_EXISTS" -eq 0 ]; then
              echo "[ds] DataSource ${DS_NAME} ya existe; lo dejo como esta."
            else
              echo "[ds] Creando DataSource ${DS_NAME} en ${DS_JNDI} con driver ${DRIVER_NAME}"
              "${WILDFLY_HOME}/bin/jboss-cli.sh" --connect <<EOF
batch
/subsystem=datasources/data-source=${DS_NAME}:add(jndi-name=${DS_JNDI}, driver-name=${DRIVER_NAME}, connection-url=jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}?useSSL=false&characterEncoding=UTF-8&serverTimezone=UTC, user-name=${DB_USER}, password=${DB_PASS}, min-pool-size=5, max-pool-size=30, enabled=true)
run-batch
:reload
EOF
            fi
          '''
        }
      }
    }

    stage('Deploy EAR y WAR') {
      steps {
        sh '''
          mkdir -p "${DEPLOYMENTS}"
          rm -f "${DEPLOYMENTS}"/*.failed "${DEPLOYMENTS}"/*.undeployed || true
          rm -f "${DEPLOYMENTS}/${EAR_NAME}" "${DEPLOYMENTS}/${WEB_NAME}" || true

          cp -f "${WORKSPACE}/dist/${EAR_NAME}" "${DEPLOYMENTS}/${EAR_NAME}"
          cp -f "${WORKSPACE}/dist/${WEB_NAME}" "${DEPLOYMENTS}/${WEB_NAME}"

          touch "${DEPLOYMENTS}/${EAR_NAME}.dodeploy" || true
          touch "${DEPLOYMENTS}/${WEB_NAME}.dodeploy" || true
        '''
      }
    }

    stage('Verificar despliegue') {
      steps {
        script {
          def logPath = "${WILDFLY_HOME}/standalone/log/server.log"
          timeout(time: 180, unit: 'SECONDS') {
            waitUntil {
              def exists = sh(script: "[ -f '${logPath}' ] && echo yes || echo no", returnStdout: true).trim()
              if (exists != 'yes') { sleep 3; return false }
              def tail = sh(script: "tail -n 800 '${logPath}'", returnStdout: true)

              if (tail.contains('.failed')) { error "WildFly reporta .failed en deployments. Revisa " + logPath }

              def earOk = tail.contains("Deployed \"${EAR_NAME}\"") || tail.contains("WFLYSRV0010: Deployed \"${EAR_NAME}\"")
              def webOk = tail.contains("Deployed \"${WEB_NAME}\"") || tail.contains("WFLYSRV0010: Deployed \"${WEB_NAME}\"")

              if (earOk && webOk) { return true }
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
      sh 'ls -lh savia-web/target || true'
      archiveArtifacts artifacts: 'savia-ear/target/*.ear, savia-web/target/*.war, dist/*', allowEmptyArchive: true
    }
    failure {
      sh 'tail -n 200 "${WILDFLY_HOME}/standalone/log/server.log" || true'
    }
  }
}
