pipeline {
  agent { label 'Linux' }   // Solo corre en agentes Linux

  tools { maven 'Maven'; jdk 'jdk11' }
  options { timestamps() }

  environment {
    MAVEN_FLAGS = '-B -U -DskipTests'
    EAR_NAME    = 'savia-ear.ear'
    ZIP_NAME    = 'savia-build.zip'
  }

  stages {
    stage('Checkout & Build') {
      steps {
        sh '''
          set -e
          echo "[BUILD] Compilando módulos..."

          mvn -f "$WORKSPACE/savia-negocio/pom.xml" clean install  ${MAVEN_FLAGS}
          mvn -f "$WORKSPACE/savia-ejb/pom.xml"     clean install  ${MAVEN_FLAGS}
          mvn -f "$WORKSPACE/savia-web/pom.xml"     clean install  ${MAVEN_FLAGS}
          mvn -f "$WORKSPACE/savia-ear/pom.xml"     clean package  ${MAVEN_FLAGS}

          echo "[BUILD] Organizando artefactos en carpeta dist..."
          rm -rf "$WORKSPACE/dist"
          mkdir -p "$WORKSPACE/dist"

          mkdir -p "$WORKSPACE/dist/savia-negocio"
          mkdir -p "$WORKSPACE/dist/savia-ejb"
          mkdir -p "$WORKSPACE/dist/savia-web"
          mkdir -p "$WORKSPACE/dist/savia-ear"

          cp "$WORKSPACE/savia-negocio/target/"*.jar "$WORKSPACE/dist/savia-negocio/" || true
          cp "$WORKSPACE/savia-ejb/target/"*.jar     "$WORKSPACE/dist/savia-ejb/" || true
          cp "$WORKSPACE/savia-web/target/"*.war     "$WORKSPACE/dist/savia-web/" || true

          EAR=$(ls "$WORKSPACE/savia-ear/target/"*.ear | head -n1)
          cp -f "$EAR" "$WORKSPACE/dist/savia-ear/${EAR_NAME}"

          echo "[BUILD] Generando ZIP con la estructura completa..."
          cd "$WORKSPACE/dist"
          zip -r "${ZIP_NAME}" *
        '''
        // Publicar ZIP en Jenkins
        archiveArtifacts artifacts: 'dist/savia-build.zip', fingerprint: true
      }
    }
  }

  post {
    success { echo "? Build compilado en Linux y empaquetado en ZIP con los 4 módulos." }
    failure { echo "? Falló el proceso. Revisa logs de compilación o empaquetado." }
  }
}
