package cn.easyact.fin.manager.aws

import cn.easyact.fin.manager.{Error, Event, EventStore, StoreInterpreter}
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.dynamodbv2.document.spec.{DeleteItemSpec, QuerySpec, ScanSpec}
import com.amazonaws.services.dynamodbv2.document.{DynamoDB, Item}
import com.amazonaws.services.dynamodbv2.{AmazonDynamoDB, AmazonDynamoDBClientBuilder}
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.scala.{DefaultScalaModule, ScalaObjectMapper}
import com.typesafe.scalalogging.Logger
import scalaz.Scalaz._
import scalaz._

import scala.collection.JavaConverters._

class DynamoDbEventStore(debug: Boolean = false) {
  val log: Logger = Logger[DynamoDbEventStore]
  //  implicit val store =

  val client: AmazonDynamoDB = if (debug)
    AmazonDynamoDBClientBuilder.standard.withEndpointConfiguration(
      new AwsClientBuilder.EndpointConfiguration("http://localhost:8000", "ap-southeast-1"))
      .build
  else
    AmazonDynamoDBClientBuilder.standard.build

  val dynamoDB = new DynamoDB(client)

  private val table = dynamoDB.getTable("freedomEvents")

  val mapper = new ObjectMapper with ScalaObjectMapper
  mapper.registerModule(DefaultScalaModule)
  mapper.registerModule(new JavaTimeModule)
  mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  mapper.activateDefaultTypingAsProperty(BasicPolymorphicTypeValidator.builder().build(), DefaultTyping.NON_FINAL, "dtype")

  def apply[K]: EventStore[K] = new EventStore[K] {
    override def clear(): Unit = table.deleteItem(new DeleteItemSpec)

    override def get(key: K): List[Event[_]] = {
      val spec = new QuerySpec().withHashKey("no", key)
      val r = table.query(spec).asScala.map(toEvent).toList
      log.debug(s"Qureying: ${spec.getHashKey}; Return: $r")
      r
    }

    override def put(key: K, event: Event[_]): Error \/ Event[_] = {
      val className = event.getClass.getName
      val json = mapper.writeValueAsString(event)
      log.debug(s"putting $key: $json, using clasName: $className")
      table.putItem {
        Item.fromJSON(json)
          .withPrimaryKey("no", event.no)
          .withKeyComponent("at", event.at.toString)
          .withString("dtype", className)
      }
      \/-(event)
    }

    override def allEvents: Error \/ List[Event[_]] = table.scan(new ScanSpec).asScala.map(toEvent).toList.right
  }

  private def toEvent[K] = { item: Item =>
    val clazz = Class.forName(s"${item.get("dtype")}")
    mapper.readerFor(clazz).readValue(item.toJSON).asInstanceOf[Event[_]]
  }
}

object DynamoDbEventStore extends DynamoDbEventStore(System.getenv("DEBUG") == "true")

object DynamoDbInterpreter extends StoreInterpreter(DynamoDbEventStore.apply)
