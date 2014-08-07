package support.steps


trait ObservationSteps extends BaseSteps {

  def i_dont_see(text: String) = {
    tagName("body").element.text should not include text
  }

  def i_see(textsToFind: String*) = {
    eventually {
      val body = tagName("body").element
      body.text should containInOrder(textsToFind.toList)
    }
  }
}
