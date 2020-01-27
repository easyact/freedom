package cn.easyact.fin.controllers.hello

import java.time.LocalDate
import java.util.UUID

import cn.easyact.fin.manager.BudgetUnitCommands._
import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import org.apache.logging.log4j.{LogManager, Logger}

class BuHandler extends RequestHandler[BU, BU] {

  val logger: Logger = LogManager.getLogger(getClass)

  def handleRequest(bu: BU, context: Context): BU = {
    logger.info(s"Received a request: $bu")
    val id = UUID.fromString(bu.name).toString
    register(id, bu.name, Some(LocalDate now()))
    bu.id = id
    bu
  }
}
