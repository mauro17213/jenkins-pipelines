pipeline {
  agent { label 'Linux' }
  options { timestamps() }

  environment {
    MAVEN_FLAGS = '-B -U -DskipTests'
    DIST_DIR    = 'dist'
    ZIP_NAME    = 'savia-build.zip'
  }

  stages {
    stage('Build y Empaquetado') {
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

        archiveArtifacts artifacts: "${env.DIST_DIR}/${env.ZIP_NAME}", fingerprint: true
      }
    }
  }

  post {
    success { echo '? Build en Linux terminado. ZIP generado y disponible para descargar.' }
    failure { echo '? Falló la compilación en Linux, revisa la consola.' }
  }
}
