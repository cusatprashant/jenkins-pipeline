pipeline {
  agent any

  environment {
    TF_WORKSPACE = "${params.TARGET_ENV}"
    TF_DIR = 'infra/terraform'
    DRY_RUN = "${params.DRY_RUN}"
  }

  parameters {
    choice(name: 'TARGET_ENV', choices: ['dev', 'qa', 'staging', 'prod'], description: 'Select environment')
    booleanParam(name: 'DRY_RUN', defaultValue: false, description: 'Run in dry mode')
  }

  stages {
    stage('Checkout') {
      steps {
        git url: 'https://github.com/prashant/my-app-repo.git'
      }
    }

    stage('Terraform Init') {
      steps {
        dir("${TF_DIR}") {
          sh 'terraform init'
        }
      }
    }

    stage('Terraform Workspace') {
      steps {
        dir("${TF_DIR}") {
          sh """
            terraform workspace select ${TF_WORKSPACE} || terraform workspace new ${TF_WORKSPACE}
          """
        }
      }
    }

    stage('Terraform Plan') {
      steps {
        dir("${TF_DIR}") {
          sh "terraform plan -var-file=${TF_WORKSPACE}.tfvars"
        }
      }
    }

    stage('Terraform Apply') {
      when {
        expression { return DRY_RUN == 'false' }
      }
      steps {
        dir("${TF_DIR}") {
          sh "terraform apply -auto-approve -var-file=${TF_WORKSPACE}.tfvars"
        }
      }
    }

    stage('App Build & Deploy') {
      steps {
        echo "Deploying app to ${TF_WORKSPACE} environment..."
        sh "./deploy.sh --env=${TF_WORKSPACE}"
      }
    }
  }

  post {
    success {
      echo "✅ Infra and App deployed to ${TF_WORKSPACE} successfully!"
    }
    failure {
      echo "❌ Deployment failed. Consider running terraform destroy or rollback."
    }
  }
}