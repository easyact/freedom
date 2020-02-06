package cn.easyact.fin.manager.aws

import cn.easyact.fin.manager.{Error, Event, EventStore, ReadService, StoreInterpreter}
import com.amazonaws.services.dynamodbv2.document.{DynamoDB, Item, ItemCollection, QueryOutcome}
import com.amazonaws.services.dynamodbv2.document.spec.{DeleteItemSpec, QuerySpec, ScanSpec}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import scalaz._
import Scalaz._
import com.typesafe.scalalogging.Logger

import scala.collection.JavaConverters._

object DynamoDbEventStore {
  val log: Logger = Logger[DynamoDbEventStore.type]

  import com.amazonaws.client.builder.AwsClientBuilder
  import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
  import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder

  val client: AmazonDynamoDB = AmazonDynamoDBClientBuilder.standard.withEndpointConfiguration(
    new AwsClientBuilder.EndpointConfiguration("http://localhost:8000", "ap-southeast-1")).build

  val dynamoDB = new DynamoDB(client)

  private val table = dynamoDB.getTable("freedomEvents")

  val mapper = new ObjectMapper with ScalaObjectMapper

  def apply[K]: EventStore[K] = new EventStore[K] {
    override def clear(): Unit = table.deleteItem(new DeleteItemSpec)

    override def get(key: K): List[Event[_]] = {
      val spec = new QuerySpec().withHashKey("no", key)
      table.query(spec).asScala.map(toEvent).toList
    }

    override def put(key: K, event: Event[_]): Error \/ Event[_] = {
      val className = event.getClass.getName
      log.debug(s"putting $key: $event, using clasName: $className")
      table.putItem {
        new Item().withPrimaryKey("no", event.no)
          .withKeyComponent("at", event.at.toString)
          .withString("dtype", className)
      }
      \/-(event)
    }

    override def allEvents: Error \/ List[Event[_]] = {
      table.scan(new ScanSpec).asScala.map(toEvent).toList.right
    }
  }

  private def toEvent[K]: Item => Event[_] = {
    item: Item =>
      val clazz = Class.forName(s"${item.get("dtype")}")
      mapper.readerFor(clazz).readValue(item.toJSON)
  }
}

object DynamoDbInterpreter extends StoreInterpreter(DynamoDbEventStore.apply)
