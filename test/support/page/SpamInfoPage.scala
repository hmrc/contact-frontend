package support.page

object SpamInfoPage extends WebPage {
  override def isCurrentPage: Boolean =
    pageSource.contains("Error submitting form. Apologies for any inconvenience caused.")

  override val url: String = "not-used"
}
