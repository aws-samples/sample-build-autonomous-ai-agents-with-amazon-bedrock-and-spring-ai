# Remote state backend (S3 + DynamoDB lock).
#
# State is kept local by default so the project works out of the box. For team
# use, store state remotely in S3 with a DynamoDB table for state locking. The
# bucket and table names are account and team specific, so this block stays
# commented. Uncomment it and supply your own values, or pass them at init time
# with a partial backend config.
#
# ---------------------------------------------------------------------------
# SECURITY REQUIREMENTS FOR THE STATE BUCKET (mandatory for a security review)
# ---------------------------------------------------------------------------
# Terraform state can contain secrets (for example generated credentials and
# resource metadata), so the state bucket MUST be hardened. The bucket and the
# lock table are created once per account OUTSIDE this configuration (a
# bootstrap must not store its own state in the bucket it is creating). Apply
# ALL of the following to the state bucket:
#
#   1. SSE-KMS encryption with a customer-managed key (not SSE-S3/AES256) and
#      bucket keys enabled to reduce KMS request cost.
#   2. Versioning enabled so state can be recovered after an accidental write.
#   3. A full public access block (all four flags true).
#   4. A TLS-only bucket policy that denies any request where
#      aws:SecureTransport is false.
#   5. A DynamoDB lock table (LockID string hash key) with encryption enabled
#      to serialize concurrent applies.
#   6. MFA delete and access logging are recommended for production buckets.
#
# Exact hardened bootstrap (run once per account; region and names are yours):
#
#   # 1. Create the bucket and block public access
#   #   aws s3api create-bucket --bucket my-tf-state-bucket --region us-east-1
#   #   aws s3api put-public-access-block --bucket my-tf-state-bucket \
#   #     --public-access-block-configuration \
#   #     BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true
#   #
#   # 2. Enable versioning
#   #   aws s3api put-bucket-versioning --bucket my-tf-state-bucket \
#   #     --versioning-configuration Status=Enabled
#   #
#   # 3. Enforce SSE-KMS with a bucket key (replace <kms-key-arn>)
#   #   aws s3api put-bucket-encryption --bucket my-tf-state-bucket \
#   #     --server-side-encryption-configuration '{
#   #       "Rules": [{
#   #         "ApplyServerSideEncryptionByDefault": {
#   #           "SSEAlgorithm": "aws:kms",
#   #           "KMSMasterKeyID": "<kms-key-arn>"
#   #         },
#   #         "BucketKeyEnabled": true
#   #       }]
#   #     }'
#   #
#   # 4. Attach a TLS-only bucket policy (deny non-HTTPS access)
#   #   aws s3api put-bucket-policy --bucket my-tf-state-bucket --policy '{
#   #     "Version": "2012-10-17",
#   #     "Statement": [{
#   #       "Sid": "DenyInsecureTransport",
#   #       "Effect": "Deny",
#   #       "Principal": "*",
#   #       "Action": "s3:*",
#   #       "Resource": [
#   #         "arn:aws:s3:::my-tf-state-bucket",
#   #         "arn:aws:s3:::my-tf-state-bucket/*"
#   #       ],
#   #       "Condition": { "Bool": { "aws:SecureTransport": "false" } }
#   #     }]
#   #   }'
#   #
#   # 5. Create the DynamoDB lock table with encryption enabled
#   #   aws dynamodb create-table --table-name my-tf-locks \
#   #     --attribute-definitions AttributeName=LockID,AttributeType=S \
#   #     --key-schema AttributeName=LockID,KeyType=HASH \
#   #     --billing-mode PAY_PER_REQUEST \
#   #     --sse-specification Enabled=true,SSEType=KMS
#
# Then either uncomment the block below, or keep it commented and run:
#   terraform init \
#     -backend-config="bucket=my-tf-state-bucket" \
#     -backend-config="key=autonomous-agent/dev/terraform.tfstate" \
#     -backend-config="region=us-east-1" \
#     -backend-config="dynamodb_table=my-tf-locks" \
#     -backend-config="encrypt=true" \
#     -backend-config="kms_key_id=<kms-key-arn>"
#
# Use a distinct "key" per environment (for example autonomous-agent/dev/... and
# autonomous-agent/prod/...) so environments never share state.
#
# terraform {
#   backend "s3" {
#     bucket         = "my-tf-state-bucket"
#     key            = "autonomous-agent/dev/terraform.tfstate"
#     region         = "us-east-1"
#     dynamodb_table = "my-tf-locks"
#     encrypt        = true
#     kms_key_id     = "<kms-key-arn>" # SSE-KMS for the state object
#   }
# }
