# AWS build service

Instant Room uses **AWS CodeBuild**, **Amazon S3**, **CloudWatch Logs**, **KMS**, **IAM**, and **CloudFormation** for on-demand Android builds. There are no cloud calls from the Android app, no photo-upload service, and no credentials in the APK.

The `instant-room-build` stack was provisioned in **eu-central-1** through the Adam connection. It contains one on-demand CodeBuild project (`instant-room-build-android`), one private versioned SSE-S3 bucket, one KMS-encrypted 14-day log group, and a narrowly scoped CodeBuild role. Concurrency is one; each build is limited to thirty minutes. No webhooks, always-running servers, VPC, NAT gateways, or mobile registration service are created. Builds incur CodeBuild usage charges; storage, logs and the customer-managed KMS key also incur standard AWS charges. This is not advertised as free.

## Validate or reproduce infrastructure

```bash
cfn-lint --regions eu-central-1 -t infra/aws/build-stack.json
cfn-guard validate --rules infra/aws/security.guard --data infra/aws/build-stack.json
aws cloudformation validate-template --template-body file://infra/aws/build-stack.json --region eu-central-1
aws cloudformation deploy --stack-name instant-room-build --template-file infra/aws/build-stack.json --capabilities CAPABILITY_IAM --region eu-central-1
```

The template uses generated bucket/role/key names. No AWS access keys, GitHub personal access tokens, or source authorization connections are needed in the build container: it receives an approved immutable source ZIP from S3. The build role can read only `source/*`, write only `builds/*` in its own bucket, and write its own logs.

## Build a specific committed revision

Run these commands from the repository root with AWS CLI credentials for the intended account. The packaging script includes committed files only. Commit all intended changes first.

```bash
python3 scripts/package-source.py --output /tmp/instant-room-source.zip
revision=$(git rev-parse HEAD)
bucket=$(aws cloudformation describe-stacks --stack-name instant-room-build --region eu-central-1 --query 'Stacks[0].Outputs[?OutputKey==`ArtifactsBucket`].OutputValue' --output text)
aws s3 cp /tmp/instant-room-source.zip "s3://$bucket/source/$revision.zip" --region eu-central-1
aws codebuild start-build --project-name instant-room-build-android --source-location-override "$bucket/source/$revision.zip" --environment-variables-override "name=EXPECTED_COMMIT,value=$revision,type=PLAINTEXT" --region eu-central-1
```

`EXPECTED_COMMIT` is not a secret. The build checks it against the snapshot's `SOURCE_COMMIT` before compilation. Source prefixes use commit SHA rather than overwriting another build's input. CodeBuild artifacts use a separate unique build-ID prefix. Do not start a source snapshot from an untrusted contributor with the build role.

The build installs JDK 21 via the managed CodeBuild runtime and Android SDK Platform 37.0/Build Tools 37.0.0, then runs core unit tests, Room/Robolectric tests, lint, app assembly, and instrumentation APK compilation. It packages APKs, lint output, JUnit XML, the source revision, and APK SHA-256. It does **not** run connected emulator tests or verify hardware.

Retrieve a private build artifact from the CodeBuild console or with `aws s3 cp` using authorized AWS credentials. No public bucket ACL, distribution site, or persistent public download link is created.

## Retention and cleanup

Build objects expire after 30 days; noncurrent build/source versions expire after 7 days. The bucket and KMS key have `Retain` deletion policies. Stack deletion leaves retained data and the KMS key behind, so removing the stack alone does not stop all storage/key charges. Deliberately remove retained build artifacts, bucket and key after deciding they are no longer required. Never schedule deletion of a key while its encrypted logs are needed.

## Validation

Local cfn-lint completed with zero findings. The two project-specific cfn-guard rules passed (private encrypted retained artifacts; bounded unprivileged builds). CloudFormation template validation and change-set validation passed before provisioning. See the Android validation report for the actual build execution outcome.
