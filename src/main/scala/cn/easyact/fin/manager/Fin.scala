package cn.easyact.fin.manager

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, Year, YearMonth}

case class MonthlyForecast(month: YearMonth, balance: Amount)

object MonthlyForecast {
  def apply(month: Int, balance: Amount)(implicit time:TimeService): MonthlyForecast =
    MonthlyForecast(YearMonth.of(time.today.getYear, month), balance)
}

case class BudgetUnit(no: String, name: String, start: LocalDate, balance: Amount = 0,
                      budgetItems: List[BudgetItem] = List.empty) extends Aggregate

sealed trait BudgetItem {
  val t: String
  val amount: Amount
  val intervalMonth: Int
  val start: LocalDate
}

sealed trait Income extends BudgetItem

sealed trait Expense extends BudgetItem

trait Factory[T <: BudgetItem] {
  val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMM")

  def apply(map: java.util.Map[String, String]): T =
    apply(map.get("项目"), map.get("数额").toDouble, map.get("月周期").toInt, map.get("开始月份"))

  def apply(t: String, amount: Amount, intervalMonth: Int, startMonth: String): T = {
    val start = YearMonth.parse(startMonth, formatter)
    apply(t, amount, intervalMonth, start)
  }

  def apply(t: String, amount: Amount, intervalMonth: Int, start: YearMonth): T =
    apply(t, amount, intervalMonth, start.atEndOfMonth())

  def apply(t: String, amount: Amount, intervalMonth: Int, start: LocalDate): T
}

object Income extends Factory[Income] {
  override def apply(t: String, amount: Amount, intervalMonth: Int, start: LocalDate): Income = t match {
    case "工资" => Salary(amount, intervalMonth, start)
    case _ => OtherIncome(amount, intervalMonth, start, t)
  }
}

object Expense extends Factory[Expense] {
  def apply(t: String, amount: Amount, intervalMonth: Int, start: LocalDate): Expense = t match {
    case _ => OtherExpense(amount, intervalMonth, start, t)
  }
}

case class Salary(amount: Amount, intervalMonth: Int, start: LocalDate) extends Income {
  override val t: String = "salary"
}

case class OtherIncome(amount: Amount, intervalMonth: Int, start: LocalDate, t: String) extends Income

case class OtherExpense(amount: Amount, intervalMonth: Int, start: LocalDate, t: String) extends Expense