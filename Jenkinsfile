pipeline {
  agent none
  options { timestamps() }

  environment {
    MAVEN_FLAGS = '-B -U -DskipTests'
    DIST_DIR    = 'dist'
    ZIP_NAME    = 'savia-build.zip'
    WAIT_LOOPS  = '180'   // 180 * 2s = 6 min de espera a .deployed
  }

  stages {
    // ------------------ BUILD EN LINUX ------------------
    stage('Build (Linux)') {
      agent { label 'Linux' }
      tools { maven 'Maven'; jdk 'jdk11' }
      steps {
        checkout scm
        sh '''
          #!/bin/sh
          set -eu
          echo "[BUILD] Compilando módulos..."
          mvn -f "$WORKSPACE/savia-negocio/pom.xml" clean install  ${MAVEN_FLAGS}
          mvn -f "$WORKSPACE/savia-ejb/pom.xml"     clean install  ${MAVEN_FLAGS}
          mvn -f "$WORKSPACE/savia-web/pom.xml"     clean package  ${MAVEN_FLAGS}
          mvn -f "$WORKSPACE/savia-ear/pom.xml"     clean package  ${MAVEN_FLAGS}

          echo "[BUILD] Detectando artefactos..."
          EAR=$(ls "$WORKSPACE/savia-ear/target/"*.ear | head -n1)
          WAR=$(ls "$WORKSPACE/savia-web/target/"*.war | head -n1)

          mkdir -p "$WORKSPACE/${DIST_DIR}/artefactos"
          cp -f "$EAR" "$WORKSPACE/${DIST_DIR}/artefactos/$(basename "$EAR")"
          cp -f "$WAR" "$WORKSPACE/${DIST_DIR}/artefactos/$(basename "$WAR")"

          echo "[BUILD] Generando ZIP..."
          cd "$WORKSPACE/${DIST_DIR}"
          zip -r "${ZIP_NAME}" artefactos
        '''

        // Publica el ZIP en Jenkins para descarga
        archiveArtifacts artifacts: "${env.DIST_DIR}/${env.ZIP_NAME}", fingerprint: true
        stash name: 'dist', includes: "${env.DIST_DIR}/**"
      }
    }
  }

  post {
    success { echo '? Build en Linux terminado. ZIP generado y listo para descargar.' }
    failure { echo '? Revisa la consola y los *.failed en el deployments de Windows.' }
  }
}
