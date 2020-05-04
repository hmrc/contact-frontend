/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package util

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class FeatureEnablingRuleSpec extends AnyWordSpec with Matchers {

  "FeatureEnablingRule parser" should {

    "parse properly written rule without service restrictions" in {

      FeatureEnablingRule.parse("feature=GetHelpWithThisPageNewWordingOfEntryLink;bucketFrom=10;bucketTo=20") shouldBe
        FeatureEnablingRule(bucketFrom = 10, bucketTo =  20, feature = GetHelpWithThisPageNewWordingOfEntryLink, servicesLimit = None)
    }

    "parse properly written rule with service restrictions" in {

      FeatureEnablingRule.parse("feature=GetHelpWithThisPageNewWordingOfEntryLink;bucketFrom=10;bucketTo=20;services=S1,S2") shouldBe
        FeatureEnablingRule(bucketFrom = 10, bucketTo =  20, feature = GetHelpWithThisPageNewWordingOfEntryLink, servicesLimit = Some(Set("S1", "S2")))
    }

    "throw an exception when rule is incorrect" in {
      an[Exception] shouldBe thrownBy(FeatureEnablingRule.parse("feature=GetHelpWithThisPageNewWordingOfEntryLink;bucketFrom=10;noToBucket"))
    }

  }

}
