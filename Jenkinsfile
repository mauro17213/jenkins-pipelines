pipeline {
  agent { label 'Linux' }   // ? Solo corre en agentes Linux

  tools { maven 'Maven'; jdk 'jdk11' }
  options { timestamps() }

  environment {
    MAVEN_FLAGS = '-B -U -DskipTests'
    EAR_NAME    = 'savia-ear.ear'
    TAR_NAME    = 'savia-build.tar.gz'

    // Ruta montada desde Windows (compartida con Docker)
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
          mvn -f "$WORKSPACE/savia-web/pom.xml"     clean install  ${MAVEN_FLAGS}
          mvn -f "$WORKSPACE/savia-ear/pom.xml"     clean package  ${MAVEN_FLAGS}

          echo "[BUILD] Copiando artefactos compilados..."
          mkdir -p "$WORKSPACE/dist"

          cp "$WORKSPACE/savia-negocio/target/"*.jar "$WORKSPACE/dist/" || true
          cp "$WORKSPACE/savia-ejb/target/"*.jar     "$WORKSPACE/dist/" || true
          cp "$WORKSPACE/savia-web/target/"*.war     "$WORKSPACE/dist/" || true

          EAR=$(ls "$WORKSPACE/savia-ear/target/"*.ear | head -n1)
          cp -f "$EAR" "$WORKSPACE/dist/${EAR_NAME}"

          echo "[BUILD] Generando TAR con todo compilado..."
          cd "$WORKSPACE/dist"
          tar -czf "${TAR_NAME}" *
        '''
        // Publicar TAR en Jenkins
        archiveArtifacts artifacts: 'dist/savia-build.tar.gz', fingerprint: true
      }
    }
  }

  post {
    success { echo "? Build compilado en Linux, empaquetado en TAR, publicado en Jenkins." }
    failure { echo "? Falló el proceso. Revisa logs de compilación o despliegue." }
  }
}
