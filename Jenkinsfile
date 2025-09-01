pipeline {
  agent { label 'Linux' }

  tools {
    maven 'Maven'
    jdk   'jdk11'
  }

  options { timestamps() }

  environment {
    EAR_NAME    = 'savia-ear.ear'
    MAVEN_FLAGS = '-B -U -DskipTests'
    WILDFLY_CONTAINER = 'wildfly'   // nombre de tu contenedor WildFly
    DEPLOY_PATH = '/opt/jboss/wildfly/standalone/deployments'
  }

  stages {
    stage('Checkout SCM') {
      steps { checkout scm }
    }

    stage('Build (negocio, ejb, web, ear)') {
      steps {
        sh '''
          set -e
          echo "?? Compilando módulos..."
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

    stage('Deploy to WildFly Container') {
      steps {
        sh '''
          set -e
          echo "? Copiando EAR al contenedor WildFly..."
          docker exec ${WILDFLY_CONTAINER} rm -f ${DEPLOY_PATH}/${EAR_NAME}* || true
          docker cp "$WORKSPACE/savia-ear/target/$EAR_NAME" ${WILDFLY_CONTAINER}:${DEPLOY_PATH}/
          docker exec ${WILDFLY_CONTAINER} touch ${DEPLOY_PATH}/${EAR_NAME}.dodeploy
          echo "? EAR copiado y marcado para despliegue"
        '''
      }
    }

    stage('Verify Deployment') {
      steps {
        script {
          echo "? Esperando que WildFly procese el EAR..."
          sleep 15
        }
        sh '''
          echo "? Verificando aplicación en http://localhost:8080/savia ..."
          curl -s -o /dev/null -w "HTTP %{http_code}\\n" http://localhost:8080/savia || true
        '''
      }
    }
  }

  post {
    success {
      echo "? Despliegue exitoso en WildFly: http://localhost:8080/savia"
    }
    failure {
      echo "? Falló el pipeline, revisar logs del contenedor WildFly"
      sh 'docker logs --tail=100 ${WILDFLY_CONTAINER} || true'
    }
  }
}
