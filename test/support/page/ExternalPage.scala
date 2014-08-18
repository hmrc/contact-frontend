package support.page

object ExternalPage extends WebPage {
  override val url: String = "http://localhost:11111/external/page"

  def feedbackLink = linkText("Leave feedback")
  def contactHmrcLink = linkText("Contact HMRC")
  def clickOnFeedbackLink() = click on feedbackLink
  def clickOnContactHmrcLink() = click on contactHmrcLink
}
