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
    HTTP_PORT    = '8080'
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
          [ -z "$EAR" ] && { echo "? No se encontró .ear en savia-ear/target"; exit 1; }
          cp -f "$EAR" "$WORKSPACE/savia-ear/target/$EAR_NAME"
          echo "? EAR listo: $EAR_NAME"
        '''
      }
    }

    stage('Start WildFly (liberar 8080, mgmt 9990)') {
      steps {
        sh '''
          set -e
          chmod +x "$WILDFLY_HOME/bin/"*.sh || true

          echo "? Intentando apagar cualquier WildFly previo por CLI..."
          set +e
          "$WILDFLY_HOME/bin/jboss-cli.sh" --connect --controller=${MGMT_HOST}:${MGMT_PORT} --command="shutdown" >/dev/null 2>&1
          set -e
          sleep 2

          echo "? Si 8080 aún está ocupado, mataré el proceso..."
          if ss -lntp "( sport = :${HTTP_PORT} )" | grep -q ${HTTP_PORT}; then
            PID=$(ss -lntp "( sport = :${HTTP_PORT} )" | awk -F',' '/users/ {sub(/.*pid=/,"",$2); sub(/\\).*/,"",$2); print $2}' | head -n1)
            [ -n "$PID" ] && { echo "  - matando PID $PID"; kill -9 "$PID" || true; }
            sleep 2
          fi

          # Kill de respaldo por firma (si quedara zombie)
          pgrep -f "jboss-modules.jar -mp $WILDFLY_HOME/modules" | xargs -r kill -9 || true
          sleep 1

          # Verificación final
          if ss -lntp "( sport = :${HTTP_PORT} )" | grep -q ${HTTP_PORT}; then
            echo "! El puerto ${HTTP_PORT} sigue ocupado. Abortando."
            ss -lntp "( sport = :${HTTP_PORT} )" || true
            exit 1
          fi

          echo "?? Iniciando WildFly en 0.0.0.0:${HTTP_PORT} (mgmt ${MGMT_HOST}:${MGMT_PORT})…"
          nohup "$WILDFLY_HOME/bin/standalone.sh" \
            -b 0.0.0.0 \
            -Djboss.bind.address.management=${MGMT_HOST} \
            -Djboss.http.port=${HTTP_PORT} \
            > "$WILDFLY_HOME/standalone/log/boot.out" 2>&1 &

          echo $! > .wf.pid

          LOG="$WILDFLY_HOME/standalone/log/server.log"

          echo "? Esperando a que la interfaz de administración responda (HTTP 200/401) o señales en el log..."
          for i in $(seq 1 60); do
            CODE=$(curl -s -o /dev/null -w "%{http_code}" "http://${MGMT_HOST}:${MGMT_PORT}/management" || true)
            if [ "$CODE" = "200" ] || [ "$CODE" = "401" ]; then
              echo "? Management arriba (HTTP $CODE)"
              break
            fi
            if [ -f "$LOG" ] && grep -Eq "WFLYSRV0060: Http management interface listening|WFLYSRV0010: Deployed|WFLYUT0012: Started server default-server" "$LOG"; then
              echo "? Servidor en marcha (detectado por log)"
              break
            fi
            sleep 2
            if [ $i -eq 60 ]; then
              echo "! Timeout esperando a WildFly en ${MGMT_HOST}:${MGMT_PORT}"
              tail -n 200 "$LOG" || true
              exit 1
            fi
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

          echo "? Limpiando deployment previo…"
          rm -f "$APP" "$APP.deployed" "$APP.failed" "$APP.undeployed" "$APP.dodeploy" || true

          echo "? Copiando EAR al scanner…"
          mkdir -p "$DEPLOY_DIR"
          cp -f "$SRC" "$APP"
          : > "$APP.dodeploy"

          echo "? Esperando .deployed/.failed…"
          for i in $(seq 1 90); do
            [ -f "$APP.deployed" ] && { echo "? Deploy OK (.deployed)"; break; }
            if [ -f "$APP.failed" ]; then
              echo "! Deploy FAILED (.failed). Últimas líneas de log:"
              tail -n 200 "$LOG" || true
              exit 1
            fi
            sleep 2
            [ $i -eq 90 ] && { echo "! Timeout esperando el resultado del deploy"; tail -n 200 "$LOG" || true; exit 1; }
          done

          echo "? Borrando el EAR y marker para no dejar archivos…"
          rm -f "$APP" "$APP.deployed" || true
        '''
      }
    }

    stage('Show App URL') {
      steps {
        sh '''
          PUB_IP=$(hostname -I | awk '{for(i=1;i<=NF;i++) if ($i!="127.0.0.1"){print $i; exit}}')
          [ -z "$PUB_IP" ] && PUB_IP=$(ip -4 -o addr show | awk '!/127\\.0\\.0\\.1/ {print $4}' | head -n1 | cut -d/ -f1)

          echo "? URL local:     http://127.0.0.1:${HTTP_PORT}/savia"
          echo "? URL pública:   http://${PUB_IP}:${HTTP_PORT}/savia"
          echo "? Probar home del servidor (HTTP code esperado 200):"
          curl -s -o /dev/null -w "HTTP %{http_code}\\n" "http://127.0.0.1:${HTTP_PORT}/"
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
    always {
      sh '''
        set +e
        echo "? post: intentando shutdown por CLI..."
        "$WILDFLY_HOME/bin/jboss-cli.sh" --connect --controller=${MGMT_HOST}:${MGMT_PORT} --command="shutdown" >/dev/null 2>&1 || true
        sleep 1

        if [ -f .wf.pid ]; then
          echo "? post: matando PID $(cat .wf.pid)"
          kill -9 "$(cat .wf.pid)" 2>/dev/null || true
          rm -f .wf.pid
        fi

        echo "? post: verificando puerto ${HTTP_PORT}..."
        if ss -lntp "( sport = :${HTTP_PORT} )" | grep -q ${HTTP_PORT}; then
          PID=$(ss -lntp "( sport = :${HTTP_PORT} )" | awk -F',' '/users/ {sub(/.*pid=/,"",$2); sub(/\\).*/,"",$2); print $2}' | head -n1)
          [ -n "$PID" ] && { echo "  - matando PID residual $PID"; kill -9 "$PID" 2>/dev/null || true; }
        fi
      '''
    }
    failure {
      sh 'tail -n 200 "$WILDFLY_HOME/standalone/log/server.log" || true'
    }
  }
}
