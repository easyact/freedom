package cn.easyact.fin.manager

import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDate, LocalTime, YearMonth}
import java.util

import io.cucumber.java.zh_cn.{假设, 当, 那么}
import io.cucumber.java.{Before, _}
import org.assertj.core.api.Assertions.assertThat
import scalaz.{Free, \/}

import scala.collection.JavaConverters._

object MockTimeService extends TimeService {
  var _today: LocalDate = LocalDate.now

  override def now: Instant = _today.atTime(LocalTime.now).atZone(zone).toInstant

  override def today: LocalDate = _today
}

class ScalaStepdefs extends BudgetUnitCommands(MockTimeService) {
  implicit val time: TimeService = MockTimeService

  import time._
  import MemReadService._

  private var result: Error \/ List[MonthlyForecast] = _
  //  private var script: Free[Event, BudgetUnit] = _
  private var no: String = _

  @Before(order = 10)
  def setInterpreter(): Unit = {
    MemInterpreter.eventLog.clear()
    SimulateInterpreter.eventLog.clear()
    //    log.info("Cleared {}, {}", MemInterpreter.eventLog, SimulateInterpreter.eventLog)
  }

  @假设("{string}开了户") def 开了户(name: String): Unit = {
    val script = register(name, name, Some(today), now)
    no = perform(script).no
  }

  @假设("月收入:") def 月收入(incomes: util.List[Income]): Unit = {
    val script = incomes.asScala.map(addItem(no, _, now)).reduceLeft((s, command) => s flatMap (_ => command))
    perform(script)
  }

  @假设("月支出:") def 月支出(expenses: util.List[Expense]): Unit = {
    val script = expenses.asScala.map(addItem(no, _)).reduce((s, income) => s >>= (_ => income))
    perform(script)
  }

  @假设("当前月为{string}") def 当前月为(string: String): Unit = {
    MockTimeService._today = YearMonth.parse(string, DateTimeFormatter.ofPattern("uuuuMM")).atDay(1)
  }

  @假设("当前存款为{amount}") def 当前存款为(amount: Amount): Unit = {
    perform(save(no, amount))
  }


  @当("拉{int}个月的预测") def 拉个月的预测(numberOfMonths: Int): Unit = {
    //    val account: BudgetUnit = perform(script)
    result = forecast(no, numberOfMonths)
  }

  private def perform(script: Command[BudgetUnit]) = MemInterpreter(script).unsafePerformSync

  @那么("生成预测:") def 生成预测(forecastList: util.List[MonthlyForecast]): Unit = {
    result.leftMap(assertThat(_).isNullOrEmpty())
    result.map(_.asJava).foreach(assertThat[MonthlyForecast](_).isEqualTo(forecastList))
  }

  @DataTableType
  def defMonthlyForecast(map: util.Map[String, String]): MonthlyForecast =
    MonthlyForecast(map.get("月").toInt, map.get("余额").toInt)

  @DataTableType
  def defIncome(map: util.Map[String, String]): Income = Income(map)

  @DataTableType
  def defExpense(map: util.Map[String, String]): Expense = Expense(map)

  @ParameterType(name = "amount", value = "[0-9]+")
  def defineAmount(value: String): Amount = BigDecimal(value)
}