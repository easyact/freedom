package cn.easyact.fin.controllers.hello

import java.time.LocalDate
import java.util
import java.util.UUID.randomUUID

import cn.easyact.fin.manager.BudgetUnitCommands._
import cn.easyact.fin.manager.{BudgetItem, BudgetUnit, BudgetUnitSnapshot, Command, Event, Expense, Income, MemInterpreter, MemReadService, TimeService}
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.apache.logging.log4j.{LogManager, Logger}
import scalaz.Free

import scala.collection.JavaConverters._

class BuHandler extends RequestHandler[APIGatewayProxyRequestEvent, BuResponse] {

  val logger: Logger = LogManager.getLogger(getClass)

  val mapper = new ObjectMapper //with ScalaObjectMapper
  mapper.registerModule(DefaultScalaModule)

  import BudgetUnitSnapshot._
  import cn.easyact.fin.manager.MemInterpreter.eventLog._
  import mapper._

  implicit val timeService: TimeService = TimeService

  def handleRequest(in: APIGatewayProxyRequestEvent, context: Context): BuResponse = {
    logger.info(s"Received a request: $in")
    logger.info(s"getPathParameters: ${in.getPathParameters}")

    def post = {
      val sBu = in.getBody
      val dto = readValue(sBu, classOf[BU])
      val name = dto.name
      val id = randomUUID.toString
      val script = register(id, name, Some(LocalDate now()))
      val task = MemInterpreter(script)
      task.unsafePerformSync
    }

    def get = allEvents.flatMap(snapshot).fold(
      err => throw new RuntimeException(err),
      m => m
    )

    def items(p: util.Map[String, String]) = {
      val sBu = in.getBody
      val dto = readValue(sBu, classOf[Items])
      val no = p.get("no")
      val zero = Free.liftF[Event, BudgetUnit](null)

      def compose(zero: Command[BudgetUnit], items: List[BudgetItem]) = {
        items.foldLeft(zero) { (s, item) =>
          s >>= (_ => addItem(no, item))
        }
      }

      val incomeScript = compose(zero, dto.incomes.map(Income(_)))
      val script = compose(incomeScript, dto.expenses.map(Expense(_)))
      val task = MemInterpreter(script)
      task.unsafePerformSync
    }

    def forecast(p: util.Map[String, String]) =
      MemReadService.forecast(p.get("no"), p.get("count").toInt).fold(
        e => throw new RuntimeException(e),
        r => r
      )

    val body = in.getHttpMethod match {
      case "GET" => in.getPathParameters match {
        case null => get
        case p => forecast(p)
      }
      case "POST" => post
      case "PUT" => items(in.getPathParameters)
    }
    BuResponse(200, writeValueAsString(body))
  }
}

case class ForecastParams(no: String, unit: String, count: Int)