package support.steps

trait ObservationSteps extends BaseSteps {

  def i_dont_see(text: String) = {
    tagName("body").element.text must not include text
  }

  def i_see(textsToFind: String*) = {
    eventually {
      val body = tagName("body").element
      body.text must containInOrder(textsToFind.toList)
    }
  }

  def i_see_links(linksToFind: String*) = {
    eventually {
      val links = tagName("a").findAllElements.map(_.attribute("href")).flatten
      linksToFind.forall(links.contains(_)) mustBe true
    }
  }
}
