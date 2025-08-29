pipeline {
  agent { label 'Linux' }

  tools {
    maven 'Maven'   // Ajusta al nombre que tengas en Jenkins (Manage Jenkins > Global Tool Config)
    jdk   'jdk11'   // Cambia a jdk17 si ya migraste
  }

  options { timestamps() }

  environment {
    // WildFly local al workspace (ajústalo si lo tienes en otro lado)
    WILDFLY_HOME = "${WORKSPACE}/wildfly-19.1.0.Final"
    DEPLOYMENTS  = "${WILDFLY_HOME}/standalone/deployments"
    EAR_NAME     = 'savia-ear.ear'
    MAVEN_FLAGS  = '-B -U -DskipTests'  // batch + force update
    AGG_POM      = ''                   // lo llenamos en tiempo de ejecución
  }

  stages {

    stage('Checkout SCM') {
      steps { checkout scm }
    }

    stage('Detectar POM agregador') {
      steps {
        script {
          // 1) Intento A: buscar un pom.xml con packaging "pom" y con <modules>
          def detectCmd = '''
            set -e
            CANDIDATES=$(grep -rl --include=pom.xml "<packaging>pom</packaging>" . || true)
            FOUND=""
            for P in $CANDIDATES; do
              if grep -q "<modules>" "$P"; then
                FOUND="$P"
                break
              fi
            done
            if [ -n "$FOUND" ]; then
              echo "$FOUND"
            fi
          '''
          def found = sh(script: detectCmd, returnStdout: true).trim()

          // 2) Intento B: deducir parent del EJB por <relativePath>
          if (!found) {
            def deduceCmd = '''
              set -e
              EJB_POM=$(ls -1d */savia-ejb/pom.xml 2>/dev/null | head -n1 || true)
              if [ -n "$EJB_POM" ]; then
                REL=$(sed -n "s#.*<relativePath>\\(.*\\)</relativePath>.*#\\1#p" "$EJB_POM" | head -n1)
                [ -z "$REL" ] && REL="../pom.xml"
                DIR=$(dirname "$EJB_POM")
                AGG="$DIR/$REL"
                if [ -f "$AGG" ]; then
                  echo "$AGG"
                fi
              fi
            '''
            found = sh(script: deduceCmd, returnStdout: true).trim()
          }

          // 3) Intento C: raíz del repo
          if (!found && fileExists("${WORKSPACE}/pom.xml")) {
            found = "${WORKSPACE}/pom.xml"
          }

          env.AGG_POM = found
          echo "AGG_POM detectado: ${env.AGG_POM ?: '(no encontrado)'}"
        }
      }
    }

    stage('Build') {
      steps {
        script {
          sh 'rm -rf ~/.m2/repository/com/saviasaludeps/savia || true' // limpia cache del parent por si quedó corrupto

          if (env.AGG_POM) {
            echo "Compilando desde agregador -> ${env.AGG_POM}"
            sh "mvn -f \"${env.AGG_POM}\" clean install ${MAVEN_FLAGS}"
          } else {
            echo "No se encontró agregador, compilo por módulos en orden manual (negocio ? ejb ? ear ? web)"
            // Ajusta rutas si tus carpetas tienen otro nombre/ubicación
            sh "mvn -f \"${WORKSPACE}/savia-negocio/pom.xml\" clean install ${MAVEN_FLAGS}"
            sh "mvn -f \"${WORKSPACE}/savia-ejb/pom.xml\"     clean install ${MAVEN_FLAGS}"
            sh "mvn -f \"${WORKSPACE}/savia-ear/pom.xml\"     clean package ${MAVEN_FLAGS}"
            sh "mvn -f \"${WORKSPACE}/savia-web/pom.xml\"     clean install ${MAVEN_FLAGS}"
          }
        }
      }
    }

    stage('Empaquetar EAR') {
      steps {
        script {
          def ear = sh(script: "ls ${WORKSPACE}/savia-ear/target/*.ear | head -n 1", returnStdout: true).trim()
          if (!ear) { error "No se encontró .ear en savia-ear/target" }
          sh "cp -f '${ear}' '${WORKSPACE}/savia-ear/target/${EAR_NAME}'"
          echo "? EAR listo: ${EAR_NAME}"
        }
      }
    }

    stage('Stop WildFly') {
      steps {
        sh """
          set +e
          ${WILDFLY_HOME}/bin/jboss-cli.sh --connect command=:shutdown
          sleep 5
          set -e
          echo '? WildFly detenido (o ya estaba detenido).'
        """
      }
    }

    stage('Deploy EAR a deployments') {
      steps {
        sh """
          mkdir -p '${DEPLOYMENTS}'
          rm -rf '${DEPLOYMENTS}'/*
          cp '${WORKSPACE}/savia-ear/target/${EAR_NAME}' '${DEPLOYMENTS}/'
          echo '? EAR copiado en ${DEPLOYMENTS}.'
        """
      }
    }

    stage('Start WildFly') {
      steps {
        sh "chmod +x ${WILDFLY_HOME}/bin/*.sh || true"
        sh "nohup ${WILDFLY_HOME}/bin/standalone.sh -b 0.0.0.0 > /dev/null 2>&1 &"
        echo '? WildFly iniciado.'
      }
    }

    stage('Verify Deployment') {
      steps {
        script {
          def logPath = "${WILDFLY_HOME}/standalone/log/server.log"
          timeout(time: 150, unit: 'SECONDS') {
            waitUntil {
              def exists = sh(script: "[ -f '${logPath}' ] && echo yes || echo no", returnStdout: true).trim()
              if (exists == 'yes') {
                def tail = sh(script: "tail -n 500 '${logPath}'", returnStdout: true)
                if (tail.contains("Deployed \"${EAR_NAME}\"")) {
                  echo '? Despliegue OK.'
                  return true
                }
                if (tail.contains('.failed')) {
                  error "? El despliegue de ${EAR_NAME} falló. Revisa ${logPath}."
                }
              }
              sleep 5
              return false
            }
          }
        }
      }
    }
  }
}
