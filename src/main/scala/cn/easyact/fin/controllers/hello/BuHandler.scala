package cn.easyact.fin.controllers.hello

import java.time.LocalDate
import java.util
import java.util.UUID.randomUUID

import cn.easyact.fin.manager.BudgetUnitCommands._
import cn.easyact.fin.manager.{BudgetUnit, BudgetUnitSnapshot, Command, Expense, Income, MemInterpreter, MemReadService, TimeService}
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.typesafe.scalalogging.Logger

import scala.util.Try

class BuHandler extends RequestHandler[APIGatewayProxyRequestEvent, BuResponse] {

  val logger: Logger = Logger[BuHandler]

  val mapper = new ObjectMapper //with ScalaObjectMapper
  mapper.registerModule(DefaultScalaModule)

  import BudgetUnitSnapshot._
  import cn.easyact.fin.manager.MemInterpreter.eventLog._
  import mapper._

  implicit val timeService: TimeService = TimeService

  def handleRequest(in: APIGatewayProxyRequestEvent, context: Context): BuResponse = {
    logger.info(s"Received a request: $in")
    logger.info(s"getPathParameters: ${in.getPathParameters}")
    Try(process(in)).map(writeValueAsString(_)).fold(e => BuResponse(500, writeValueAsString(e)), BuResponse(200, _))
  }

  private def process(in: APIGatewayProxyRequestEvent) = {
    def post = {
      val dto = readValue(in.getBody, classOf[BU])
      val cmd = register(randomUUID.toString, dto.name, Some(LocalDate.now))
      MemInterpreter(cmd).unsafePerformSync
    }

    def get = allEvents.flatMap(snapshot).fold(
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
      MemInterpreter(script).unsafePerformSync
    }

    def forecast(p: util.Map[String, String]) =
      MemReadService.forecast(p.get("no"), p.get("count").toInt).fold(
        e => throw new RuntimeException(e),
        r => r
      )

    System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "INFO")
    val body = in.getHttpMethod match {
      case "GET" => in.getPathParameters match {
        case null => get
        case p => forecast(p)
      }
      case "POST" => post
      case "PUT" => items(in.getPathParameters)
    }
    body
  }
}

case class ForecastParams(no: String, unit: String, count: Int)