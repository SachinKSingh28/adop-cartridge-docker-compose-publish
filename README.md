# adop-cartridge-docker-compose-publish
This cartridge allows you to publish a docker compose artefact to S3 bucket.

## Using this cartridge
Cartridge can be loaded into an ADOP/C instance. The cartridge expects three credentials to be available credential store.

 * **aws_s3_publish_bucket** : This is a secret text type credential parameter where Credential ID is set to aws\_s3\_publish\_bucket and Secret is set to the name of the S3 bucket where you want to upload the compose artefact. 
 * **aws_s3_publish_path** : This is a secret text type credential parameter where Credential ID is set to aws\_s3\_publish\_path and Secret is set to the S3 bucket path where you want to upload the compose artefact. For instance, if the s3 url is **s3://proxy-bucket/jenkins/compose/artefact/docker-compose.zip** then **proxy-bucket** is **bucket name** and **jenkins/compose/artefact** is **bucket path**
 * **aws_s3_publish_credentials** : This is a username and password type credential parameter where Credential ID is set to aws\_s3\_publish\_credentials, username is AWS access ID and password AWS secret key. These AWS keys must be associated to a AWS user/role with enough privileges to upload the artefact to specified S3 bucket.
 
