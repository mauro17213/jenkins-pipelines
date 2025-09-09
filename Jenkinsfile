pipeline {
  agent { label 'Linux' }   // Solo corre en agentes Linux
  tools { maven 'Maven'; jdk 'jdk11' }
  options { timestamps() }

  environment {
    MAVEN_FLAGS = '-B -U -DskipTests'
    TAR_NAME    = 'savia-build.tar.gz'   // solo para descargar desde Jenkins
    DEPLOY_DIR  = '/opt/deployments'     // carpeta de Windows montada en Linux
    DIST_DIR    = 'dist'                 // donde guardo artefactos para descargar
    WAIT_LOOPS  = '180'                  // 180*2s = 6 min esperando .deployed
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

          echo "[BUILD] Detectando artefactos reales (con nombre de versión)..."
          EAR=$(ls "$WORKSPACE/savia-ear/target/"*.ear | head -n1)
          WAR=$(ls "$WORKSPACE/savia-web/target/"*.war | head -n1)
          BASENAME_EAR=$(basename "$EAR")   # p.ej. savia-ear-1.0-SNAPSHOT.ear
          BASENAME_WAR=$(basename "$WAR")   # p.ej. savia-web-1.0-SNAPSHOT.war

          mkdir -p "$WORKSPACE/${DIST_DIR}"
          # Copias para descarga (con nombres ORIGINALES, sin renombrar)
          cp -f "$EAR" "$WORKSPACE/${DIST_DIR}/${BASENAME_EAR}"
          cp -f "$WAR" "$WORKSPACE/${DIST_DIR}/${BASENAME_WAR}"

          # Paquete tar.gz para descargar desde Jenkins (opcional)
          (cd "$WORKSPACE/${DIST_DIR}" && tar -czf "${TAR_NAME}" "${BASENAME_EAR}" "${BASENAME_WAR}")

          # Exporto nombres para la siguiente etapa
          {
            echo "BASENAME_EAR=${BASENAME_EAR}"
            echo "BASENAME_WAR=${BASENAME_WAR}"
          } > build.env

          echo "[BUILD] Artefactos en ${DIST_DIR}:"
          ls -la "$WORKSPACE/${DIST_DIR}"
        '''
        archiveArtifacts artifacts: "${env.DIST_DIR}/*", fingerprint: true
      }
    }

    stage('Copiar a deployments (exploded + .deployed)') {
      steps {
        sh '''
          set -euo pipefail
          source build.env

          echo "[DEPLOY] Verificando deployments montado: ${DEPLOY_DIR}"
          test -d "${DEPLOY_DIR}" || (echo "No existe ${DEPLOY_DIR}" && exit 1)

          echo "[DEPLOY] Limpiando restos solo de esta app..."
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

          echo "[DEPLOY] Copiando artefactos con su NOMBRE ORIGINAL..."
          cp -f "${DIST_DIR}/${BASENAME_EAR}" "${DEPLOY_DIR}/${BASENAME_EAR}"
          cp -f "${DIST_DIR}/${BASENAME_WAR}" "${DEPLOY_DIR}/${BASENAME_WAR}"

          echo "[DEPLOY] Disparando el scanner con .dodeploy..."
          : > "${DEPLOY_DIR}/${BASENAME_EAR}.dodeploy"
          : > "${DEPLOY_DIR}/${BASENAME_WAR}.dodeploy"

          echo "[DEPLOY] Esperando a .deployed (o .failed)..."
          for F in "${BASENAME_EAR}" "${BASENAME_WAR}"; do
            i=0
            until [ -e "${DEPLOY_DIR}/${F}.deployed" ] || [ -e "${DEPLOY_DIR}/${F}.failed" ]; do
              sleep 2; i=$((i+1))
              if [ $i -ge ${WAIT_LOOPS} ]; then echo "Timeout esperando a ${F}"; exit 1; fi
            done
            if [ -e "${DEPLOY_DIR}/${F}.failed" ]; then
              echo "Fallo el despliegue de ${F}"; cat "${DEPLOY_DIR}/${F}.failed" || true; exit 1
            fi
            # Confirmar que se explotó a carpeta como en tu primera imagen
            if [ -d "${DEPLOY_DIR}/${F}" ]; then
              echo "OK: ${F} desplegado y EXPLOTADO a carpeta."
            else
              echo "ADVERTENCIA: ${F} desplegado pero NO explotado. Revisa que auto-extract=true."
            fi
          done

          echo "[DEPLOY] Contenido final:"
          ls -la "${DEPLOY_DIR}"
        '''
      }
    }
  }

  post {
    success { echo "? Compilado, empaquetado para descarga y pegado en deployments con nombres originales (como la primera imagen)." }
    failure { echo "? Falló el proceso. Revisa consola y los .failed en ${env.DEPLOY_DIR}." }
  }
}
