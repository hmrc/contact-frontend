package acceptance.support

import org.scalatest.concurrent.Eventually.PatienceConfig
import org.scalatest.concurrent.Futures.scaled
import org.scalatest.time.{Seconds, Span}

trait IntegrationPatience {
  protected val timeoutInSeconds: Int                        = 2
  implicit val eventuallyWithTimeoutOverride: PatienceConfig =
    PatienceConfig(timeout = scaled(Span(timeoutInSeconds, Seconds)))
}
