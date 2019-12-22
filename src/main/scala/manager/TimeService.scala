package manager

import java.time.{Instant, LocalDate}

trait TimeService {
  def now: Instant

  def today: LocalDate
}

object TimeService extends TimeService {
  def now: Instant = Instant.now

  def today: LocalDate = LocalDate.now()
}