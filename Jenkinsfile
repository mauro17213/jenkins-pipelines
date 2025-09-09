prueba
pipeline {
  agent { label 'Linux' }   // Solo corre en agentes Linux

  tools { maven 'Maven'; jdk 'jdk11' }
  options { timestamps() }

  environment {
    MAVEN_FLAGS = '-B -U -DskipTests'
    EAR_NAME    = 'savia-ear.ear'             // nombre fijo solo para el TAR (descarga)
    TAR_NAME    = 'savia-build.tar.gz'

    // Ruta montada desde Windows (compartida con Docker o SMB) o el deployments local
    DEPLOY_DIR   = '/opt/deployments'

    // Ruta a WildFly (si corre en el mismo agente Linux)
    WILDFLY_HOME = '/opt/wildfly'
    WF_HOST      = 'localhost'
    WF_PORT      = '9990'

    DIST_DIR     = 'dist'                     // carpeta con artefactos para descargar
    WAIT_LOOPS   = '180'                      // 180*2s = 6 minutos de espera a .deployed
  }

  stages {
    stage('Checkout & Build') {
      steps {
        checkout scm
        sh '''
          set -euo pipefail
          echo "[BUILD] Compilando módulos..."
          mvn -f "$WORKSPACE/savia-negocio/pom.xml" clean install  ${MAVEN_FLAGS}
          mvn -f "$WORKSPACE/savia-ejb/pom.xml"     clean install  ${MAVEN_FLAGS}
          mvn -f "$WORKSPACE/savia-web/pom.xml"     clean package  ${MAVEN_FLAGS}
          mvn -f "$WORKSPACE/savia-ear/pom.xml"     clean package  ${MAVEN_FLAGS}

          echo "[BUILD] Detectando artefactos reales..."
          EAR=$(ls "$WORKSPACE/savia-ear/target/"*.ear | head -n1)
          WAR=$(ls "$WORKSPACE/savia-web/target/"*.war | head -n1)
          BASENAME_EAR=$(basename "$EAR")
          BASENAME_WAR=$(basename "$WAR")

          mkdir -p "$WORKSPACE/${DIST_DIR}"

          # Copias para descarga (con nombres originales)
          cp -f "$EAR" "$WORKSPACE/${DIST_DIR}/${BASENAME_EAR}"
          cp -f "$WAR" "$WORKSPACE/${DIST_DIR}/${BASENAME_WAR}"

          # TAR combinado (mantengo tu variable TAR_NAME)
          (cd "$WORKSPACE/${DIST_DIR}" && tar -czf "${TAR_NAME}" "${BASENAME_EAR}" "${BASENAME_WAR}")

          # SHA opcional
          (cd "$WORKSPACE/${DIST_DIR}" && sha256sum * > SHA256SUMS.txt || true)

          # Exporto nombres a un envfile para las etapas siguientes
          {
            echo "BASENAME_EAR=${BASENAME_EAR}"
            echo "BASENAME_WAR=${BASENAME_WAR}"
          } > build.env
        '''
        script {
          def p = readProperties file: 'build.env'
          env.BASENAME_EAR = p['BASENAME_EAR']
          env.BASENAME_WAR = p['BASENAME_WAR']
        }

        // Publicar todo para descargar desde Jenkins
        archiveArtifacts artifacts: "${env.DIST_DIR}/*", fingerprint: true
      }
    }

    stage('Iniciar WildFly') {
      steps {
        sh '''
          set -euo pipefail
          if [ -x "${WILDFLY_HOME}/bin/standalone.sh" ]; then
            if pgrep -f "org.jboss.as.standalone" >/dev/null 2>&1; then
              echo "[WILDFLY] Ya está corriendo."
            else
              echo "[WILDFLY] Iniciando servidor..."
              nohup ${WILDFLY_HOME}/bin/standalone.sh -b 0.0.0.0 > ${WILDFLY_HOME}/standalone/log/jenkins-boot.log 2>&1 &
              echo "[WILDFLY] Arrancado en background."
              sleep 10
            fi
          else
            echo "[WILDFLY] Saltando (no existe ${WILDFLY_HOME}/bin/standalone.sh)."
          fi
        '''
      }
    }

    stage('Desplegar por File Scanner (deja pegado en deployments)') {
      steps {
        sh '''
          set -euo pipefail
          echo "[DEPLOY] Usando deployments: ${DEPLOY_DIR}"
          test -d "${DEPLOY_DIR}" || (echo "No existe ${DEPLOY_DIR}" && exit 1)

          echo "[DEPLOY] Limpiando restos anteriores SOLO de esta app..."
          rm -rf \
            "${DEPLOY_DIR}/${BASENAME_EAR}" \
            "${DEPLOY_DIR}/${BASENAME_EAR}.dodeploy" \
            "${DEPLOY_DIR}/${BASENAME_EAR}.deployed" \
            "${DEPLOY_DIR}/${BASENAME_EAR}.failed" \
            "${DEPLOY_DIR}/${BASENAME_EAR}.isdeploying" \
            "${DEPLOY_DIR}/${BASENAME_WAR}" \
            "${DEPLOY_DIR}/${BASENAME_WAR}.dodeploy" \
            "${DEPLOY_DIR}/${BASENAME_WAR}.deployed" \
            "${DEPLOY_DIR}/${BASENAME_WAR}.failed" \
            "${DEPLOY_DIR}/${BASENAME_WAR}.isdeploying" || true

          echo "[DEPLOY] Copiando EAR y WAR al deployments real..."
          cp -f "${WORKSPACE}/${DIST_DIR}/${BASENAME_EAR}" "${DEPLOY_DIR}/${BASENAME_EAR}"
          cp -f "${WORKSPACE}/${DIST_DIR}/${BASENAME_WAR}" "${DEPLOY_DIR}/${BASENAME_WAR}"

          echo "[DEPLOY] Disparando deploy con .dodeploy..."
          : > "${DEPLOY_DIR}/${BASENAME_EAR}.dodeploy"
          : > "${DEPLOY_DIR}/${BASENAME_WAR}.dodeploy"

          echo "[DEPLOY] Esperando a que WildFly marque .deployed (o .failed)..."
          for F in "${BASENAME_EAR}" "${BASENAME_WAR}"; do
            i=0
            until [ -e "${DEPLOY_DIR}/${F}.deployed" ] || [ -e "${DEPLOY_DIR}/${F}.failed" ]; do
              sleep 2
              i=$((i+1))
              if [ $i -ge ${WAIT_LOOPS} ]; then
                echo "Timeout esperando a ${F}"; exit 1
              fi
            done
            if [ -e "${DEPLOY_DIR}/${F}.failed" ]; then
              echo "Fallo el despliegue de ${F}"; cat "${DEPLOY_DIR}/${F}.failed" || true; exit 1
            fi
            echo "  -> ${F} desplegado OK."
          done

          echo "[DEPLOY] Contenido final en deployments:"
          ls -la "${DEPLOY_DIR}"
        '''
      }
    }

    stage('Deploy via jboss-cli (opcional)') {
      when { expression { return fileExists("${env.WILDFLY_HOME}/bin/jboss-cli.sh") } }
      steps {
        withCredentials([usernamePassword(credentialsId: 'wildfly-admin', usernameVariable: 'WF_USER', passwordVariable: 'WF_PASS')]) {
          sh '''
            echo "[DEPLOY] (Opcional) jboss-cli --force sobre el EAR (no necesario si usas .dodeploy)"
            ${WILDFLY_HOME}/bin/jboss-cli.sh \
              --connect --controller=${WF_HOST}:${WF_PORT} \
              --user=${WF_USER} --password=${WF_PASS} \
              --command="deploy ${DEPLOY_DIR}/${BASENAME_EAR} --force"
          '''
        }
      }
    }
  }

  post {
    success { echo "? Compilado, artefactos publicados y copiados a deployments (.dodeploy creado)." }
    failure { echo "? Falló el proceso. Revisa la consola y los .failed en ${env.DEPLOY_DIR}." }
  }
}
