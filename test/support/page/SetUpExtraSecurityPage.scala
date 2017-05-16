package support.page

object SetUpExtraSecurityPage extends WebPage {
  override val url = ""
  override def title = "Set up extra security"
  override def isCurrentPage: Boolean =
    pageTitle == title

  def clickNotNow(): Unit = click on linkText("Not now")

}
