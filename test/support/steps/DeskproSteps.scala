package support.steps

import org.scalatest.time.{Seconds, Span}
import support.page.{DeskproSignInPage, DeskproViewTicketPage}

trait DeskproSteps extends BaseSteps {

  def ticket_in_deskpro_exists(ticketId: String, name: String, email: String, textInMessageBody: Seq[String]) = {
    val ticketPage = new DeskproViewTicketPage(ticketId)
    go to ticketPage

    val deskproSignInPage = new DeskproSignInPage()

    eventually(timeout(Span(10, Seconds))) {
      withClue(s"Expected to be in the DeskPro Log In page, but was on page: $currentUrl - ") {
        deskproSignInPage should be('isCurrentPage)
      }
    }

    deskproSignInPage.signIn("tim.britten@digital.cabinet-office.gov.uk", "Skanpowv7")

    eventually(timeout(Span(10, Seconds))) {
      ticketPage.profile.element.text should be(s"$name ($email)")
      ticketPage.messageBody.element.text should containInOrder(textInMessageBody)
    }

  }
}
