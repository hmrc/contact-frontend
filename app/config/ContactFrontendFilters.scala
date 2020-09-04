/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package config;

import javax.inject.Inject
import play.api.http.DefaultHttpFilters
import play.filters.cors.CORSFilter
import uk.gov.hmrc.play.bootstrap.frontend.filters.FrontendFilters

class ContactFrontendFilters @Inject()(defaultFilters: FrontendFilters, playCORSFilter: CORSFilter)
    extends DefaultHttpFilters(defaultFilters.filters :+ playCORSFilter: _*)
