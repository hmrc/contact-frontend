package support.page

object ExternalPageToFeedback extends WebPage {
  override val url: String = "http://localhost:11111/external/page-to-feedback"

  def linkToFeedback = linkText("Link to feedback")
  def clickOnLinkToFeedback() = click on linkToFeedback
}
