pipeline {
  agent { label 'Linux' }

  tools { maven 'Maven'; jdk 'jdk11' }
  options { timestamps() }

  environment {
    MAVEN_FLAGS = '-B -U -DskipTests'
    // OJO: este directorio DEBE ser el deployments "real" del WildFly de Windows,
    // montado en el agente Linux (SMB/CIFS o similar).
    DEPLOY_DIR   = '/opt/deployments'
  }

  stages {
    stage('Checkout & Build') {
      steps {
        sh '''
          set -e
          echo "[BUILD] Compilando módulos..."
          mvn -f "$WORKSPACE/savia-negocio/pom.xml" clean install  ${MAVEN_FLAGS}
          mvn -f "$WORKSPACE/savia-ejb/pom.xml"     clean install  ${MAVEN_FLAGS}
          mvn -f "$WORKSPACE/savia-web/pom.xml"     clean package  ${MAVEN_FLAGS}
          mvn -f "$WORKSPACE/savia-ear/pom.xml"     clean package  ${MAVEN_FLAGS}

          echo "[BUILD] Detectando artefactos..."
          EAR=$(ls "$WORKSPACE/savia-ear/target/"*.ear | head -n1)
          WAR=$(ls "$WORKSPACE/savia-web/target/"*.war | head -n1)
          echo "EAR=$EAR"
          echo "WAR=$WAR"

          # Exportar nombres para etapas siguientes
          echo "BASENAME_EAR=$(basename "$EAR")" >  build.env
          echo "BASENAME_WAR=$(basename "$WAR")" >> build.env
          echo "PATH_EAR=$EAR"                   >> build.env
          echo "PATH_WAR=$WAR"                   >> build.env
        '''
        script {
          def props = readProperties file: 'build.env'
          env.BASENAME_EAR = props['BASENAME_EAR']
          env.BASENAME_WAR = props['BASENAME_WAR']
          env.PATH_EAR     = props['PATH_EAR']
          env.PATH_WAR     = props['PATH_WAR']
        }
      }
    }

    stage('Desplegar por File Scanner (exploded + .deployed)') {
      steps {
        sh '''
          set -e
          echo "[DEPLOY] Directorio de deployments: ${DEPLOY_DIR}"
          test -d "${DEPLOY_DIR}" || (echo "No existe ${DEPLOY_DIR}" && exit 1)

          echo "[DEPLOY] Limpiando restos anteriores..."
          rm -rf \
            "${DEPLOY_DIR}/${BASENAME_EAR}" \
            "${DEPLOY_DIR}/${BASENAME_EAR}.deployed" \
            "${DEPLOY_DIR}/${BASENAME_EAR}.failed" \
            "${DEPLOY_DIR}/${BASENAME_EAR}.isdeploying" \
            "${DEPLOY_DIR}/${BASENAME_EAR}.dodeploy" \
            "${DEPLOY_DIR}/${BASENAME_WAR}" \
            "${DEPLOY_DIR}/${BASENAME_WAR}.deployed" \
            "${DEPLOY_DIR}/${BASENAME_WAR}.failed" \
            "${DEPLOY_DIR}/${BASENAME_WAR}.isdeploying" \
            "${DEPLOY_DIR}/${BASENAME_WAR}.dodeploy" || true

          echo "[DEPLOY] Copiando artefactos..."
          cp -f "${PATH_EAR}" "${DEPLOY_DIR}/${BASENAME_EAR}"
          cp -f "${PATH_WAR}" "${DEPLOY_DIR}/${BASENAME_WAR}"

          echo "[DEPLOY] Disparando deployment (.dodeploy)..."
          touch "${DEPLOY_DIR}/${BASENAME_EAR}.dodeploy"
          touch "${DEPLOY_DIR}/${BASENAME_WAR}.dodeploy"

          echo "[DEPLOY] Esperando a que WildFly marque .deployed (y auto-explote a carpeta)..."
          for F in "${BASENAME_EAR}" "${BASENAME_WAR}"; do
            echo "  - Esperando ${F}..."
            i=0
            until [ -e "${DEPLOY_DIR}/${F}.deployed" ] || [ -e "${DEPLOY_DIR}/${F}.failed" ]; do
              sleep 2
              i=$((i+1))
              if [ $i -gt 120 ]; then
                echo "Timeout esperando a ${F}"; exit 1
              fi
            done
            if [ -e "${DEPLOY_DIR}/${F}.failed" ]; then
              echo "Fallo el despliegue de ${F}"; cat "${DEPLOY_DIR}/${F}.failed" || true; exit 1
            fi
            echo "  -> ${F} desplegado OK."
          done

          echo "[DEPLOY] Listado final en deployments:"
          ls -la "${DEPLOY_DIR}"
        '''
      }
    }
  }

  post {
    success { echo "? Build y deployment completados (scanner filesystem)." }
    failure { echo "? Falló el proceso. Revisa logs y marcadores .failed en ${env.DEPLOY_DIR}." }
  }
}
