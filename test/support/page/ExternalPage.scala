package support.page

object ExternalPage extends WebPage {
  override val url: String = "http://localhost:11111/external/page"

  def feedbackLink: ExternalPage.LinkTextQuery = linkText("Leave feedback")
  def contactHmrcLink: ExternalPage.LinkTextQuery = linkText("Contact HMRC")
  def clickOnFeedbackLink(): Unit = click on feedbackLink
  def clickOnContactHmrcLink(): Unit = click on contactHmrcLink

  override def title: String = "Page with links"
  override def isCurrentPage: Boolean = heading == title
}
