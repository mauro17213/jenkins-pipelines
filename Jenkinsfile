pipeline {
  agent { label 'Linux' }

  tools {
    maven 'Maven'   // Ajusta al nombre real en Manage Jenkins > Global Tool Configuration
    jdk   'jdk11'   // Cambia a 'jdk17' si ya migraste
  }

  options { timestamps() }

  environment {
    WILDFLY_HOME = "${WORKSPACE}/wildfly-19.1.0.Final"
    DEPLOYMENTS  = "${WILDFLY_HOME}/standalone/deployments"

    EAR_NAME     = 'savia-ear.ear'
    WEB_NAME     = 'savia-web.war'

    MAVEN_FLAGS  = '-B -U -DskipTests'
    BIND_ADDR    = '0.0.0.0'
  }

  stages {

    stage('Checkout SCM') { steps { checkout scm } }

    // Compilación sin POM padre: orden explícito
    stage('Build savia-negocio') {
      steps {
        sh """
          echo '? Compilando savia-negocio...'
          mvn -f "${WORKSPACE}/savia-negocio/pom.xml" clean install ${MAVEN_FLAGS}
        """
      }
    }

    stage('Build savia-ejb') {
      steps {
        sh """
          echo '? Compilando savia-ejb...'
          mvn -f "${WORKSPACE}/savia-ejb/pom.xml" clean install ${MAVEN_FLAGS}
        """
      }
    }

    stage('Package savia-ear') {
      steps {
        sh """
          echo '? Empaquetando savia-ear...'
          mvn -f "${WORKSPACE}/savia-ear/pom.xml" clean package ${MAVEN_FLAGS}
        """
      }
    }

    stage('Package savia-web') {
      steps {
        sh """
          echo '? Empaquetando savia-web...'
          mvn -f "${WORKSPACE}/savia-web/pom.xml" clean package ${MAVEN_FLAGS}
        """
      }
    }

    stage('Resolver artefactos (.ear y .war)') {
      steps {
        script {
          def earPath = sh(script: "ls -1 \"${WORKSPACE}/savia-ear/target\"/*.ear | head -n1", returnStdout: true).trim()
          if (!earPath) { error "No se encontró un .ear en savia-ear/target" }
          def warPath = sh(script: "ls -1 \"${WORKSPACE}/savia-web/target\"/*.war | head -n1", returnStdout: true).trim()
          if (!warPath) { error "No se encontró un .war en savia-web/target" }

          echo "EAR detectado: ${earPath}"
          echo "WAR detectado: ${warPath}"

          sh """
            mkdir -p "${WORKSPACE}/dist"
            cp -f "${earPath}" "${WORKSPACE}/dist/${EAR_NAME}"
