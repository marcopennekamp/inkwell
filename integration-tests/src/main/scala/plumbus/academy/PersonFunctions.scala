package plumbus.academy

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

trait PersonFunctions { self: Person =>
  def age: Long = self.birthday.until(LocalDateTime.now(), ChronoUnit.DAYS)
}
