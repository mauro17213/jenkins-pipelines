pipeline {
  agent { label 'Linux' }

  tools {
    maven 'Maven'   // nombre configurado en Manage Jenkins
    jdk   'jdk11'   // o 'jdk17'
  }

  options { timestamps() }

  environment {
    MAVEN_FLAGS = '-B -U -DskipTests'
    ZIP_OUT_DIR = "${WORKSPACE}/dist"
    ZIP_NAME    = "savia-modulos.zip"
  }

  stages {
    stage('Checkout SCM') {
      steps { checkout scm }
    }

    stage('Build (ejb ? negocio ? web ? ear)') {
      steps {
        // Forzamos bash para tener pipefail y mejor manejo de errores
        sh '''
/usr/bin/env bash <<'BASH'
set -euo pipefail

echo "? Limpieza selectiva del repo local (opcional)"
rm -rf "${HOME}/.m2/repository/com/saviasaludeps/savia" || true

echo "? Compilando ejb..."
mvn -f "${WORKSPACE}/savia-ejb/pom.xml" clean install ${MAVEN_FLAGS}

echo "? Compilando negocio..."
mvn -f "${WORKSPACE}/savia-negocio/pom.xml" clean install ${MAVEN_FLAGS}

echo "? Empaquetando web (WAR)..."
mvn -f "${WORKSPACE}/savia-web/pom.xml" clean package ${MAVEN_FLAGS}

echo "? Empaquetando ear..."
mvn -f "${WORKSPACE}/savia-ear/pom.xml" clean package ${MAVEN_FLAGS}

echo "? Resolviendo artefactos generados..."
EJB_JAR=$(ls -1 "${WORKSPACE}/savia-ejb/target/"*.jar 2>/dev/null | grep -vE 'sources|javadoc' | head -n1 || true)
NEG_JAR=$(ls -1 "${WORKSPACE}/savia-negocio/target/"*.jar 2>/dev/null | grep -vE 'sources|javadoc' | head -n1 || true)
WAR_PATH=$(ls -1 "${WORKSPACE}/savia-web/target/"*.war 2>/dev/null | head -n1 || true)
EAR_PATH=$(ls -1 "${WORKSPACE}/savia-ear/target/"*.ear 2>/dev/null | head -n1 || true)

[ -n "$EAR_PATH" ] || { echo "? No se encontró .ear en savia-ear/target"; exit 1; }
[ -n "$WAR_PATH" ] || echo "??  No se encontró .war en savia-web/target (continuo sin WAR)"

echo "? Preparando carpeta de distribución..."
rm -rf "${ZIP_OUT_DIR}"; mkdir -p "${ZIP_OUT_DIR}"

cp -f "$EAR_PATH" "${ZIP_OUT_DIR}/"
[ -n "$WAR_PATH" ] && cp -f "$WAR_PATH" "${ZIP_OUT_DIR}/"
[ -n "$EJB_JAR" ] && cp -f "$EJB_JAR" "${ZIP_OUT_DIR}/"
[ -n "$NEG_JAR" ] && cp -f "$NEG_JAR" "${ZIP_OUT_DIR}/"

echo "??  Empaquetando ZIP..."
cd "${ZIP_OUT_DIR}"
if command -v zip >/dev/null 2>&1; then
  zip -r "${ZIP_NAME}" .
else
  # Fallback sin 'zip': usa jar (mismo formato zip)
  jar -cfM "${ZIP_NAME}" .
fi

echo "? Listo: ${ZIP_OUT_DIR}/${ZIP_NAME}"
BASH
        '''
      }
    }

    stage('Publicar artefactos') {
      steps {
        archiveArtifacts artifacts: 'dist/*.zip, dist/*.ear, dist/*.war, dist/*.jar', allowEmptyArchive: false
      }
    }
  }

  post {
    always {
      sh 'ls -lh dist || true'
    }
  }
}
