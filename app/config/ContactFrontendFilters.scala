package config;

import javax.inject.Inject

import play.api.http.DefaultHttpFilters
import uk.gov.hmrc.play.bootstrap.filters.FrontendFilters

class ContactFrontendFilters @Inject()(defaultFilters : FrontendFilters, myCustomFilter: CorsFilter)
  extends DefaultHttpFilters(defaultFilters.filters :+ myCustomFilter: _*)
