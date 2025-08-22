pipeline {
  agent { label 'Windows' }

  tools {
    jdk   'jdk11'
    maven 'Maven'
  }

  parameters {
    booleanParam(name: 'USE_DOCKER_DB',    defaultValue: true,  description: 'Levantar MySQL en Docker para pruebas')
    booleanParam(name: 'DEPLOY_TO_WILDFLY', defaultValue: false, description: 'Desplegar EAR al finalizar')
  }

  environment {
    MAVEN_OPTS   = '-Dmaven.wagon.http.pool=false -Djava.awt.headless=true'
    WF_HOST      = 'localhost'
    WF_PORT      = '9990'
    WILDFLY_HOME = 'C:\\wildfly-19.1.0.Final'
  }

  options {
    timestamps()
    skipDefaultCheckout()
  }

  // Usa SOLO uno (o ninguno) de estos triggers:
  // triggers { githubPush() }
  // triggers { pollSCM('H/2 * * * *') }

  stages {

    stage('Checkout') {
      steps { checkout scm }
    }

    stage('Database (MySQL)') {
      when { expression { return params.USE_DOCKER_DB } }
      steps {
        // Arranca si existe; crea si no existe; si ya corre, solo avisa
        powershell '''
          $name = "ci-mysql"
          $exists = (docker ps -a --format "{{.Names}}" | findstr /I "^$name$") -ne $null
          if ($exists) {
            $running = (docker ps --format "{{.Names}}" | findstr /I "^$name$") -ne $null
            if ($running) {
              Write-Host "$name ya está corriendo. Continuamos."
            } else {
              docker start $name | Out-Null
              Write-Host "Iniciado $name."
            }
          } else {
            docker run -d --name $name -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=appdb -p 3306:3306 mysql:8 | Out-Null
            Write-Host "Creado y levantado $name."
          }
        '''
      }
    }

    stage('Build all modules') {
      steps {
        script {
          def repo = '%WORKSPACE%\\.m2'
          def failures = []

          def run = { name, cmd ->
            echo "Compilando ${name}..."
            def code = bat(returnStatus: true, script: "cd ${name} && ${cmd} -Dmaven.repo.local=${repo} && cd ..")
            if (code != 0) {
              echo "??  Falló ${name} (exit ${code}). Continuamos."
              failures << name
            }
          }

          // Orden típico de dependencias
          run('savia-negocio', 'mvn -B -U -DskipTests clean install')
          run('savia-ejb',     'mvn -B -U -DskipTests clean install')
          run('savia-web',     'mvn -B -U -DskipTests clean install')
          run('savia-ear',     'mvn -B -U -DskipTests clean package')

          if (failures) {
            currentBuild.result = 'UNSTABLE'
            env.MODULE_FAILURES = failures.join(',')
            echo "Módulos con error: ${env.MODULE_FAILURES}"
          } else {
            echo "? Todos los módulos compilaron OK."
          }
        }
      }
    }

    stage('Create ZIP (compiled targets)') {
      steps {
        script {
          def ts = new Date().format('yyyyMMdd_HHmmss')
          env.ZIP_NAME = "compiled_${ts}_build-${env.BUILD_NUMBER}.zip"
          currentBuild.description = env.ZIP_NAME
        }
        powershell '''
          $ErrorActionPreference = "Stop"
          $zip = "$Env:WORKSPACE\\$Env:ZIP_NAME"

          if (Test-Path $zip) { Remove-Item -Force $zip }
          Remove-Item -Recurse -Force dist -ErrorAction SilentlyContinue

          $pairs = @(
            @{src='savia-negocio\\target'; dst='dist\\savia-negocio\\target'},
            @{src='savia-ejb\\target';     dst='dist\\savia-ejb\\target'},
            @{src='savia-web\\target';     dst='dist\\savia-web\\target'},
            @{src='savia-ear\\target';     dst='dist\\savia-ear\\target'}
          )

          foreach($p in $pairs){
            if (Test-Path $p.src) {
              New-Item -ItemType Directory -Force $p.dst | Out-Null
              Copy-Item -Recurse -Force ($p.src + "\\*") $p.dst -ErrorAction SilentlyContinue
            }
          }

          if (-not (Test-Path 'dist')) { New-Item -ItemType Directory -Force dist | Out-Null }
          Compress-Archive -Path 'dist\\*' -DestinationPath $zip -Force
          Write-Host "ZIP creado: $zip"
        '''
      }
    }

    stage('Archive ZIP') {
      steps {
        archiveArtifacts artifacts: "${env.ZIP_NAME}", fingerprint: true
        script {
          echo "Descárgalo desde los artefactos del build: ${env.ZIP_NAME}"
          if (env.MODULE_FAILURES) {
            echo "Se generó el ZIP con lo compilado, pero fallaron: ${env.MODULE_FAILURES}"
          }
        }
      }
    }

    stage('Deploy to WildFly') {
      when { expression { return params.DEPLOY_TO_WILDFLY } }
      steps {
        withCredentials([usernamePassword(credentialsId: 'wildfly-admin', usernameVariable: 'WF_USER', passwordVariable: 'WF_PASS')]) {
          powershell '''
            $cli = "$Env:WILDFLY_HOME\\bin\\jboss-cli.bat"
            $ear = Get-ChildItem -Path 'savia-ear\\target' -Filter *.ear -Recurse | Select-Object -First 1
            if ($ear) {
              Write-Host "Desplegando $($ear.FullName)"
              & $cli --controller=$Env:WF_HOST:$Env:WF_PORT --user=$Env:WF_USER --password=$Env:WF_PASS --connect --command="deploy `"$($ear.FullName)`" --force"
            } else {
              Write-Host "No hay EAR para desplegar; se omite."
            }
          '''
        }
      }
    }
  }

  post {
    always {
      script {
        if (params.USE_DOCKER_DB) {
          // Parar el contenedor sin fallar si no existe/está parado
          powershell(returnStatus: true, script: 'docker stop ci-mysql *> $null; exit 0')
        }
      }
    }
    success { echo '? Pipeline OK' }
    unstable { echo '?? Pipeline UNSTABLE (se creó el ZIP igualmente)' }
    failure { echo '? Pipeline FAILED' }
  }
}
