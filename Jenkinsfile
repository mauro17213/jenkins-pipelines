pipeline {
  agent { label 'Linux' }

  tools {
    maven 'Maven'
    jdk   'jdk11'
  }

  options { timestamps() }

  environment {
    WILDFLY_HOME = "${WORKSPACE}/wildfly-19.1.0.Final"
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
          echo "? Compilando m�dulos..."
          mvn -f "$WORKSPACE/savia-negocio/pom.xml" clean install ${MAVEN_FLAGS}
          mvn -f "$WORKSPACE/savia-ejb/pom.xml"     clean install ${MAVEN_FLAGS}
          mvn -f "$WORKSPACE/savia-web/pom.xml"     clean install ${MAVEN_FLAGS}
          mvn -f "$WORKSPACE/savia-ear/pom.xml"     clean package ${MAVEN_FLAGS}

          EAR=$(ls "$WORKSPACE/savia-ear/target/"*.ear | head -n1 || true)
          [ -z "$EAR" ] && { echo "? No se encontr� .ear en savia-ear/target"; exit 1; }
          cp -f "$EAR" "$WORKSPACE/savia-ear/target/$EAR_NAME"
          echo "? EAR listo: $EAR_NAME"
        '''
      }
    }

    stage('Start WildFly (8080, mgmt 9990)') {
      steps {
        sh '''
          set -e
          chmod +x "$WILDFLY_HOME/bin/"*.sh || true

          # 1) apaga cualquier WF previo y limpia
          set +e
          "$WILDFLY_HOME/bin/jboss-cli.sh" --connect --commands=":shutdown" >/dev/null 2>&1
          pgrep -f "jboss-modules.jar -mp $WILDFLY_HOME/modules" | xargs -r kill -9
          set -e
          sleep 2

          echo "?? Iniciando WildFly en 0.0.0.0:8080 (mgmt 127.0.0.1:9990)�"
          nohup "$WILDFLY_HOME/bin/standalone.sh" \
            -b 0.0.0.0 \
            -Djboss.bind.address.management=127.0.0.1 \
            > "$WILDFLY_HOME/standalone/log/boot.out" 2>&1 &

          # 2) espera a que suba: mgmt HTTP (200/401) o se�ales en server.log
          LOG="$WILDFLY_HOME/standalone/log/server.log"
          for i in $(seq 1 60); do
            CODE=$(curl -s -o /dev/null -w "%{http_code}" "http://${MGMT_HOST}:${MGMT_PORT}/management" || true)
            if [ "$CODE" = "200" ] || [ "$CODE" = "401" ]; then
              echo "? Management arriba (HTTP $CODE)"; break
            fi
            if [ -f "$LOG" ] && grep -Eq "WFLYSRV0021: Http management interface|WFLYSRV0025:.* Undertow|WFLYSRV0010: Deployed" "$LOG"; then
              echo "? Servidor en marcha (detectado por log)"; break
            fi
            sleep 2
            [ $i -eq 60 ] && { echo "? WildFly no lleg� a RUNNING"; tail -n 200 "$LOG" || true; exit 1; }
          done
        '''
      }
    }

    stage('Deploy via Scanner (sin dejar archivos)') {
      steps {
        sh '''
          set -e
          DEPLOY_DIR="$WILDFLY_HOME/standalone/deployments"
          APP="$DEPLOY_DIR/$EAR_NAME"
          LOG="$WILDFLY_HOME/standalone/log/server.log"
          SRC="$WORKSPACE/savia-ear/target/$EAR_NAME"

          echo "? Limpiando deployment previo�"
          rm -f "$APP" "$APP.deployed" "$APP.failed" "$APP.undeployed" "$APP.dodeploy" || true

          echo "? Copiando EAR al scanner�"
          mkdir -p "$DEPLOY_DIR"
          cp -f "$SRC" "$APP"
          : > "$APP.dodeploy"

          echo "? Esperando .deployed/.failed�"
          for i in $(seq 1 90); do
            [ -f "$APP.deployed" ] && { echo "? Deploy OK (.deployed)"; break; }
            if [ -f "$APP.failed" ]; then
              echo "? Deploy FAILED (.failed). �ltimas l�neas:"
              tail -n 200 "$LOG" || true
              exit 1
            fi
            sleep 2
            [ $i -eq 90 ] && { echo "? Timeout esperando el resultado del deploy"; tail -n 200 "$LOG" || true; exit 1; }
          done

          echo "? Borrando el EAR y marker para no dejar archivos�"
          rm -f "$APP" "$APP.deployed" || true
        '''
      }
    }

    stage('Show App URL') {
      steps {
        sh '''
          echo "? URL local: http://127.0.0.1:8080/savia"
          echo "? Probar localmente (desde el agente):"
          curl -s -o /dev/null -w "HTTP %{http_code}\\n" "http://127.0.0.1:8080/"
        '''
      }
    }

    stage('Verificar en server.log') {
      steps {
        sh '''
          LOG="$WILDFLY_HOME/standalone/log/server.log"
          echo "? �ltimas l�neas de server.log:"
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
