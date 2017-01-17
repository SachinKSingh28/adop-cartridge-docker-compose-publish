// Folders
def workspaceFolderName = "${WORKSPACE_NAME}"
def projectFolderName = "${PROJECT_NAME}"

// Variables
// **The git repo variables will be changed to the users' git repositories manually in the Jenkins jobs**
def composeGitRepo = 'adop-catridge-docker-compose-skeleton'
def composeGitUrl = "ssh://jenkins@gerrit:29418/" + projectFolderName + "/" + composeGitRepo

// ** The logrotator variables should be changed to meet your build archive requirements
def logRotatorDaysToKeep = 7
def logRotatorBuildNumToKeep = 7
def logRotatorArtifactsNumDaysToKeep = 7
def logRotatorArtifactsNumToKeep = 7

// Jobs
def getDockerCompose = freeStyleJob(projectFolderName + "/Get_Docker_Compose")
def publishToS3 = freeStyleJob(projectFolderName + "/Publish_To_S3")

// Views
def pipelineView = buildPipelineView(projectFolderName + "/Docker_Compose _Publish")

pipelineView.with{
    title('Docker Compose Publish Pipeline')
    displayedBuilds(5)
    selectedJob(projectFolderName + "/Get_Docker_Compose")
    showPipelineParameters()
    showPipelineDefinitionHeader()
    refreshFrequency(5)
}

// All jobs are tied to build on the Jenkins slave
// The functional build steps for each job have been left empty
// A default set of wrappers have been used for each job
// New jobs can be introduced into the pipeline as required

getDockerCompose.with{
  description("Skeleton application build job.")
  logRotator {
    daysToKeep(logRotatorDaysToKeep)
    numToKeep(logRotatorBuildNumToKeep)
    artifactDaysToKeep(logRotatorArtifactsNumDaysToKeep)
    artifactNumToKeep(logRotatorArtifactsNumToKeep)
  }
  scm{
    git{
      remote{
        url(composeGitUrl)
        credentials("adop-jenkins-master")
      }
      branch("*/master")
    }
  }
  environmentVariables {
      env('WORKSPACE_NAME',workspaceFolderName)
      env('PROJECT_NAME',projectFolderName)
  }
  wrappers {
    preBuildCleanup()
    injectPasswords()
    maskPasswords()
    sshAgent("adop-jenkins-master")
  }
  triggers {
    gerrit {
      events {
          refUpdated()
      }
      project(projectFolderName + '/' + composeGitRepo, 'plain:master')
      configure { node ->
          node / serverName("ADOP Gerrit")
      }
    }
  }
  steps {
    shell('''
	|set -x
	|# Get the compose file root directory and set some environment variables.
	|COMPOSE_FILE=$(find . -name docker-compose.yml | head -1)
	|COMPOSE_DIR=$(dirname ${COMPOSE_FILE})
	|# Zip the compose files 
	|cd ${COMPOSE_DIR}
	|zip -r docker-compose.zip *
	|mv docker-compose.zip ${WORKSPACE}
	|# Go back to the root of workspace
	|cd ${WORKSPACE}
	|set +x	
	|'''.stripMargin())
  }
  publishers{
    archiveArtifacts('docker-compose.zip')
    downstreamParameterized{
      trigger(projectFolderName + "/Publish_To_S3"){
        condition("UNSTABLE_OR_BETTER")
        parameters{
          predefinedProp("B",'${BUILD_NUMBER}')
          predefinedProp("PARENT_BUILD", '${JOB_NAME}')
        }
      }
    }
  }
}

publishToS3.with{
  description("This job runs unit tests on our skeleton application.")
  parameters{
    stringParam("B",'',"Parent build number")
    stringParam("PARENT_BUILD","Get_Docker_Compose","Parent build name")
  }
  logRotator {
    daysToKeep(logRotatorDaysToKeep)
    numToKeep(logRotatorBuildNumToKeep)
    artifactDaysToKeep(logRotatorArtifactsNumDaysToKeep)
    artifactNumToKeep(logRotatorArtifactsNumToKeep)
  }
  wrappers {
    preBuildCleanup()
    injectPasswords()
    maskPasswords()
    sshAgent("adop-jenkins-master")
    credentialsBinding {
    	usernamePassword('AWS_ACCESS_KEY_ID', 'AWS_SECRET_ACCESS_KEY', 'aws_s3_publish_credentials')
        string('S3_PUBLISH_BUCKET', 'aws_s3_publish_bucket')
        string('S3_PUBLISH_PATH', 'aws_s3_publish_path')
    }
  }
  environmentVariables {
      env('WORKSPACE_NAME',workspaceFolderName)
      env('PROJECT_NAME',projectFolderName)
  }
  label("docker")
  steps {
    copyArtifacts("Get_Docker_Compose") {
        includePatterns('*.zip')
        buildSelector {
            buildNumber('${B}')
        }
    }
    shell('''
	|#set -x
	|#Remove trailing slash from S3
	|if [[ ${S3_PUBLISH_PATH%/} == "" ]]; then
	|    aws s3 cp docker-compose.zip s3://${S3_PUBLISH_BUCKET}/docker-compose.zip
	|else
	|    aws s3 cp docker-compose.zip s3://${S3_PUBLISH_BUCKET}/${S3_PUBLISH_PATH%/}/docker-compose.zip
	|fi
	|#set +x'''.stripMargin())
  }
}

