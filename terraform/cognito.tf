resource "aws_cognito_user_pool_client" "web" {
  name = "web"
  user_pool_id = aws_cognito_user_pool.freedom.id
}
resource "aws_cognito_user_pool" "freedom" {
  name = "freedom"
}

output "cognito" {
  value = {
    userPool = aws_cognito_user_pool.freedom.endpoint
    client = aws_cognito_user_pool_client.web.id
  }
}