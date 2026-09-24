# Deployment Providers Reference — DevPilot AI

## 1. Local Docker Provider (`LocalDockerCloudProvider`)
- Uses local Docker daemon / container runtime.
- Port allocation managed by `PortAllocationService`.
- Zero cloud credential requirement.

## 2. AWS Cloud Provider (`AwsCloudProvider`)
- Deploys container artifacts to AWS ECS (Elastic Container Service) or App Runner.
- Validates Access Key and Secret Key encrypted with `SecretManager`.
- Configurable region (default `us-east-1`).

## 3. Azure Cloud Provider (`AzureCloudProvider`)
- Deploys container artifacts to Azure Container Apps (ACA).
- Supports custom domain configuration and automated TLS termination.

## 4. GCP Cloud Provider (`GcpCloudProvider`)
- Deploys container artifacts to Google Cloud Run serverless container runtime.
- Automatic horizontal scaling from 0 to N instances.
