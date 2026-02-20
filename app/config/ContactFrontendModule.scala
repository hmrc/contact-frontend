/*
 * Copyright 2023 HM Revenue & Customs
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

package config

import play.api.inject.*
import play.api.mvc.RequestHeader
import play.api.{Configuration, Environment}
import uk.gov.hmrc.hmrcfrontend.config.ServiceNavCanBeControlledByRequestAttr.UseServiceNav
import util.{BackUrlValidator, ConfigurationBasedBackUrlValidator}
import uk.gov.hmrc.hmrcfrontend.config.{ServiceNavCanBeControlledByRequestAttr, ServiceNavigationConfig}

class ServiceNavUsageControlledByAllowList extends ServiceNavigationConfig {
  private val inListOfServicesKnownToBeUsingServiceNav = Set( // would come from config
    "example-a",
    "example-b"
  )

  override def forceServiceNavigation(implicit request: RequestHeader): Boolean = {
    request.attrs.get(UseServiceNav).getOrElse( // to show how it can work, I forced /contact/accessibility to use service nav
      request.getQueryString("service")
        .exists(inListOfServicesKnownToBeUsingServiceNav)
    )
  }
}

class ContactFrontendModule extends Module {
  override def bindings(environment: Environment, configuration: Configuration) =
    Seq(
      bind[AppConfig].to[CFConfig],
      bind[BackUrlValidator].to[ConfigurationBasedBackUrlValidator],
      bind[ServiceNavigationConfig].to[ServiceNavUsageControlledByAllowList]
    )
}
