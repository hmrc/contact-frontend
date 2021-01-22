/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package util

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class FeatureEnablingRuleSpec extends AnyWordSpec with Matchers {

  "FeatureEnablingRule parser" should {

    "parse properly written rule without service restrictions" in {

      FeatureEnablingRule.parse("feature=GetHelpWithThisPageFeatureFieldHints;bucketFrom=10;bucketTo=20") shouldBe
        FeatureEnablingRule(
          bucketFrom = 10,
          bucketTo = 20,
          feature = GetHelpWithThisPageFeatureFieldHints,
          servicesLimit = None
        )
    }

    "parse properly written rule with service restrictions" in {

      FeatureEnablingRule.parse(
        "feature=GetHelpWithThisPageFeatureFieldHints;bucketFrom=10;bucketTo=20;services=S1,S2"
      ) shouldBe
        FeatureEnablingRule(
          bucketFrom = 10,
          bucketTo = 20,
          feature = GetHelpWithThisPageFeatureFieldHints,
          servicesLimit = Some(Set("S1", "S2"))
        )
    }

    "throw an exception when rule is incorrect" in {
      an[Exception] shouldBe thrownBy(
        FeatureEnablingRule.parse("feature=GetHelpWithThisPageFeatureFieldHints;bucketFrom=10;noToBucket")
      )
    }

  }

}
