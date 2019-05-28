#!/usr/bin/env bash
kubectl apply -f jenkins.yaml
kubectl create clusterrolebinding jenkins --clusterrole cluster-admin --serviceaccount=jenkins:default
helm install --name jenkins --namespace jenkins --values demo-values.yaml --version 1.1.22 stable/jenkins
