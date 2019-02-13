package unit.controllers

import controllers.{ContactForm, SpamHandler}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}
import play.api.mvc.Results
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SpamHandlerSpec extends WordSpec with Matchers with ScalaFutures {

  "Spam Handler" should {
    "detect ideographic (Chinese, Japanese, Korean or Vietnamese) chars" in {
      messagesWithIdeographicCharacters.foreach { m =>
        val result = SpamHandler.ignoreIfSpam(contactForm(m))(Future(Results.NoContent))

        status(result) shouldBe 200
        contentAsString(result) shouldBe "Error submitting form. Apologies for any inconvenience caused."
      }
    }
  }

  def contactForm(comments: String) = ContactForm(
    contactName = "name",
    contactEmail = "contactEmail",
    contactComments = comments,
    isJavascript = false,
    referer = "referrer",
    csrfToken = "csrfToken"
  )

  val messagesWithIdeographicCharacters = Seq(
    "golden opportunity 金",
    "猪 something",
    "伽",
    "金猪贺岁伽企鹅961105608即送⑤⑧彩锦 可出款链接199270COM",
    "金猪贺岁添加企鹅656575311即送⑤⑧彩锦可出款链接199246COM",
    "金猪贺岁伽企鹅961105608即送⑤⑧彩锦 可出款链接199270COM",
    "金猪贺岁添加企鹅656575311即送⑤⑧彩锦可出款链接199246COM",
    "金猪贺岁伽企鹅961105608即送⑤⑧彩锦 可出款链接199270COM",
    "新葡京864949點com註冊送58元財金 加專員QQ：1436317566領取! 更有丰厚首存优惠！不要错过！"
  )

}
