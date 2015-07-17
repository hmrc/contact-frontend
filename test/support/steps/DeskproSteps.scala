package support.steps

import org.scalatest.time.{Seconds, Span}
import support.behaviour.NavigationSugar
import support.page.{DeskproSignInPage, DeskproViewTicketPage}

trait DeskproSteps extends NavigationSugar with BaseSteps {

  def ticket_in_deskpro_exists(ticketId: String, name: String, email: String, textInMessageBody: Seq[String]) = {
    val ticketPage = new DeskproViewTicketPage(ticketId)
    go(ticketPage)


    eventually(timeout(Span(10, Seconds))) {
      withClue(s"Expected to be in the DeskPro Log In page, but was on page: $currentUrl - ") {
        DeskproSignInPage should be('isCurrentPage)
      }
    }

    DeskproSignInPage.signIn("haroon.rasheed@digital.hmrc.gov.uk", "W3lc0me-to-D3skpro2")

    eventually(timeout(Span(10, Seconds))) {
      ticketPage.profile.element.text should be(s"$name ($email)")
      ticketPage.messageBody.element.text should containInOrder(textInMessageBody)
    }

  }
}
