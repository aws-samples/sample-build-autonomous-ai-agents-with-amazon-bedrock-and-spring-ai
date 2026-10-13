# Module: ecr

Creates the Amazon ECR repository that stores the ARM64 agent container image,
along with a lifecycle policy that keeps the repository tidy and an optional,
scoped repository policy.

## Purpose

- Provision a single private ECR repository for the agent image.
- Default to immutable image tags and scan-on-push.
- Encrypt images at rest with AWS-managed AES256, or customer-managed KMS when
  `kms_key_arn` is set.
- Expire untagged images after 3 days and cap the number of retained tagged
  images with a lifecycle policy.
- Optionally attach a repository policy that restricts pull access to an
  explicit list of principals and denies untrusted callers (off by default).
  When enabled it denies non-TLS requests and, when `account_id` is set, denies
  principals outside this account.

## Requirements

| Name      | Version   |
| --------- | --------- |
| terraform | >= 1.6    |
| aws       | ~> 6.0    |

## Providers

| Name | Version |
| ---- | ------- |
| aws  | ~> 6.0  |

## Inputs

| Name                     | Description                                                                                        | Type          | Default     | Required |
| ------------------------ | -------------------------------------------------------------------------------------------------- | ------------- | ----------- | :------: |
| repository_name          | Name of the ECR repository.                                                                        | string        | n/a         |   yes    |
| image_tag_mutability     | Whether image tags can be overwritten. MUTABLE or IMMUTABLE.                                        | string        | "IMMUTABLE" |    no    |
| scan_on_push             | Scan images for vulnerabilities automatically on push.                                             | bool          | true        |    no    |
| kms_key_arn              | Optional KMS key ARN. Empty uses AES256; set to use customer-managed KMS encryption.               | string        | ""          |    no    |
| force_delete             | Allow deleting the repository even if it still contains images.                                    | bool          | true        |    no    |
| keep_last_images         | Number of tagged images to retain before older images expire.                                      | number        | 10          |    no    |
| attach_repository_policy | When true, attach a repository policy restricting access to allowed_principal_arns and denying untrusted callers. | bool | false | no |
| allowed_principal_arns   | IAM principal ARNs allowed to pull when attach_repository_policy is true.                           | list(string)  | []          |    no    |
| account_id               | AWS account id used to deny untrusted cross-account principals in the repository policy. Empty skips the deny. | string | "" | no |
| tags                     | Tags applied to the repository (in addition to provider default_tags).                             | map(string)   | {}          |    no    |

## Outputs

| Name            | Description                        |
| --------------- | ---------------------------------- |
| repository_url  | Repository URL for docker push.    |
| repository_arn  | Repository ARN.                    |
| repository_name | Repository name.                   |

## Usage

```hcl
module "ecr" {
  source          = "../../modules/ecr"
  repository_name = "autonomous-agent-dev"
  account_id      = "123456789012"

  # Secure defaults: immutable tags and scan-on-push are already on.
  keep_last_images = 15

  # Optional: customer-managed KMS encryption instead of AES256.
  # kms_key_arn = "arn:aws:kms:us-east-1:123456789012:key/abcd-1234"

  # Optional: restrict and deny untrusted pull access.
  attach_repository_policy = true
  allowed_principal_arns   = ["arn:aws:iam::123456789012:role/autonomousagentDevAgentCoreRuntimeRole"]

  tags = {
    Application = "autonomous-agent"
    Environment = "dev"
  }
}
```
