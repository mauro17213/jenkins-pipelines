pipeline {
  agent { label 'Windows' } 

  tools {
    jdk 'jdk11'        
    Maven_3.9.3 'Maven_3.9.3'   

  parameters {
    booleanParam(name: 'USE_DOCKER_DB', defaultValue: true, description: 'Levantar MySQL en Docker para pruebas')
    booleanParam(name: 'DEPLOY_TO_WILDFLY', defaultValue: false, description: 'Desplegar al finalizar')
  }

  environment {
    MAVEN_OPTS = '-Dmaven.wagon.http.pool=false -Djava.awt.headless=true'
    // Ajusta estas variables si vas a desplegar
    WF_HOST = 'localhost'
    WF_PORT = '9990'
    WILDFLY_HOME = 'C:\wildfly-19.1.0.Final\standalone\configuration\standalone-full.xml' 
    MYSQL_SERVICE = 'MySQL80'
  }

  options {
    ansiColor('xterm')
    timestamps()
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
      }
    }

    stage('Database (MySQL)') {
      when { anyOf { expression { return params.USE_DOCKER_DB }; expression { return isUnix() == false } } }
      steps {
        script {
          if (params.USE_DOCKER_DB) {
            // Intenta levantar MySQL 8 con Docker (Windows)
            bat '''
              docker ps -a --format "{{.Names}}" | findstr /I "^ci-mysql$" >nul && (
                docker start ci-mysql
              ) || (
                docker run -d --name ci-mysql -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=appdb -p 3306:3306 mysql:8
              )
            '''
          } else {
            // O intenta iniciar el servicio local de MySQL en Windows
            bat '''
              sc query "%MYSQL_SERVICE%" | findstr /I "RUNNING" >nul || net start "%MYSQL_SERVICE%"
            '''
          }
        }
      }
    }

    stage('Build savia-ejb') {
      steps {
        dir('savia-ejb') {
          bat 'mvn -B -U clean install -DskipTests'
        }
      }
    }

    stage('Build savia-negocio') {
      steps {
        dir('savia-negocio') {
          bat 'mvn -B -U clean install -DskipTests'
        }
      }
    }

    stage('Build savia-web') {
      steps {
        dir('savia-web') {
          bat 'mvn -B -U clean install -DskipTests'
        }
      }
    }

    stage('Package savia-ear') {
      steps {
        dir('savia-ear') {
          bat 'mvn -B -U clean package -DskipTests'
        }
      }
    }

    stage('Archive Artifacts') {
      steps {
        // Ajusta patrones si tus nombres cambian
        archiveArtifacts artifacts: 'savia-ear/target/*.ear, savia-web/target/*.war', fingerprint: true
        junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
      }
    }

    stage('Deploy to WildFly') {
      when { expression { return params.DEPLOY_TO_WILDFLY } }
      steps {
        withCredentials([usernamePassword(credentialsId: 'wildfly-admin', usernameVariable: 'WF_USER', passwordVariable: 'WF_PASS')]) {
          // Elige qué desplegar: EAR (habitual) o WAR
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
    success {
      echo '? Pipeline OK'
    }
    failure {
      echo '? Pipeline FAILED'
    }
    always {
      // Si levantaste MySQL con Docker, puedes pararlo para liberar recursos (opcional)
      script {
        if (params.USE_DOCKER_DB) {
          bat 'docker ps --format "{{.Names}}" | findstr /I "^ci-mysql$" >nul && docker stop ci-mysql'
        }
      }
    }
  }
}
