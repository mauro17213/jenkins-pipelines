pipeline {
  agent { label 'Linux' }

  tools {
    maven 'Maven'
    jdk   'jdk11'
  }

  environment {
    // === BUILD LOCAL ===
    EAR_NAME     = 'savia-ear.ear'
    ZIP_OUT_DIR  = "${WORKSPACE}/dist"
    ZIP_NAME     = "savia-modulos.zip"

    // === REMOTO WINDOWS ===
    WIN_HOST        = 'MI-SERVIDOR-WINDOWS'          // <-- CAMBIA AQUÍ (hostname o IP)
    REMOTE_WF_MNT   = '/mnt/wildfly'                 // punto de montaje en Linux
    REMOTE_WF_ROOT  = "${REMOTE_WF_MNT}"             // mapearemos C:\wildfly-19.1.0.Final a /mnt/wildfly
    REMOTE_DEPLOY   = "${REMOTE_WF_ROOT}/standalone/deployments"
    REMOTE_LOG      = "${REMOTE_WF_ROOT}/standalone/log/server.log"
  }

  stages {
    stage('Checkout SCM') { steps { checkout scm } }

    stage('Build Modules') {
      steps {
        script {
          echo '? Compilando módulos (negocio ? ejb ? web ? ear)...'
          sh "mvn -f \"${WORKSPACE}/savia-negocio/pom.xml\" clean install -DskipTests -B -U"
          sh "mvn -f \"${WORKSPACE}/savia-ejb/pom.xml\"     clean install -DskipTests -B -U"
          sh "mvn -f \"${WORKSPACE}/savia-web/pom.xml\"     clean install -DskipTests -B -U"
          sh "mvn -f \"${WORKSPACE}/savia-ear/pom.xml\"     clean package   -DskipTests -B -U"

          def generatedEar = sh(script: "ls ${WORKSPACE}/savia-ear/target/*.ear | head -n 1", returnStdout: true).trim()
          if (!generatedEar) { error "No se encontró el EAR en target" }
          sh "cp -f \"${generatedEar}\" \"${WORKSPACE}/savia-ear/target/${EAR_NAME}\""
          echo "? EAR listo como ${EAR_NAME}"
        }
      }
    }

    stage('Package ZIP with modules') {
      steps {
        sh """
          set -e
          rm -rf "${ZIP_OUT_DIR}" && mkdir -p "${ZIP_OUT_DIR}"
          cp -f ${WORKSPACE}/savia-ejb/target/*.jar      "${ZIP_OUT_DIR}/" || true
          cp -f ${WORKSPACE}/savia-negocio/target/*.jar  "${ZIP_OUT_DIR}/" || true
          cp -f ${WORKSPACE}/savia-web/target/*.war      "${ZIP_OUT_DIR}/" || true
          cp -f ${WORKSPACE}/savia-ear/target/*.ear      "${ZIP_OUT_DIR}/" || true
          cd "${ZIP_OUT_DIR}"
          jar -cfM "${ZIP_NAME}" .
          echo "?? ZIP generado en: ${ZIP_OUT_DIR}/${ZIP_NAME}"
        """
      }
    }

    stage('Publicar ZIP') {
      steps { archiveArtifacts artifacts: 'dist/savia-modulos.zip', fingerprint: true }
    }

    stage('Montar share de WildFly (Windows)') {
      steps {
        withCredentials([usernamePassword(credentialsId: 'win-smb-creds',
                                          usernameVariable: 'WIN_USER',
                                          passwordVariable: 'WIN_PASS')]) {
          sh """
            set -e
            # Instalar cifs-utils si falta
            if ! command -v mount.cifs >/dev/null 2>&1; then
              if command -v apt-get >/dev/null 2>&1; then
                sudo apt-get update && sudo apt-get install -y cifs-utils
              elif command -v yum >/dev/null 2>&1; then
                sudo yum install -y cifs-utils
              elif command -v apk >/dev/null 2>&1; then
                sudo apk add --no-cache cifs-utils
              else
                echo "? No puedo instalar cifs-utils automáticamente. Instálalo en el agente."
                exit 1
              fi
            fi

            sudo mkdir -p "${REMOTE_WF_MNT}"
            # Desmontar si estaba montado
            sudo umount "${REMOTE_WF_MNT}" >/dev/null 2>&1 || true

            # Montar C$\\wildfly-19.1.0.Final en /mnt/wildfly
            sudo mount -t cifs //${WIN_HOST}/C\\$/wildfly-19.1.0.Final "${REMOTE_WF_MNT}" \\
              -o username="${WIN_USER}",password="${WIN_PASS}",vers=3.0,sec=ntlmssp,uid=$(id -u),gid=$(id -g),file_mode=0664,dir_mode=0775

            echo "? Montado //${WIN_HOST}/C$/wildfly-19.1.0.Final -> ${REMOTE_WF_MNT}"
          """
        }
      }
    }

    stage('Desplegar EXPLODEADO al deployments') {
      steps {
        sh """
          set -e
          # Asegurar unzip
          if ! command -v unzip >/dev/null 2>&1; then
            if command -v apt-get >/dev/null 2>&1; then sudo apt-get update && sudo apt-get install -y unzip;
            elif command -v yum     >/dev/null 2>&1; then sudo yum install -y unzip;
            elif command -v apk     >/dev/null 2>&1; then sudo apk add --no-cache unzip;
            else echo "? Instala 'unzip' en el agente"; exit 1; fi
          fi

          APP_DIR="${REMOTE_DEPLOY}/${EAR_NAME}"

          echo "? Limpiando despliegue previo…"
          rm -rf "${APP_DIR}" \\
                 "${APP_DIR}.dodeploy" \\
                 "${APP_DIR}.deployed" \\
                 "${APP_DIR}.failed" \\
                 "${APP_DIR}.undeployed" || true

          echo "? Creando carpeta explodeada: ${APP_DIR}"
          mkdir -p "${APP_DIR}"

          echo "? Descomprimiendo EAR en ${APP_DIR}"
          unzip -q "${WORKSPACE}/savia-ear/target/${EAR_NAME}" -d "${APP_DIR}"

          echo "? Marcando .dodeploy"
          : > "${APP_DIR}.dodeploy"
        """
      }
    }

    stage('Verificar despliegue (scanner)') {
      steps {
        script {
          def appDir = "${env.REMOTE_DEPLOY}/${env.EAR_NAME}"
          def logPath = "${env.REMOTE_LOG}"

          timeout(time: 180, unit: 'SECONDS') {
            waitUntil {
              def state = sh(script: """
                if [ -f "${appDir}.deployed" ]; then echo OK;
                elif [ -f "${appDir}.failed" ]; then echo FAIL;
                else echo WAIT; fi
              """, returnStdout: true).trim()

              if (state == 'OK') {
                echo "? Deploy OK (.deployed encontrado)"
                return true
              }
              if (state == 'FAIL') {
                echo "? Deploy FAILED (.failed encontrado). Últimas líneas de server.log:"
                sh "tail -n 200 '${logPath}' || true"
                error "El despliegue de ${env.EAR_NAME} falló."
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
      sh """
        echo "? Contenido dist:"
        ls -lh '${ZIP_OUT_DIR}' || true

        echo "? Desmontando share…"
        sudo umount '${REMOTE_WF_MNT}' || true
      """
    }
  }
}
