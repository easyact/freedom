package cn.easyact.fin.controllers.hello

import java.time.LocalDate
import java.util.UUID

import cn.easyact.fin.manager.BudgetUnitCommands._
import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import org.apache.logging.log4j.{LogManager, Logger}

class BuHandler extends RequestHandler[Map[String, String], BU] {

  val logger: Logger = LogManager.getLogger(getClass)

  def handleRequest(in: Map[String, String], context: Context): BU = {
    logger.info(s"Received a request: $in")
    val name = in("name")
    val id = UUID.fromString(name).toString
    register(id, name, Some(LocalDate now()))
    new BU(id, name)
  }
}
