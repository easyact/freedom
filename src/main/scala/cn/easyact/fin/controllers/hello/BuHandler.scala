package cn.easyact.fin.controllers.hello

import java.time.{Instant, LocalDate}
import java.util
import java.util.UUID.randomUUID

import cn.easyact.fin.manager.BudgetUnitCommands._
import cn.easyact.fin.manager.{AggregateId, BudgetUnit, BudgetUnitSnapshot, Command, Error, Event, EventStore, Expense, Income, ReadService}
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.typesafe.scalalogging.Logger
import scalaz.Scalaz._
import scalaz._

import scala.language.implicitConversions

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

    process(in).map(writeValueAsString).fold(
      {
        case e@"Not found" => BuResponse(404, e)
        case e => BuResponse(500, e)
      },
      BuResponse(200, _))
  }

  private def process(in: APIGatewayProxyRequestEvent) = {
    def budgetUnit: Error \/ BudgetUnit = {
      val dto = readValue(in.getBody, classOf[BU])
      val cmd = register(randomUUID.toString, dto.name, Some(LocalDate.now))
      apply(cmd).unsafePerformSync.right
    }

    def get = events.allEvents.flatMap(snapshot)

    def items(p: util.Map[String, String]) = {
      val dto = readValue(in.getBody, classOf[Items])
      val no = p.get("no")
      val initScript: Command[BudgetUnit] = getItems(no).fold(
        _ => register(no, no, Some(LocalDate.now), Instant.now),
        Free.pure)

      val incomeScript = dto.incomes.map(Income(_)).map(addItem(no, _)).foldLeft(initScript) { (script, command) =>
        script.flatMap(_ => command)
      }
      val script = dto.expenses.map(Expense(_)).map(addItem(no, _)).foldLeft(incomeScript) { (script, command) =>
        script.flatMap(_ => command)
      }
      apply(script).unsafePerformSync.right[Error]
    }

    def getItems(no: String) = for {
      l <- events.events(no)
      s <- snapshot(l)
      u <- s.get(no)
    } yield u

    def forecast(p: util.Map[String, String]) =
      ReadService.forecast(p.get("no"), p.get("count").toInt)

    System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "INFO")
    in.getHttpMethod match {
      case "GET" => in.getPathParameters match {
        case params if in.getPath.matches("/budget-units/.*/forecast.*") => forecast(params)
        case params if in.getPath.matches("/budget-units/.*/items") => getItems(params.get("no"))
        case null => get
      }
      case "POST" => budgetUnit
      case "PUT" => items(in.getPathParameters)
    }
  }

  implicit def option2Either[A](o: Option[A]): Error \/ A = o match {
    case Some(a) => a.right
    case None => "Not found".left
  }
}

case class ForecastParams(no: String, unit: String, count: Int)