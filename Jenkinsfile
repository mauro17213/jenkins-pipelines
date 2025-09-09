pipeline {
  agent { label 'Linux' }   // Solo corre en agentes Linux

  tools { maven 'Maven'; jdk 'jdk11' }
  options { timestamps() }

  environment {
    MAVEN_FLAGS = '-B -U -DskipTests'
    TAR_NAME    = 'savia-build.tar.gz'  // para descargar desde Jenkins
    DEPLOY_DIR  = '/opt/deployments'    // carpeta de Windows montada en Linux
    DIST_DIR    = 'dist'                // artefactos para descarga
    WAIT_LOOPS  = '180'                 // 180*2s = 6 min esperando .deployed
  }

  stages {
    stage('Checkout & Build') {
      steps {
        checkout scm
        sh '''
          #!/bin/sh
          set -eu                             # (sin pipefail porque /bin/sh no lo soporta)

          echo "[BUILD] Compilando módulos..."
          mvn -f "$WORKSPACE/savia-negocio/pom.xml" clean install  ${MAVEN_FLAGS}
          mvn -f "$WORKSPACE/savia-ejb/pom.xml"     clean install  ${MAVEN_FLAGS}
          mvn -f "$WORKSPACE/savia-web/pom.xml"     clean package  ${MAVEN_FLAGS}
          mvn -f "$WORKSPACE/savia-ear/pom.xml"     clean package  ${MAVEN_FLAGS}

          echo "[BUILD] Detectando artefactos con nombre de versión..."
          EAR=$(ls "$WORKSPACE/savia-ear/target/"*.ear | head -n1)
          WAR=$(ls "$WORKSPACE/savia-web/target/"*.war | head -n1)
          BASENAME_EAR=$(basename "$EAR")   # p.ej. savia-ear-1.0-SNAPSHOT.ear
          BASENAME_WAR=$(basename "$WAR")   # p.ej. savia-web-1.0-SNAPSHOT.war

          mkdir -p "$WORKSPACE/${DIST_DIR}"
          # Copias para descarga (mantener nombres ORIGINALES)
          cp -f "$EAR" "$WORKSPACE/${DIST_DIR}/${BASENAME_EAR}"
          cp -f "$WAR" "$WORKSPACE/${DIST_DIR}/${BASENAME_WAR}"

          # Paquete tar.gz opcional para descargar
          (
            cd "$WORKSPACE/${DIST_DIR}" && tar -czf "${TAR_NAME}" "${BASENAME_EAR}" "${BASENAME_WAR}"
          ) || true

          # Exportar nombres a variables de entorno (Jenkins las inyecta en el siguiente step)
          echo "BASENAME_EAR=${BASENAME_EAR}" >  build.env
          echo "BASENAME_WAR=${BASENAME_WAR}" >> build.env
        '''
        script {
          def p = readProperties file: 'build.env'
          env.BASENAME_EAR = p['BASENAME_EAR']
          env.BASENAME_WAR = p['BASENAME_WAR']
        }
        archiveArtifacts artifacts: "${env.DIST_DIR}/*", fingerprint: true
      }
    }

    stage('Copiar a deployments (exploded + .deployed)') {
      steps {
        sh '''
          #!/bin/sh
          set -eu

          echo "[DEPLOY] Verificando deployments montado: ${DEPLOY_DIR}"
          [ -d "${DEPLOY_DIR}" ] || { echo "No existe ${DEPLOY_DIR}"; exit 1; }

          echo "[DEPLOY] Limpiando restos de esta app..."
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
            "${DEPLOY_DIR}/${BASENAME_WAR}.isdeploying" 2>/dev/null || true

          echo "[DEPLOY] Copiando artefactos con nombres ORIGINALES..."
          cp -f "${DIST_DIR}/${BASENAME_EAR}" "${DEPLOY_DIR}/${BASENAME_EAR}"
          cp -f "${DIST_DIR}/${BASENAME_WAR}" "${DEPLOY_DIR}/${BASENAME_WAR}"

          echo "[DEPLOY] Disparando scanner con .dodeploy..."
          : > "${DEPLOY_DIR}/${BASENAME_EAR}.dodeploy"
          : > "${DEPLOY_DIR}/${BASENAME_WAR}.dodeploy"

          echo "[DEPLOY] Esperando a .deployed (o .failed)..."
          for F in "${BASENAME_EAR}" "${BASENAME_WAR}"; do
            i=0
            while [ ! -e "${DEPLOY_DIR}/${F}.deployed" ] && [ ! -e "${DEPLOY_DIR}/${F}.failed" ]; do
              sleep 2; i=$((i+1))
              if [ "$i" -ge "${WAIT_LOOPS}" ]; then echo "Timeout esperando a ${F}"; exit 1; fi
            done
            if [ -e "${DEPLOY_DIR}/${F}.failed" ]; then
              echo "Fallo el despliegue de ${F}"; cat "${DEPLOY_DIR}/${F}.failed" || true; exit 1
            fi
            if [ -d "${DEPLOY_DIR}/${F}" ]; then
              echo "OK: ${F} desplegado y EXPLOTADO a carpeta."
            else
              echo "ADVERTENCIA: ${F} desplegado pero NO explotado (revisa auto-extract=true)."
            fi
          done

          echo "[DEPLOY] Contenido final en ${DEPLOY_DIR}:"
          ls -la "${DEPLOY_DIR}"
        '''
      }
    }
  }

  post {
    success { echo "? Compilado, artefactos publicados y pegados en deployments con nombre de versión (como tu primera imagen)." }
    failure { echo "? Falló el proceso. Revisa la consola y los .failed en ${env.DEPLOY_DIR}." }
  }
}
