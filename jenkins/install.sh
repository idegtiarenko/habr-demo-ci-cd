#!/usr/bin/env bash
kubectl apply -f jenkins.yaml
helm install --name jenkins --namespace jenkins -f demo-values.yaml stable/jenkins
kubectl create clusterrolebinding jenkins --clusterrole cluster-admin --serviceaccount=jenkins:default
