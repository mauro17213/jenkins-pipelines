pipeline {
    agent none

    stages {
        stage('Build en Linux') {
            agent { label 'Linux' }
            steps {
                echo "Compilando en Linux..."
                sh 'mvn clean package -DskipTests'

                // Guarda el WAR como artefacto en Jenkins
                archiveArtifacts artifacts: 'target/*.war', fingerprint: true
            }
        }

        stage('Deploy en Windows') {
            agent { label 'Windows' }
            steps {
                echo "Desplegando en Windows..."

                // Descarga el artefacto desde el stage anterior
                copyArtifacts(
                    projectName: env.JOB_NAME,
                    selector: lastSuccessful(),
                    filter: 'target/*.war'
                )

                // Copia el WAR al directorio de WildFly en Windows
                bat '''
                    copy target\\*.war C:\\wildfly\\standalone\\deployments\\
                '''
            }
        }
    }
}
