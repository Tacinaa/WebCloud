#!/usr/bin/env bash
set -euo pipefail

REGION="${AWS_REGION:-eu-west-3}"
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)

TICKETS_BUCKET="utopios-tickets-${ACCOUNT_ID}"
REPORTS_BUCKET="utopios-reports-${ACCOUNT_ID}"

echo "=== Deleting AWS resources for UtopiosTickets ==="
echo "Region     : $REGION"
echo "Account ID : $ACCOUNT_ID"
echo ""

# ── S3 ────────────────────────────────────────────────────────────────────────

delete_bucket() {
  local BUCKET="$1"
  echo "Deleting S3 bucket: $BUCKET"
  aws s3 rm "s3://${BUCKET}" --recursive 2>/dev/null && echo "  Objects deleted." || echo "  Bucket already empty or not found."
  aws s3api delete-bucket --bucket "$BUCKET" --region "$REGION" 2>/dev/null && echo "  Bucket deleted." || echo "  Bucket not found, skipping."
}

delete_bucket "$TICKETS_BUCKET"
delete_bucket "$REPORTS_BUCKET"

# ── DynamoDB ──────────────────────────────────────────────────────────────────

delete_table() {
  local TABLE="$1"
  echo "Deleting DynamoDB table: $TABLE"
  aws dynamodb delete-table --table-name "$TABLE" --region "$REGION" 2>/dev/null \
    && echo "  Table deleted." || echo "  Table not found, skipping."
}

delete_table "utopios-events"
delete_table "utopios-bookings"

echo ""
echo "=== Cleanup complete ==="
