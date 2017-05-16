package support.page

object YouWillNeedToDoThisSoonPage extends WebPage {
  override val url = ""
  override def title = "You'll need to do this soon"
  override def isCurrentPage: Boolean = pageTitle == title

  def clickNotNow(): Unit = click on linkText("Not now")

}

