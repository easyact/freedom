resource "aws_codebuild_project" "freedom" {
  name = "freedom"
  service_role = "arn:aws:iam::644386201194:role/service-role/codebuild-freedom-service-role"
  artifacts {
    encryption_disabled = false
    name = "freedom"
    override_artifact_name = false
    packaging = "NONE"
    type = "CODEPIPELINE"
  }
  environment {
    compute_type = "BUILD_GENERAL1_SMALL"
    image = "aws/codebuild/standard:3.0"
    image_pull_credentials_type = "CODEBUILD"
    privileged_mode = false
    type = "LINUX_CONTAINER"
  }
  source {
    git_clone_depth = 0
    insecure_ssl = false
    report_build_status = false
    type = "CODEPIPELINE"
  }
}