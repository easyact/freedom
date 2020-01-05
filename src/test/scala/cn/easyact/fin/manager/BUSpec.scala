package cn.easyact.fin.manager

import java.time.LocalDate

import org.specs2.mutable.Specification

object BUSpec extends Specification {
  "注册" should {
    "正确" in {
      val now = LocalDate.now
      val script = BudgetUnitCommands.register("1", "test", Some(now))
      script must not beNull
    }
  }
}
