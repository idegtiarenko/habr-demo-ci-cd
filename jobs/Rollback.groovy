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
                        ),
                        stringParam(
                                name: 'VERSIONS',
                                defaultValue: '1'
                        ),
                ]
        )
])

def targetHelmRevision

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
    job: rollback-service
spec:
  containers:
  - name: git
    image: alpine/git
    command: ["cat"]
    tty: true
  - name: helm-cli
    image: ibmcom/k8s-helm:v2.6.0
    command: ["cat"]
    tty: true
"""
        }
    }

    stages {

        stage('Find current version') {
            steps {
                container('helm-cli') {
                    script {
                        def currentHelmRevision = sh(script: "/helm history ${params.ENV}-${params.GIT_REPO.toLowerCase()} | grep DEPLOYED | awk '{ print \$1 }'", returnStdout: true).trim()
                        targetHelmRevision = sh (script: "echo $currentHelmRevision | awk '{print \$1-${params.VERSIONS}}'", returnStdout: true)
                        sh "echo 'Rolling back ${params.ENV}-${params.GIT_REPO.toLowerCase()} helm revision ${currentHelmRevision}->${targetHelmRevision}'"
                    }
                }
            }
        }
        stage('Execute rollback') {
            steps {
                container('helm-cli') {
                    script {
                        sh "/helm rollback ${params.ENV}-${params.GIT_REPO.toLowerCase()} ${targetHelmRevision}"
                    }
                }
            }
        }
    }
}