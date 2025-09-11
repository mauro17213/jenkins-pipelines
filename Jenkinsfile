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

          echo "[BUILD] Copiando todos los target/..."
          mkdir -p "$WORKSPACE/${DIST_DIR}/artefactos"
          cp -r "$WORKSPACE/savia-negocio/target" "$WORKSPACE/${DIST_DIR}/artefactos/savia-negocio-target"
          cp -r "$WORKSPACE/savia-ejb/target"     "$WORKSPACE/${DIST_DIR}/artefactos/savia-ejb-target"
          cp -r "$WORKSPACE/savia-web/target"     "$WORKSPACE/${DIST_DIR}/artefactos/savia-web-target"
          cp -r "$WORKSPACE/savia-ear/target"     "$WORKSPACE/${DIST_DIR}/artefactos/savia-ear-target"

          echo "[BUILD] Generando ZIP con todos los target..."
          cd "$WORKSPACE/${DIST_DIR}"
          zip -r "${ZIP_NAME}" artefactos
        '''

        archiveArtifacts artifacts: "${env.DIST_DIR}/${env.ZIP_NAME}", fingerprint: true
      }
    }
  }

  post {
    success { echo '? Build en Linux terminado. ZIP con todos los target generado y disponible para descargar.' }
    failure { echo '? Falló la compilación en Linux, revisa la consola.' }
  }
}
