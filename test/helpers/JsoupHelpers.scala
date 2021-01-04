/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package helpers

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import play.twirl.api.Html

trait JsoupHelpers {
  implicit class RichHtml(html: Html) {
    def select(cssQuery: String): Elements =
      parseNoPrettyPrinting(html).select(cssQuery)
  }

  // otherwise Jsoup inserts linefeed https://stackoverflow.com/questions/12503117/jsoup-line-feed
  def parseNoPrettyPrinting(html: Html): Document = {
    val doc = Jsoup.parse(html.body)
    doc.outputSettings().prettyPrint(false)
    doc
  }

  def asDocument(html: Html): Document = Jsoup.parse(html.toString())
}
