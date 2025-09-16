pipeline {
  agent none
  options { timestamps() }

  environment {
    MAVEN_FLAGS = '-B -U -DskipTests'
    DIST_DIR    = 'dist'
    TAR_NAME    = 'savia-build.tar.gz'
    WAIT_LOOPS  = '180'   // 180 * 2s = 6 min de espera a .deployed
  }

  triggers {
    githubPush()   // ? Dispara el pipeline al recibir push desde GitHub webhook
  }

  stages {
    stage('Build (Linux)') {
      agent { label 'ubuntu-agent' }
      tools { maven 'Maven'; jdk 'jdk-11' }
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

          mkdir -p "$WORKSPACE/${DIST_DIR}"
          cp -f "$EAR" "$WORKSPACE/${DIST_DIR}/$(basename "$EAR")"
          cp -f "$WAR" "$WORKSPACE/${DIST_DIR}/$(basename "$WAR")"
          (cd "$WORKSPACE/${DIST_DIR}" && tar -czf "${TAR_NAME}" "$(basename "$EAR")" "$(basename "$WAR")") || true

          echo "[BUILD] Contenido de ${DIST_DIR}:"
          ls -la "$WORKSPACE/${DIST_DIR}"
        '''
        archiveArtifacts artifacts: "${env.DIST_DIR}/*", fingerprint: true
        stash name: 'dist', includes: "${env.DIST_DIR}/**"
      }
    }
  }

  post {
    success { echo '? Build en Linux, luego start WildFly + deploy & RUN en Windows.' }
    failure { echo "? Revisa la consola y los *.failed en el deployments de Windows." }
  }
}
