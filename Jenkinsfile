pipeline {
  agent none   // no usar un agente global, cada stage decide

  options { timestamps() }

  environment {
    EAR_NAME     = 'savia-ear.ear'
    MAVEN_FLAGS  = '-B -U -DskipTests'
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
      tools {
        maven 'Maven'
        jdk   'jdk11'
      }
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
      }
    }

    stage('Deploy en Windows') {
      agent { label 'Windows' }
      steps {
        bat '''
          echo ? Deploy en Windows...
          set EAR_NAME=%EAR_NAME%
          set SRC=%WORKSPACE%\\savia-ear\\target\\%EAR_NAME%
          set DEPLOY_DIR=C:\\wildfly\\standalone\\deployments

          if exist "%DEPLOY_DIR%\\%EAR_NAME%" del /f "%DEPLOY_DIR%\\%EAR_NAME%"
          if exist "%DEPLOY_DIR%\\%EAR_NAME%.deployed" del /f "%DEPLOY_DIR%\\%EAR_NAME%.deployed"
          if exist "%DEPLOY_DIR%\\%EAR_NAME%.failed" del /f "%DEPLOY_DIR%\\%EAR_NAME%.failed"

          copy /Y "%SRC%" "%DEPLOY_DIR%\\%EAR_NAME%"
          type nul > "%DEPLOY_DIR%\\%EAR_NAME%.dodeploy"

          echo ? Copiado EAR en el scanner de WildFly
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
