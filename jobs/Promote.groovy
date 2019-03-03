properties([
        parameters(
                [
                        stringParam(
                                name: 'GIT_REPO',
                                defaultValue: ''
                        )
                ]
        )
])

def version
def revision

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
    job: promote-service
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

        stage('Find version on staging') {
            steps {
                container('helm-cli') {
                    script {
                        def chart = params.GIT_REPO.toLowerCase()
                        def chartVersion = sh(script: "/helm history staging-${chart} | grep DEPLOYED | awk '{ print \$8 }'", returnStdout: true).trim()

                        version = chartVersion.substring(chart.length() + 1)
                        revision = version.substring(0, 7)

                        sh "echo 'Promoting revision: ${chart}-${version} staging->production'"
                    }
                }
            }
        }
        stage('Find deployment descriptor') {
            steps {
                container('git') {
                    script {
                        withCredentials([[
                                                 $class: 'UsernamePasswordMultiBinding',
                                                 credentialsId: 'github',
                                                 usernameVariable: 'USERNAME',
                                                 passwordVariable: 'PASSWORD'
                                         ]]) {
                            sh "git clone https://$USERNAME:$PASSWORD@github.com/gmandnepr/${params.GIT_REPO}.git"
                            dir ("${params.GIT_REPO}") {
                                sh "git checkout ${revision}"
                            }
                        }
                    }
                }
            }
        }
        stage('Promote version to production') {
            steps {
                container('helm-cli') {
                    script {
                        dir ("${params.GIT_REPO}") {
                            sh "./helm/setRevision.sh ${version}"
                            def registryIp = sh(script: 'getent hosts registry.kube-system | awk \'{ print $1 ; exit }\'', returnStdout: true).trim()
                            sh "/helm upgrade production-${params.GIT_REPO.toLowerCase()} helm/ --install --namespace production --set registry=${registryIp}"
                        }
                    }
                }
            }
        }
    }
}
