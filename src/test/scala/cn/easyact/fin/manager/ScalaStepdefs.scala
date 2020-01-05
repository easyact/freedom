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
  var _today = LocalDate.now

  override def now: Instant = _today.atTime(LocalTime.now).atZone(zone).toInstant

  override def today: LocalDate = _today
}

class ScalaStepdefs extends BudgetUnitCommands(MockTimeService) {
  implicit val time = MockTimeService

  import time._
  import MemReadService._

  private var result: Error \/ List[MonthlyForecast] = _
  private var script: Free[Event, BudgetUnit] = _

  @Before(order = 10)
  def setInterpreter(): Unit = {
    MemInterpreter.eventLog.clear()
    SimulateInterpreter.eventLog.clear()
    //    log.info("Cleared {}, {}", MemInterpreter.eventLog, SimulateInterpreter.eventLog)
  }

  @假设("{string}开了户") def 张三在号开了户(name: String): Unit = {
    script = register(name, name, Some(today), now)
  }

  @假设("月收入:") def 月收入(incomes: util.List[Income]): Unit = {
    script = incomes.asScala.foldLeft(script)((s, income) => s flatMap (bu =>
      addItem(bu.no, income, now)))
  }

  @假设("月支出:") def 月支出(incomes: util.List[Expense]): Unit = {
    script = incomes.asScala.foldLeft(script)((s, income) => s >>= (bu => addItem(bu.no, income)))
  }

  @假设("当前月为{string}") def 当前月为(string: String): Unit = {
    MockTimeService._today = YearMonth.parse(string, DateTimeFormatter.ofPattern("uuuuMM")).atDay(1)
  }

  @假设("当前存款为{amount}") def 当前存款为(amount: Amount): Unit = {
    script = script >>= (bu => save(bu.no, amount))
  }


  @当("拉{int}个月的预测") def 拉个月的预测(numberOfMonths: Int): Unit = {
    val task = MemInterpreter(script)
    val account = task.unsafePerformSync
    result = forecast(account.no, numberOfMonths)
  }

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