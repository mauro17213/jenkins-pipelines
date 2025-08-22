pipeline {
  agent { label 'Windows' }         // Cambia por el label real de tu nodo

  tools {
    jdk   'jdk11'                   // Nombre EXACTO en Global Tool Configuration
    maven 'Maven'                   // Nombre EXACTO en Global Tool Configuration
  }

  options {
    timestamps()
    skipDefaultCheckout()
  }

  // ?? Disparo automático ??
  triggers { pollSCM('H/2 * * * *') }   // quita esto si usarás webhook
  // triggers { githubPush() }          // usa esto si tienes webhook configurado

  parameters {
    booleanParam(name: 'DEPLOY_TO_WILDFLY', defaultValue: false, description: 'Desplegar al finalizar')
  }

  environment {
    MAVEN_OPTS   = '-Dmaven.wagon.http.pool=false -Djava.awt.headless=true'
    WILDFLY_HOME = 'C:\\wildfly-19.1.0.Final'  // Carpeta de instalación, NO el XML
    WF_HOST      = 'localhost'
    WF_PORT      = '9990'
  }

  stages {
    stage('Checkout') {
      steps { checkout scm }
    }

    stage('Build all modules') {
      steps {
        bat '''
          bat 'cd savia-negocio && mvn -B -U -DskipTests clean install -Dmaven.repo.local=%WORKSPACE%\\.m2 && cd ..'
          bat 'cd savia-ejb     && mvn -B -U -DskipTests clean install -Dmaven.repo.local=%WORKSPACE%\\.m2 && cd ..'
          bat 'cd savia-ear     && mvn -B -U -DskipTests clean package -Dmaven.repo.local=%WORKSPACE%\\.m2 && cd ..'
          bat 'cd savia-web     && mvn -B -U -DskipTests clean install -Dmaven.repo.local=%WORKSPACE%\\.m2 && cd ..'
        '''
      }
    }

    stage('Create ZIP (compiled targets)') {
      steps {
        script {
          // genera timestamp: 2025-08-22_18-35-07
          env.BUILD_TS  = new Date().format("yyyy-MM-dd_HH-mm-ss")
          env.ZIP_NAME  = "compiled_${env.BUILD_TS}_build-${env.BUILD_NUMBER}.zip"
        }
        // Crea un único ZIP con TODO lo compilado (target/ de cada módulo)
        bat """
          if exist %ZIP_NAME% del /F /Q %ZIP_NAME%
          powershell -NoProfile -Command ^
            "Remove-Item -Recurse -Force dist -ErrorAction SilentlyContinue; ^
             New-Item -ItemType Directory -Force dist\\savia-ejb\\target,dist\\savia-negocio\\target,dist\\savia-web\\target,dist\\savia-ear\\target | Out-Null; ^
             Copy-Item -Recurse -Force 'savia-ejb\\target\\*'     'dist\\savia-ejb\\target'     -ErrorAction SilentlyContinue; ^
             Copy-Item -Recurse -Force 'savia-negocio\\target\\*' 'dist\\savia-negocio\\target' -ErrorAction SilentlyContinue; ^
             Copy-Item -Recurse -Force 'savia-web\\target\\*'     'dist\\savia-web\\target'     -ErrorAction SilentlyContinue; ^
             Copy-Item -Recurse -Force 'savia-ear\\target\\*'     'dist\\savia-ear\\target'     -ErrorAction SilentlyContinue; ^
             Compress-Archive -Path 'dist\\*' -DestinationPath '$env:ZIP_NAME' -Force"
        """
      }
    }

    stage('Archive ZIP') {
      steps {
        // quedará disponible para descarga desde la página del build
        archiveArtifacts artifacts: "${env.ZIP_NAME}", fingerprint: true
      }
    }

    stage('Deploy to WildFly') {
      when { expression { return params.DEPLOY_TO_WILDFLY } }
      steps {
        // Arranca WildFly si no está escuchando en el puerto de management
        bat """
          powershell -NoProfile -Command ^
            "if(-not (Test-NetConnection -ComputerName ${env.WF_HOST} -Port ${env.WF_PORT}).TcpTestSucceeded){ ^
               Start-Process -FilePath '${env.WILDFLY_HOME}\\bin\\standalone.bat' -ArgumentList '-c','standalone-full.xml','-b','0.0.0.0','-bmanagement','0.0.0.0' -WindowStyle Hidden; ^
               for($i=0;$i -lt 60;$i++){ if((Test-NetConnection -ComputerName ${env.WF_HOST} -Port ${env.WF_PORT}).TcpTestSucceeded){break}; Start-Sleep -Seconds 2 } ^
             }"
        """

        // despliegue del EAR (o WAR si no hay EAR) con jboss-cli
        withCredentials([usernamePassword(credentialsId: 'wildfly-admin', usernameVariable: 'WF_USER', passwordVariable: 'WF_PASS')]) {
          bat """
            set "JBOSS_CLI=%WILDFLY_HOME%\\bin\\jboss-cli.bat"
            set "DEPLOY_FILE="
            for /f "delims=" %%F in ('dir /B /S "savia-ear\\target\\*.ear"') do set "DEPLOY_FILE=%%F"
            if not defined DEPLOY_FILE for /f "delims=" %%F in ('dir /B /S "savia-web\\target\\*.war"') do set "DEPLOY_FILE=%%F"
            echo Deploying %DEPLOY_FILE%
            "%JBOSS_CLI%" --controller=%WF_HOST%:%WF_PORT% --user=%WF_USER% --password=%WF_PASS% --connect --commands="deploy \"%DEPLOY_FILE%\" --force"
          """
        }
      }
    }
  }

  post {
    success { echo "? Compilación OK — ZIP generado: ${env.ZIP_NAME}" }
    failure { echo '? Falló la compilación' }
  }
}
