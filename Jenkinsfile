pipeline {
  agent { label 'Linux' }

  tools {
    maven 'Maven'
    jdk   'jdk11'
  }

  options { timestamps() }

  environment {
    // WildFly local en el agente Linux
    WILDFLY_HOME = "${WORKSPACE}/wildfly-19.1.0.Final"
    MGMT_HOST    = '127.0.0.1'
    MGMT_PORT    = '9990'     // puerto por defecto de management

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
          cp -f "$EAR" "$WORKSPACE/savia-ear/target/$EAR_NAME"
          echo "? EAR listo: $EAR_NAME"
        '''
      }
    }

    stage('Start WildFly (sin offset)') {
      steps {
        sh '''
          set -e
          chmod +x "$WILDFLY_HOME/bin/"*.sh || true

          # Intenta apagar una instancia previa
          set +e
          "$WILDFLY_HOME/bin/jboss-cli.sh" --connect --commands=":shutdown" >/dev/null 2>&1
          # Mata cualquier proceso de este WILDFLY_HOME
          pgrep -f "jboss-modules.jar -mp $WILDFLY_HOME/modules" | xargs -r kill -9
          set -e
          sleep 2

          echo "?? Iniciando WildFly en 0.0.0.0:8080 (mgmt 9990)..."
          nohup "$WILDFLY_HOME/bin/standalone.sh" \
            -b 0.0.0.0 \
            -Djboss.bind.address.management=127.0.0.1 \
            > "$WILDFLY_HOME/standalone/log/boot.out" 2>&1 &

          # Asegurar curl
          if ! command -v curl >/dev/null 2>&1; then
            if   command -v apt-get >/dev/null 2>&1; then sudo apt-get update && sudo apt-get install -y curl
            elif command -v yum     >/dev/null 2>&1; then sudo yum install -y curl
            elif command -v apk     >/dev/null 2>&1; then sudo apk add --no-cache curl
            else echo "?? No pude instalar curl automáticamente"; fi
          fi

          echo "? Esperando management en ${MGMT_HOST}:${MGMT_PORT}..."
          for i in $(seq 1 60); do
            CODE=$(curl -s -o /dev/null -w "%{http_code}" "http://${MGMT_HOST}:${MGMT_PORT}/management" || true)
            # 200 o 401 significan que la interfaz HTTP de management ya está arriba
            if [ "$CODE" = "200" ] || [ "$CODE" = "401" ]; then
              echo "? Management arriba (HTTP $CODE)"; break
            fi
            sleep 2
            if [ $i -eq 60 ]; then
              echo "? Management no respondió a tiempo en ${MGMT_HOST}:${MGMT_PORT}"
              exit 1
            fi
          done
        '''
      }
    }

    stage('Deploy via File Scanner (sin dejar archivos)') {
      steps {
        sh '''
          set -e
          DEPLOY_DIR="$WILDFLY_HOME/standalone/deployments"
          APP="$DEPLOY_DIR/$EAR_NAME"
          LOG="$WILDFLY_HOME/standalone/log/server.log"
          SRC="$WORKSPACE/savia-ear/target/$EAR_NAME"

          echo "? Limpiando deployment previo..."
          rm -f "$APP" "$APP.deployed" "$APP.failed" "$APP.undeployed" "$APP.dodeploy" || true

          echo "? Copiando EAR al scanner..."
          mkdir -p "$DEPLOY_DIR"
          cp -f "$SRC" "$APP"

          echo "? Marcando .dodeploy"
          : > "$APP.dodeploy"

          echo "? Esperando .deployed/.failed..."
          for i in $(seq 1 90); do
            if [ -f "$APP.deployed" ]; then
              echo "? Deploy OK (.deployed)"
              break
            fi
            if [ -f "$APP.failed" ]; then
              echo "? Deploy FAILED (.failed). Últimas líneas:"
              tail -n 200 "$LOG" || true
              exit 1
            fi
            sleep 2
            if [ $i -eq 90 ]; then
              echo "? Timeout esperando el resultado del deploy"
              tail -n 200 "$LOG" || true
              exit 1
            fi
          done

          echo "? Borrando el EAR y el marker para no dejar archivos..."
          rm -f "$APP" "$APP.deployed" || true
        '''
      }
    }

    stage('Verificar en server.log') {
      steps {
        sh '''
          LOG="$WILDFLY_HOME/standalone/log/server.log"
          echo "? Últimas líneas de server.log:"
          tail -n 120 "$LOG" || true
        '''
      }
    }
  }

  post {
    failure {
      sh 'tail -n 200 "$WILDFLY_HOME/standalone/log/server.log" || true'
    }
  }
}
