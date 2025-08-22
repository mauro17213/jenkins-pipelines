pipeline {
  agent { label 'Windows' }

  tools {
    jdk   'jdk11'    // nombre EXACTO en Global Tool Configuration
    maven 'Maven'    // nombre EXACTO en Global Tool Configuration
  }

  options {
    timestamps()
    skipDefaultCheckout()
  }

  parameters {
    booleanParam(name: 'USE_DOCKER_DB',     defaultValue: true,  description: 'Levantar MySQL en Docker para pruebas')
    booleanParam(name: 'DEPLOY_TO_WILDFLY', defaultValue: false, description: 'Desplegar al finalizar')
  }

  environment {
    MAVEN_OPTS   = '-Dmaven.wagon.http.pool=false -Djava.awt.headless=true'
    WF_HOST      = 'localhost'
    WF_PORT      = '9990'
    WILDFLY_HOME = 'C:\\wildfly-19.1.0.Final'   // usa doble backslash o slashes
  }

  // Si ya configuraste "GitHub hook trigger for GITScm polling" en el job, no necesitas triggers aquí.
  // triggers { pollSCM('H/2 * * * *') } // alternativa si NO usas webhook

  stages {

    stage('Checkout') {
      steps { checkout scm }
    }

    stage('Database (MySQL)') {
      when { expression { params.USE_DOCKER_DB } }
      steps {
        // Idempotente: crea si no existe, inicia si está detenido, no hace nada si ya corre
        powershell '''
          $name = "ci-mysql"

          $exists = docker ps -a --format "{{.Names}}" | Where-Object { $_ -eq $name }
          if (-not $exists) {
            Write-Host "No existe $name, creando..."
            docker run -d --name $name -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=appdb -p 3306:3306 mysql:8 | Out-Null
          }
          else {
            $running = docker inspect -f "{{.State.Running}}" $name 2>$null
            if ($running -eq "true") {
              Write-Host "$name ya está corriendo. Continuamos."
            } else {
              Write-Host "$name existe pero está detenido. Iniciando..."
              docker start $name | Out-Null
            }
          }
        '''
      }
    }

    stage('Build all modules') {
      steps {
        bat 'cd savia-negocio && mvn -B -U -DskipTests clean install  -Dmaven.repo.local=%WORKSPACE%\\.m2 && cd ..'
        bat 'cd savia-ejb     && mvn -B -U -DskipTests clean install  -Dmaven.repo.local=%WORKSPACE%\\.m2 && cd ..'
        bat 'cd savia-ear     && mvn -B -U -DskipTests clean package  -Dmaven.repo.local=%WORKSPACE%\\.m2 && cd ..'
        bat 'cd savia-web     && mvn -B -U -DskipTests clean install  -Dmaven.repo.local=%WORKSPACE%\\.m2 && cd ..'
      }
    }

    stage('Create ZIP (compiled targets)') {
      steps {
        script {
          def fmt = new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss")
          env.ZIP_NAME = "compiled_${fmt.format(new Date())}_build-${env.BUILD_NUMBER}.zip"
        }
        powershell """
          Remove-Item -Recurse -Force dist -ErrorAction SilentlyContinue
          New-Item -ItemType Directory -Force `
            dist\\savia-ejb\\target, `
            dist\\savia-negocio\\target, `
            dist\\savia-web\\target, `
            dist\\savia-ear\\target | Out-Null

          Copy-Item -Recurse -Force 'savia-ejb\\target\\*'     'dist\\savia-ejb\\target'     -ErrorAction SilentlyContinue
          Copy-Item -Recurse -Force 'savia-negocio\\target\\*' 'dist\\savia-negocio\\target' -ErrorAction SilentlyContinue
          Copy-Item -Recurse -Force 'savia-web\\target\\*'     'dist\\savia-web\\target'     -ErrorAction SilentlyContinue
          Copy-Item -Recurse -Force 'savia-ear\\target\\*'     'dist\\savia-ear\\target'     -ErrorAction SilentlyContinue

          Compress-Archive -Path 'dist\\*' -DestinationPath '${env.ZIP_NAME}' -Force
        """
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
            set "DEPLOY_FILE="

            IF EXIST "savia-ear\\target" (
              FOR /F "delims=" %%F IN ('dir /B /S "savia-ear\\target\\*.ear"') DO set "DEPLOY_FILE=%%F"
            )
            IF NOT DEFINED DEPLOY_FILE (
              IF EXIST "savia-web\\target" (
                FOR /F "delims=" %%F IN ('dir /B /S "savia-web\\target\\*.war"') DO set "DEPLOY_FILE=%%F"
              )
            )

            echo Deploying %DEPLOY_FILE%
            "%JBOSS_CLI%" --controller=%WF_HOST%:%WF_PORT% --user=%WF_USER% --password=%WF_PASS% --connect --command="deploy \"%DEPLOY_FILE%\" --force"
          '''
        }
      }
    }
  }

  post {
    success { echo "? Pipeline OK. ZIP: ${env.ZIP_NAME}" }
    failure { echo "? Falló la compilación" }
    always {
      script {
        if (params.USE_DOCKER_DB) {
          // No fallar si el contenedor no existe o no está corriendo
          powershell(returnStatus: true, script: '''
            $name = "ci-mysql"
            $running = docker ps --format "{{.Names}}" | Where-Object { $_ -eq $name }
            if ($running) { docker stop $name | Out-Null }
          ''')
        }
      }
    }
  }
}
