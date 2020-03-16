package cn.easyact.fin.controllers.hello

import java.time.LocalDate
import java.util
import java.util.UUID.randomUUID

import cn.easyact.fin.manager.BudgetUnitCommands._
import cn.easyact.fin.manager.{AggregateId, BudgetUnit, BudgetUnitSnapshot, Command, EventStore, Expense, Income, MemInterpreter, ReadService, TimeService}
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.typesafe.scalalogging.Logger

import scala.collection.Set
import scala.util.Try

class BuHandler extends RequestHandler[APIGatewayProxyRequestEvent, BuResponse] {

  val logger: Logger = Logger[BuHandler]

  val mapper = new ObjectMapper //with ScalaObjectMapper
  mapper.registerModule(DefaultScalaModule)
  mapper.registerModule(new JavaTimeModule)

  import BudgetUnitSnapshot._
  import cn.easyact.fin.manager.MemInterpreter._

  implicit val events: EventStore[AggregateId] = eventLog

  import mapper._

  def handleRequest(in: APIGatewayProxyRequestEvent, context: Context): BuResponse = {
    logger.info(s"Received a request: $in")
    logger.info(s"getPathParameters: ${in.getPathParameters}")
    Try(process(in)).map(writeValueAsString(_)).fold(e => BuResponse(500, writeValueAsString(e)), BuResponse(200, _))
  }

  private def process(in: APIGatewayProxyRequestEvent) = {
    def post = {
      val dto = readValue(in.getBody, classOf[BU])
      val cmd = register(randomUUID.toString, dto.name, Some(LocalDate.now))
      apply(cmd).unsafePerformSync
    }

    def get = events.allEvents.flatMap(snapshot).fold(
      err => throw new RuntimeException(err),
      m => m
    )

    def items(p: util.Map[String, String]) = {
      val dto = readValue(in.getBody, classOf[Items])
      val no = p.get("no")

      val incomeScript = dto.incomes.map(Income(_)).map(addItem(no, _)).reduceLeft[Command[BudgetUnit]] { (s, s1) =>
        s.flatMap(_ => s1)
      }
      val script = dto.expenses.map(Expense(_)).foldLeft(incomeScript) { (s, item) =>
        s >>= (_ => addItem(no, item))
      }
      apply(script).unsafePerformSync
    }

    def itemsByNo(no: String) = for {
      es <- events.events(no)
      bUs <- snapshot(es)
    } yield bUs(no)

    def forecast(p: util.Map[String, String]) =
      ReadService.forecast(p.get("no"), p.get("count").toInt).fold(
        e => throw new RuntimeException(e),
        r => r
      )

    System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "INFO")
    in.getHttpMethod match {
      case "GET" => in.getPathParameters.keySet match {
        case p if p.isEmpty => get
        case p if p.contains("unit") => forecast(in.getPathParameters)
        case _ => itemsByNo(in.getPathParameters.get("no"))
      }
      case "POST" => post
      case "PUT" => items(in.getPathParameters)
    }
  }

}

case class ForecastParams(no: String, unit: String, count: Int)