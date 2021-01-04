/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package config;

import javax.inject.Inject
import play.api.http.DefaultHttpFilters
import play.filters.cors.CORSFilter
import play.filters.csp.CSPFilter
import uk.gov.hmrc.play.bootstrap.frontend.filters.FrontendFilters

class ContactFrontendFilters @Inject() (
  defaultFilters: FrontendFilters,
  playCORSFilter: CORSFilter,
  cspFilter: CSPFilter
) extends DefaultHttpFilters(cspFilter +: defaultFilters.filters :+ playCORSFilter: _*)
