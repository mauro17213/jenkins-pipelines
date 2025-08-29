pipeline {
  agent { label 'Linux' }

  tools {
    maven 'Maven'
    jdk   'jdk11'
  }

  options { timestamps() }

  environment {
    // WildFly local en el agente Linux (ajusta si está en otra ruta)
    WILDFLY_HOME = "${WORKSPACE}/wildfly-19.1.0.Final"
    BIND_ADDR    = '0.0.0.0'
    MGMT_HOST    = '127.0.0.1'
    MGMT_PORT    = '9990'

    EAR_NAME     = 'savia-ear.ear'
    MAVEN_FLAGS  = '-B -U -DskipTests'
  }

  stages {
    stage('Checkout SCM') { steps { checkout scm } }

    stage('Build (negocio ? ejb ? web ? ear)') {
      steps {
        sh '''
          set -e
          echo "? Compilando módulos..."
          mvn -f "$WORKSPACE/savia-negocio/pom.xml" clean install ${MAVEN_FLAGS}
          mvn -f "$WORKSPACE/savia-ejb/pom.xml"     clean install ${MAVEN_FLAGS}
          mvn -f "$WORKSPACE/savia-web/pom.xml"     clean install ${MAVEN_FLAGS}
          mvn -f "$WORKSPACE/savia-ear/pom.xml"     clean package ${MAVEN_FLAGS}

          EAR=$(ls "$WORKSPACE/savia-ear/target/"*.ear | head -n1 || true)
          if [ -z "$EAR" ]; then
            echo "? No se encontró .ear en savia-ear/target"; exit 1
          fi

          # Normaliza el nombre del artefacto para usarlo como runtime-name
          cp -f "$EAR" "$WORKSPACE/savia-ear/target/$EAR_NAME"
          echo "? EAR listo: $EAR_NAME"
        '''
      }
    }

    stage('Start WildFly') {
      steps {
        sh '''
          set -e
          chmod +x "$WILDFLY_HOME/bin/"*.sh || true

          # Intenta apagar por si quedó algo
          set +e
          "$WILDFLY_HOME/bin/jboss-cli.sh" --connect --commands=":shutdown" >/dev/null 2>&1
          set -e
          sleep 3

          echo "?? Iniciando WildFly..."
          nohup "$WILDFLY_HOME/bin/standalone.sh" -b ${BIND_ADDR} > "$WILDFLY_HOME/standalone/log/boot.out" 2>&1 &

          echo "? Esperando a que el mgmt llegue a RUNNING..."
          for i in $(seq 1 60); do
            STATE=$("$WILDFLY_HOME/bin/jboss-cli.sh" --connect --commands=":read-attribute(name=server-state)" 2>/dev/null | sed -n 's/.*result => \"\\(.*\\)\".*/\\1/p')
            if [ "$STATE" = "running" ]; then
              echo "? WildFly en estado RUNNING"; break
            fi
            sleep 2
            if [ $i -eq 60 ]; then echo "? WildFly no llegó a RUNNING"; exit 1; fi
          done
        '''
      }
    }

    stage('Deploy via CLI (sin dejar archivos)') {
      steps {
        sh '''
          set -e
          EAR_PATH="$WORKSPACE/savia-ear/target/$EAR_NAME"

          echo "? Desplegando ${EAR_NAME} con CLI (managed deployment)..."
          # --name / --runtime-name fijan el nombre lógico; --force hace redeploy si ya existe
          "$WILDFLY_HOME/bin/jboss-cli.sh" --connect --commands="deploy \"$EAR_PATH\" --name=${EAR_NAME} --runtime-name=${EAR_NAME} --force"

          # Verifica que el deployment exista y esté habilitado
          INFO=$("$WILDFLY_HOME/bin/jboss-cli.sh" --connect --commands="/deployment=${EAR_NAME}:read-resource(include-runtime=true)")
          echo "$INFO" | grep -q 'enabled => true' || { echo "? Deployment no quedó enabled"; echo "$INFO"; exit 1; }

          echo "? Eliminando artefacto local (no quedará en ninguna carpeta)"
          rm -f "$EAR_PATH"
        '''
      }
    }

    stage('Verificar en server.log') {
      steps {
        script {
          def logPath = "${env.WILDFLY_HOME}/standalone/log/server.log"
          timeout(time: 120, unit: 'SECONDS') {
            waitUntil {
              def ok = sh(script: "grep -qE 'Deployed \\\"${env.EAR_NAME}\\\"|WFLYSRV0010: Deployed \\\"${env.EAR_NAME}\\\"' '${logPath}' && echo yes || echo no", returnStdout: true).trim()
              if (ok == 'yes') {
                echo "? Despliegue OK según server.log"
                return true
              }
              def fail = sh(script: "grep -q '.failed' '${logPath}' && echo yes || echo no", returnStdout: true).trim()
              if (fail == 'yes') {
                error "? El despliegue de ${env.EAR_NAME} falló. Revisa ${logPath}."
              }
              sleep 3
              return false
            }
          }
        }
      }
    }
  }

  post {
    failure {
      sh 'tail -n 200 "$WILDFLY_HOME/standalone/log/server.log" || true'
    }
  }
}
