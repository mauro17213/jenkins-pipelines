pipeline {
  agent none   // Cada stage decide su agente

  tools {
    maven 'Maven'
    jdk   'jdk11'
  }

  options { timestamps() }

  environment {
    EAR_NAME    = 'savia-ear.ear'
    MAVEN_FLAGS = '-B -U -DskipTests'
  }

  stages {

    stage('Checkout SCM') {
      agent { label 'Linux' }
      steps {
        checkout scm
      }
    }

    stage('Build en Linux') {
      agent { label 'Linux' }
      steps {
        sh '''
          set -e
          echo "? Compilando módulos en Linux..."
          mvn -f "$WORKSPACE/savia-negocio/pom.xml" clean install ${MAVEN_FLAGS}
          mvn -f "$WORKSPACE/savia-ejb/pom.xml"     clean install ${MAVEN_FLAGS}
          mvn -f "$WORKSPACE/savia-web/pom.xml"     clean install ${MAVEN_FLAGS}
          mvn -f "$WORKSPACE/savia-ear/pom.xml"     clean package ${MAVEN_FLAGS}

          EAR=$(ls "$WORKSPACE/savia-ear/target/"*.ear | head -n1 || true)
          [ -z "$EAR" ] && { echo "? No se encontró .ear en savia-ear/target"; exit 1; }
          cp -f "$EAR" "$WORKSPACE/savia-ear/target/$EAR_NAME"
          echo "? EAR listo: $EAR_NAME"
        '''
        stash includes: 'savia-ear/target/savia-ear.ear', name: 'ear'
      }
    }

    stage('Deploy en Windows') {
      agent { label 'Windows' }
      steps {
        unstash 'ear'
        bat '''
          echo ? Deploy en Windows...
          set EAR_NAME=%EAR_NAME%
          set SRC=%WORKSPACE%\\savia-ear\\target\\%EAR_NAME%
          set DEPLOY_DIR=C:\\wildfly-19.1.0.Final\\standalone\\deployments

          echo ? Revisando si el EAR existe en %SRC%
          dir "%SRC%"

          echo ?? Limpiando deploy previo...
          if exist "%DEPLOY_DIR%\\%EAR_NAME%" del /f "%DEPLOY_DIR%\\%EAR_NAME%"
          if exist "%DEPLOY_DIR%\\%EAR_NAME%.deployed" del /f "%DEPLOY_DIR%\\%EAR_NAME%.deployed"
          if exist "%DEPLOY_DIR%\\%EAR_NAME%.failed" del /f "%DEPLOY_DIR%\\%EAR_NAME%.failed"
          if exist "%DEPLOY_DIR%\\%EAR_NAME%.undeployed" del /f "%DEPLOY_DIR%\\%EAR_NAME%.undeployed"
          if exist "%DEPLOY_DIR%\\%EAR_NAME%.dodeploy" del /f "%DEPLOY_DIR%\\%EAR_NAME%.dodeploy"

          echo ? Copiando EAR...
          copy /Y "%SRC%" "%DEPLOY_DIR%\\%EAR_NAME%"

          echo ? Creando marker .dodeploy...
          type nul > "%DEPLOY_DIR%\\%EAR_NAME%.dodeploy"

          echo ? Deploy solicitado en WildFly
        '''
      }
    }
  }

  post {
    failure {
      echo "? El pipeline falló, revisar logs"
    }
  }
}
