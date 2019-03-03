properties([
        parameters(
                [
                        stringParam(
                                name: 'GIT_REPO',
                                defaultValue: ''
                        ),
                        choiceParam(
                                name: 'ENV',
                                choices: ['test', 'staging', 'production']
                        )
                ]
        )
])

pipeline {

    agent {
        kubernetes {
            label 'deploy-service-pod'
            defaultContainer 'jnlp'
            yaml """
apiVersion: v1
kind: Pod
metadata:
  labels:
    job: uninstall-service
spec:
  containers:
  - name: helm-cli
    image: ibmcom/k8s-helm:v2.6.0
    command: ["cat"]
    tty: true
"""
        }
    }

    stages {

        stage('Uninstall') {
            steps {
                container('helm-cli') {
                    sh "/helm delete --purge ${params.ENV}-${params.GIT_REPO.toLowerCase()}"
                }
            }
        }
    }
}
