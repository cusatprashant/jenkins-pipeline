pipeline {
  agent any

  environment {
    APP_NAME = 'my-app'
    VERSION = '1.0.${BUILD_NUMBER}'
    DRY_RUN = 'false'
  }

  parameters {
    booleanParam(name: 'DRY_RUN', defaultValue: false, description: 'Run in dry mode')
    choice(name: 'TARGET_ENV', choices: ['dev', 'qa', 'staging', 'prod'], description: 'Select environment')
  }

  stages {
    stage('Checkout') {
      steps {
        git url: 'https://github.com/prashant/my-app-repo.git'
      }
    }

    stage('Build') {
      steps {
        echo "Building ${APP_NAME} version ${VERSION}"
        sh './build.sh'
      }
    }

    stage('Unit Test') {
      steps {
        sh './run_tests.sh'
      }
    }

    stage('Deploy to Environment') {
      when {
        expression { return params.TARGET_ENV != null }
      }
      steps {
        script {
          def config = getEnvConfig(params.TARGET_ENV)
          echo "Deploying to ${params.TARGET_ENV} with config: ${config}"
          if (params.DRY_RUN == true) {
            echo "Dry run enabled. Skipping actual deployment."
          } else {
            sh "./deploy.sh --env=${params.TARGET_ENV} --config=${config}"
          }
        }
      }
    }

    stage('Post-Deploy Validation') {
      when {
        expression { return params.TARGET_ENV == 'prod' }
      }
      steps {
        echo "Running smoke tests in production..."
        sh './smoke_test.sh'
      }
    }
  }

  post {
    success {
      echo "✅ Deployment to ${params.TARGET_ENV} succeeded!"
    }
    failure {
      echo "❌ Deployment failed. Triggering rollback..."
      sh './rollback.sh'
    }
  }
}

def getEnvConfig(env) {
  switch(env) {
    case 'dev':
      return 'configs/dev.json'
    case 'qa':
      return 'configs/qa.json'
    case 'staging':
      return 'configs/staging.json'
    case 'prod':
      return 'configs/prod.json'
    default:
      error "Unknown environment: ${env}"
  }
}