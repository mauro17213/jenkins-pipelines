pipeline {
  agent { label 'Windows' }   // usa el label real

  tools {
    jdk   'jdk11'             // nombres EXACTOS en Global Tool Configuration
    maven 'Maven'
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
    MYSQL_SERVICE = 'MySQL80'
  }

  options {
    timestamps()
    skipDefaultCheckout()
  }

  stages {
    stage('Checkout') {
      steps { checkout scm }
    }

    stage('Database (MySQL)') {
      when { expression { return params.USE_DOCKER_DB } }
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

    stage('Build savia-ejb')       { steps { dir('savia-ejb')     { bat 'mvn -B -U clean install -DskipTests' } } }
    stage('Build savia-negocio')   { steps { dir('savia-negocio') { bat 'mvn -B -U clean install -DskipTests' } } }
    stage('Build savia-web')       { steps { dir('savia-web')     { bat 'mvn -B -U clean install -DskipTests' } } }
    stage('Package savia-ear')     { steps { dir('savia-ear')     { bat 'mvn -B -U clean package -DskipTests' } } }

    stage('Run Tests (all)') {
      steps { bat 'mvn -q -DskipTests=false test' }
    }

    // ?? NUEVA ETAPA: crea un único ZIP con lo compilado de los 4 módulos
    stage('Zip compiled modules') {
      steps {
        // Limpia zip anterior, arma dist/ con target de cada módulo y comprime
        bat '''
          if exist build_compiled_all.zip del /F /Q build_compiled_all.zip
          powershell -NoProfile -Command ^
            "Remove-Item -Recurse -Force dist -ErrorAction SilentlyContinue; ^
             New-Item -ItemType Directory -Force dist\\savia-ejb,dist\\savia-negocio,dist\\savia-web,dist\\savia-ear | Out-Null; ^
             Copy-Item -Recurse -Force 'savia-ejb\\target\\*'     'dist\\savia-ejb\\target'     -ErrorAction SilentlyContinue; ^
             Copy-Item -Recurse -Force 'savia-negocio\\target\\*' 'dist\\savia-negocio\\target' -ErrorAction SilentlyContinue; ^
             Copy-Item -Recurse -Force 'savia-web\\target\\*'     'dist\\savia-web\\target'     -ErrorAction SilentlyContinue; ^
             Copy-Item -Recurse -Force 'savia-ear\\target\\*'     'dist\\savia-ear\\target'     -ErrorAction SilentlyContinue; ^
             Compress-Archive -Path 'dist\\*' -DestinationPath 'build_compiled_all.zip' -Force"
        '''
      }
    }

    stage('Archive Artifacts') {
      steps {
        // Publica el ZIP único + los artefactos finales como antes
        archiveArtifacts artifacts: 'build_compiled_all.zip, savia-ear/target/*.ear, savia-web/target/*.war', fingerprint: true
        junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
      }
    }

    stage('Deploy to WildFly') {
      when { expression { return params.DEPLOY_TO_WILDFLY } }
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
            "%JBOSS_CLI%" --controller=%WF_HOST%:%WF_PORT% --user=%WF_USER% --password=%WF_PASS% --connect --command="deploy \"%DEPLOY_FILE%\" --force"
          '''
        }
      }
    }
  }

  post {
    success { echo '? Pipeline OK' }
    failure { echo '? Pipeline FAILED' }
    always {
      script {
        if (params.USE_DOCKER_DB && env.WORKSPACE) {
          bat 'docker ps --format "{{.Names}}" | findstr /I "^ci-mysql$" >nul && docker stop ci-mysql'
        }
      }
    }
  }
}
