#!/usr/bin/env bash

minikube start --cpus 4 --memory 8192 --insecure-registry "10.0.0.0/24"
minikube addons enable registry
minikube addons enable ingress
