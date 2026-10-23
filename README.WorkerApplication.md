# WorkerApplication Configuration and HK2 Bindings

This document inventories all configurations used in WorkerApplication.


## Configuration overview

1. bucketName
- Type and name: Context: bucket. ENV: PUBLISH_S3_BUCKET
- What it does: Defines the S3 bucket used for export.
- Quarkus suggestion: Map to `@ConfigProperty` in `application.properties`.

2. cloudId
- Type and name: Context: cloudId. ENV: CLOUDID
- What it does: Identifies the AWS worker instance. If not set, AWS worker will not start.
- Quarkus suggestion: Map to `@ConfigProperty`.

3. aws.accessKeyId
- Type and name: Context: aws.accessKeyId.
- What id does: AWS access key used for authentication.
- Quarkus suggestion: Map to `quarkus.amazon.credentials.type` or `@ConfigProperty`.

4. aws.secretKey
- Type and name: Context: aws.secretKey.
- What it does: AWS secret key is used for authentication.
- Quarkus suggestion: Map to `quarkus.amazon.credentials.type` or `@ConfigProperty`.

5. AWS Region
- Type and name: ENV (AWS default region configuration)
- What it does: Determines which AWS region is used. And default fallback is eu-west-1.
- Quarkus suggestion: Use `quarkus.amazon.region` or `@ConfigProperty`.

6. PUBLISH_S3_ACCELERATED
- Type and name: ENV: PUBLISH_S3_ACCELERATED 
- What it does: Enables S3 transfer acceleration.
- Quarkus suggestion: Map to `@ConfigProperty`.

7. PUBLISH_FS_PATH
- Type and name: ENV: PUBLISH_FS_PATH
- What is does: Defines local filesystem export path. If set, export uses local file system instead of S3.
- Quarkus suggestion: Map to `@ConfigProperty`.

8. APTAPPLICATION
- Type and name: ENV: APTAPPLICATION
- What it does: Defines application path prefix used by PublishJobService.
- Quarkus suggestion: Map to `@ConfigProperty`.


## HK2 Bindings Overview

1. WorkerStatusReport
- Class / Type: WorkerStatusReport
- Purpose: Used to report worker status inside the application
- Quarkus suggestion: Replace with `@Singleton` CDI bean.

2. region
- Class / Type: Region.
- Purpose: Represents the AWS region used by AWS clients.
- Quarkus suggestion: Inject via `@ConfigProperty` or `@Singleton`.

3. bucketName
- Class / Type: String (named binding)
- Purpose: Holds the configured S3 bucket name. Injected by name in HK2.
- Quarkus suggestion: Use `@ConfigProperty` with `@Named`.

4. exportProviders (Important – Multiple Implementations).
- Class / Type: Map<String, CmsExportProvider>.
- Purpose: Contains multiple export provider implementations.
- "fs" --> CmsExportProviderFsSingle (local file export).
- "s3" --> CmsExportProviderAwsSingle (S3 export).
- Quarkus suggestion: Inject as a singleton bean with `@Produces`.

5. SfnClient
- Class / Type: SfnClient.
- Purpose: AWS Step Functions client
- Quarkus suggestion: Replace with `@Singleton` or Quarkus AWS SDK client injection.

6. ObjectReader / ObjectWriter
- Class / Type: Jackson
- Purpose: JSON serialization and deserialization.
- Quarkus suggestion: Inject via CDI or use `@Produces` beans.

7. PublishServicePe
- Class / Type: PublishServicePe.
- Purpose: Backend publish service.
- Quarkus suggestion: Replace with `@Singleton` CDI bean.

8. PublishJobService
- Class / Type: PublishJobService.
- Purpose: Orchestrates export and publish jobs.
- Quarkus suggestion: Replace with `@Singleton` CDI bean.






