pipeline {
  agent { label 'Windows' }

  tools {
    jdk   'jdk11'     // nombre exacto en Global Tool Configuration
    maven 'Maven'     // nombre exacto en Global Tool Configuration
  }

  parameters {
    booleanParam(name: 'USE_DOCKER_DB',     defaultValue: true,  description: 'Levantar MySQL en Docker para pruebas')
    booleanParam(name: 'DEPLOY_TO_WILDFLY', defaultValue: false, description: 'Desplegar al finalizar')
  }

  environment {
    MAVEN_OPTS   = '-Dmaven.wagon.http.pool=false -Djava.awt.headless=true'
    WF_HOST      = 'localhost'
    WF_PORT      = '9990'
    WILDFLY_HOME = 'C:\\wildfly-19.1.0.Final'
  }

  options { timestamps() }

  stages {

    stage('Checkout') {
      steps { checkout scm }
    }

    stage('Database (MySQL)') {
      when { expression { params.USE_DOCKER_DB } }
      steps {
        bat '''
          docker ps -a --format "{{.Names}}" | findstr /I "^ci-mysql$" >nul && (
            docker start ci-mysql
          ) || (
            docker run -d --name ci-mysql -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=appdb -p 3306:3306 mysql:8
          )
        '''
      }
    }

    stage('Build all modules') {
      steps {
        // Compilamos todo con repo local en el workspace
        bat '''
          cd savia-negocio && mvn -B -U -DskipTests clean install  -Dmaven.repo.local=%WORKSPACE%\\.m2 && cd ..
          cd savia-ejb     && mvn -B -U -DskipTests clean install  -Dmaven.repo.local=%WORKSPACE%\\.m2 && cd ..
          cd savia-web     && mvn -B -U -DskipTests clean install  -Dmaven.repo.local=%WORKSPACE%\\.m2 && cd ..
          cd savia-ear     && mvn -B -U -DskipTests clean package  -Dmaven.repo.local=%WORKSPACE%\\.m2 && cd ..
        '''
      }
    }

    stage('Create ZIP (compiled targets)') {
      steps {
        script {
          // nombre con fecha/hora + número de build
          env.ZIP_NAME = "compiled_${new Date().format('yyyy-MM-dd_HH-mm-ss')}_build-${env.BUILD_NUMBER}.zip"
        }
        // Usamos el step nativo de Jenkins para PowerShell (no 'bat powershell ...')
        powershell '''
          Remove-Item -Recurse -Force dist -ErrorAction SilentlyContinue

          New-Item -ItemType Directory -Force `
            -Path 'dist\\savia-ejb\\target','dist\\savia-negocio\\target','dist\\savia-web\\target','dist\\savia-ear\\target' | Out-Null

          Copy-Item -Recurse -Force 'savia-ejb\\target\\*'      'dist\\savia-ejb\\target'      -ErrorAction SilentlyContinue
          Copy-Item -Recurse -Force 'savia-negocio\\target\\*'  'dist\\savia-negocio\\target'  -ErrorAction SilentlyContinue
          Copy-Item -Recurse -Force 'savia-web\\target\\*'      'dist\\savia-web\\target'      -ErrorAction SilentlyContinue
          Copy-Item -Recurse -Force 'savia-ear\\target\\*'      'dist\\savia-ear\\target'      -ErrorAction SilentlyContinue

          if (Test-Path $env:ZIP_NAME) { Remove-Item -Force $env:ZIP_NAME }
          Compress-Archive -Path 'dist\\*' -DestinationPath $env:ZIP_NAME -Force
        '''
      }
    }

    stage('Archive ZIP') {
      steps {
        archiveArtifacts artifacts: "${env.ZIP_NAME}", fingerprint: true
      }
    }

    stage('Deploy to WildFly') {
      when { expression { params.DEPLOY_TO_WILDFLY } }
      steps {
        withCredentials([usernamePassword(credentialsId: 'wildfly-admin', usernameVariable: 'WF_USER', passwordVariable: 'WF_PASS')]) {
          bat '''
            set "JBOSS_CLI=%WILDFLY_HOME%\\bin\\jboss-cli.bat"
            IF EXIST "savia-ear\\target" (
              FOR /F "delims=" %%F IN ('dir /B /S "savia-ear\\target\\*.ear"') DO set "DEPLOY_FILE=%%F"
            )
            IF NOT DEFINED DEPLOY_FILE (
              FOR /F "delims=" %%F IN ('dir /B /S "savia-web\\target\\*.war"') DO set "DEPLOY_FILE=%%F"
            )
            echo Deploying %DEPLOY_FILE%
            "%JBOSS_CLI%" --controller=%WF_HOST%:%WF_PORT% --user=%WF_USER% --password=%WF_PASS% --connect --command="deploy \\"%DEPLOY_FILE%\\" --force"
          '''
        }
      }
    }
  }

  post {
    success {
      echo "? Pipeline OK. ZIP: ${env.ZIP_NAME} (descárgalo desde *Artifacts* en la página del build)."
    }
    failure {
      echo "? Falló la compilación"
    }
    always {
      script {
        if (params.USE_DOCKER_DB) {
          bat 'docker ps --format "{{.Names}}" | findstr /I "^ci-mysql$" >nul && docker stop ci-mysql'
        }
      }
    }
  }
}
