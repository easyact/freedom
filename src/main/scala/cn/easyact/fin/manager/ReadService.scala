package cn.easyact.fin.manager

import java.time.Instant.now
import java.time._
import java.time.temporal.ChronoUnit.MONTHS
import java.util.UUID
import java.util.UUID.randomUUID

import com.typesafe.scalalogging.Logger
import scalaz.Scalaz._
import scalaz._
import scalaz.concurrent.Task

import scala.collection.mutable
import scala.language.implicitConversions

trait BudgetUnitEvent extends Event[BudgetUnit]

trait IncomeEvent extends BudgetUnitEvent

trait OutcomeEvent extends BudgetUnitEvent

case class Registered(no: String, name: String, startDate: Option[LocalDate], at: Instant = now) extends BudgetUnitEvent

case class SalaryAdded(no: String, amount: Amount, at: Instant = now) extends BudgetUnitEvent

case class ItemAdded(no: String, income: BudgetItem, at: Instant = now) extends BudgetUnitEvent

case class Earned(no: String, amount: Amount, budgetItem: Option[BudgetItem] = None, at: Instant = now) extends IncomeEvent

trait ReadService {
  def forecast(no: AggregateId, numberOfMonths: Int)(implicit time: TimeService): Error \/ List[MonthlyForecast]
}

object MemReadService extends ReadService {

  import BudgetUnitCommands._
  import BudgetUnitSnapshot._
  import MemInterpreter.eventLog._

  val log: Logger = Logger[ReadService]

  override def forecast(no: AggregateId, numberOfMonths: Int)(implicit time: TimeService): Error \/ List[MonthlyForecast] = {
    import SimulateInterpreter.eventLog
    eventLog.clear()
    val startMonth = YearMonth.from(time.today)
    val endMonth = startMonth.plusMonths(numberOfMonths)
    val eventList: Error \/ List[Event[_]] = events(no)
    for {
      l <- eventList
      _ <- l.map(e => (e.at, randomUUID()) -> e).foreach { t =>
        eventLog.put(t._1, t._2)
      }.right
    } yield ()
    log.debug("Added result: {}", eventLog)

    //    eventList foreach {
    //      _.map(e => (e.at, UUID.randomUUID()) -> e)
    //        .foreach { t =>
    //          eventLog.put(t._1, t._2)
    //          log.info("Added result: ", eventLog)
    //        }
    //    }

    val bu: Error \/ BudgetUnit = for {
      l <- eventList
      s <- snapshot(l)
    } yield s(no)

    bu.map(_.budgetItems.map(simulateScript(no, endMonth)))
      .map(_ reduceLeft { (script, command) => script.flatMap(_ => command) })
      .foreach(SimulateInterpreter(_).unsafePerformSync)
    log.debug("EventLog is {}", eventLog)
    val groups: Map[YearMonth, List[Event[_]]] = eventLog
      .groupBy(tuple => YearMonth.from(tuple._1._1.atZone(zone)))
      .mapValues(_.values.toList)
      .withDefaultValue(List())
    (0 to numberOfMonths)
      .map(n => startMonth.plusMonths(n) -> (0 to n)
        .map(startMonth.plusMonths(_))
        .map(groups(_))
        .reduceLeft(_ ++ _))
      .foldLeft(List[MonthlyForecast]()) {
        (list: List[MonthlyForecast], t) =>
          val b = t._2.right
            .flatMap(snapshot)
            .flatMap(_.get(no))
            .map(_.balance)
            .getOrElse(BigDecimal(0))
          list :+ MonthlyForecast(t._1, b)
      }.right
  }

  private def simulateScript(no: AggregateId, endMonth: YearMonth) = {
    item: BudgetItem => {
      val startMonth = YearMonth.from(item.start)
      val months = startMonth.until(endMonth, MONTHS)
      (0L to(months, item.intervalMonth))
        .map(startMonth.plusMonths)
        .map(_.atEndOfMonth.atTime(23, 59, 59, 999999999).atZone(zone).toInstant)
        .map(earn(no, item, _))
        .reduceLeft { (script, command) => script.flatMap(_ => command) }
    }
  }
}

case class BudgetUnitCommands(ts: TimeService) {
  private implicit def liftEvent[A](e: Event[A]): Command[A] = Free.liftF(e)

  def register(no: String, name: String, startDate: Option[LocalDate], at: Instant = ts.now): Command[BudgetUnit] =
    Registered(no, name, startDate, at)

  def addSalary(no: String, amount: Amount): Command[BudgetUnit] = SalaryAdded(no, amount, ts.now)

  def addItem(no: String, income: BudgetItem, at: Instant = ts.now): Command[BudgetUnit] =
    ItemAdded(no, income, at)

  def gainSalary(no: String, amount: Amount): Command[BudgetUnit] = Earned(no, amount, None, ts.now)

  def save(no: AggregateId, amount: Amount): Command[BudgetUnit] = Earned(no, amount, None, ts.now)

  def earn(no: AggregateId, budgetItem: BudgetItem, instant: Instant = ts.now): Command[BudgetUnit] = {
    val amount = budgetItem match {
      case _: Income => budgetItem.amount
      case _: Expense => -budgetItem.amount
    }
    Earned(no, amount, Some(budgetItem), instant)
  }
}

object BudgetUnitCommands extends BudgetUnitCommands(TimeService)

object MemInterpreter extends Interpreter[BudgetUnit] {
  val eventLog: EventStore[AggregateId] = MemEventStore.apply

  import BudgetUnitSnapshot._
  import eventLog._

  override def step: Event ~> Task = new (Event ~> Task) {
    override def apply[A](e: Event[A]): Task[A] = e match {
      case _: BudgetUnitEvent => Task {
        val no = e.no
        put(no, e)
        events(no)
          .flatMap(snapshot)
          .fold(
            err => throw new RuntimeException(err),
            m => m(no)
          )
      }
      case _ => throw new RuntimeException(s"不支持的解释步骤: $e")
    }
  }

}

object BudgetUnitSnapshot extends Snapshot[BudgetUnit] {
  val log: Logger = Logger[Snapshot[BudgetUnit]]

  override def updateState(state: Map[String, BudgetUnit], e: Event[_]): Map[String, BudgetUnit] = {
    log.trace(s"updating state: $state. Event: $e")
    e match {
      case Registered(no, name, s, _) =>
        state + (no -> BudgetUnit(no, name, s.get))
      case Earned(no, amount, _, _) =>
        state.get(no) match {
          case None =>
            log.error(s"针对不存在的账户操作: $no")
            state
          case Some(bu) =>
            state + (no -> bu.copy(balance = bu.balance + amount))
        }
      case ItemAdded(no, i, _) =>
        val bu = state(no)
        state + (no -> bu.copy(budgetItems = bu.budgetItems.filterNot(_.id == i.id) :+ i))
    }
  }
}

object SimulateInterpreter extends Interpreter[BudgetUnit] {
  val eventLog: mutable.TreeMap[(Instant, UUID), Event[_]] = mutable.TreeMap.empty

  override def step: Event ~> Task = new (Event ~> Task) {
    override def apply[A](e: Event[A]): Task[A] =
      e match {
        case event: BudgetUnitEvent => Task {
          eventLog((e.at, randomUUID())) = event
          null
        }
        case _ => throw new RuntimeException("不支持的解释步骤")
      }
  }

}