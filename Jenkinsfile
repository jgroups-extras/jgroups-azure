#!/usr/bin/env groovy

pipeline {
    agent any
    stages {
        stage('SCM Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                script {
                    def mvnHome = tool 'Maven'
                    sh "${mvnHome}/bin/mvn clean install -Dmaven.test.failure.ignore=true"
                    junit '**/target/*-reports/*.xml'
                }
            }
        }

        
        stage('Deploy SNAPSHOT') {
            when {
                branch '1.3'
            }
            steps {
                configFileProvider([configFile(fileId: 'maven-settings-with-deploy-snapshot', variable: 'MAVEN_SETTINGS')]) {
                    script {
                        def mvnHome = tool 'Maven'
                        sh "${mvnHome}/bin/mvn deploy -s $MAVEN_SETTINGS -DskipTests"
                    }
                }
            }
        }
    }
}
