resource "aws_dynamodb_table" "events" {
  hash_key = "no"
  range_key = ""
  name = "freedomEvents"
  attribute {
    name = "no"
    type = "S"
  }
  attribute {
    name = ""
    type = ""
  }
}