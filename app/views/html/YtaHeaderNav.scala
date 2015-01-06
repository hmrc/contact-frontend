package views.html

import views.helpers.HeaderNav

// this is a copy of the same file from BT_ACCOUNT frontend. It should either kept in sync or re-thought what kind of link we need in this app
object YtaHeaderNav {

  def apply() = HeaderNav(
    title = Some("Your tax account"),
    showBetaLink = false,
    links = Some(views.html.yta_header_nav_links())
  )
}
