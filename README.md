contact-frontend
================

The service allows users to contact HMRC Customer Contact team for the catalogue of reasons including:
* reporting problems and asking questions
* providing feedback about the service
* providing feedback about how the issue was solved by HMRC Customer Contact team

Contact-frontend retrieves is responsible for showing the forms listed above, validating the input and
passing user requests to downstream services - DeskPro (reporting problems, service feedback) and Splunk datastore 
(feedback about customer contact).

The service is not intended to be used standalone, rather to be integrated with other services.

# Contents
   * [Forms provided by the Customer Contact Subsystem](#forms-provided-by-the-customer-contact-subsystem)
      * [Contacting HMRC - <em>Get help with this page</em>](#contacting-hmrc---get-help-with-this-page)
      * [Contacting HMRC - <em>Help and contact</em>](#contacting-hmrc---help-and-contact)
      * [Providing feedback about Digital Customer Support Team](#providing-feedback-about-digital-customer-support-team)
      * [Providing Beta feedback about services](#providing-beta-feedback-about-services)
   * [Integration guide](#integration-guide)
      * [Cross-Origin Resource Sharing (CORS)](#cross-origin-resource-sharing-cors)
      * [Creating own customer contact forms](#creating-own-customer-contact-forms)
   * [Other relevant details](#other-relevant-details)
      * [User details attached to the ticket](#user-details-attached-to-the-ticket)
      * [Location of Javascript code used by the service](#location-of-javascript-code-used-by-the-service)
      * [Related projects, useful links](#appendix__linx)
      * [Slack](#appendix__links__slack)


# Forms provided by the Customer Contact Subsystem <a name="forms-provided-by-the-customer-contact-subsystem"></a>

## Contacting HMRC - *Get help with this page* <a name="contacting-hmrc---get-help-with-this-page"></a>


This form should be used as a standard way of allowing users to report problems and ask questions about the services.

Here is a screenshot of the form:

![alt tag](docs/get-help.png)

This contact form consists of the following fields:
- an users name
- an users email address
- an action performed by the user
- an error seen by the user

Requests of this type are forwarded to *Deskpro* with a subject *"Support Request"*. 
Contents of *action* and *error* fields is concatenated and placed in the ticket body.

UI components allowing to use the form are provided by the [play-ui](https://github.com/hmrc/play-ui) library.
Play UI library contains Twirl template that allows to render "Get help with this page" link in the footer
of the standard HMRC page, provided that you use *hmrcGovUkTemplate* template. In order to use it, you have add the following changes
in the file where you use *hmrcGovUkTemplate*:
1) Define 'get help form component'
```
@getHelpForm = @{
  uiHelpers.reportAProblemLink(
    /contact/problem_reports_ajax?service=<your-service-name-here>,
    /contact/problem_reports_nonjs?service=<your-service-name-here>
  )
} 
```

2) Use it when instantiating *hmrcGovUkTemplate*

```
@content = {
  @uiLayouts.main_content(
    article = ...,
    mainClass = ...,
    mainDataAttributes = ...,
    mainContentHeader = ...,
    serviceInfo = ...,
    getHelpForm = getHelpForm,
    sidebar = ...
  )
}
```

[[Back to the top]](#top)

## Contacting HMRC - *Help and contact* <a name="contacting-hmrc---help-and-contact"></a>

This form is very similar to *Get help with this page*.
There are minor differences in how this form works in comparison to support requests.

This contact form contains only three input fields:
- name
- email address
- comments

Requests of this type are forwarded to *Deskpro* with a subject *"Contact form submission"*

This form can be used in two ways:

This functionality can be used by services in two modes:
* standalone form
* form included in the underlying page, retrieved by partial

If you want to use standalone version of the form, you have to redirect the user to one of the following URLs:
* if user is unauthenticated - `https://www.development.tax.service.gov.uk/contact/contact-hmrc-unauthenticated?service=${serviceId}`
* if user is authenticated - `https://www.development.tax.service.gov.uk/contact/contact-hmrc?service=${serviceId}`

`Help and contact` also historically was supporting showing *Help and contact* page as a partial - however this
functionality is deprecated and should'nt be used.

[[Back to the top]](#top)

## Providing feedback about Digital Customer Support Team <a name="providing-feedback-about-digital-customer-support-team"></a>
Response emails sent by Digital Customer Support Team contain link inviting users to complete the survey about quality of service.
This link redirects user to the survey form provided by contact-frontend. Data from the survey are then stored in DeskPro.

Here is an example of email received by the user:
![alt tag](docs/support-confirmation-email.png)

Thats how DCST feedback form looks like:

![alt tag](docs/survey.png)

Feedback survey results as stored in Splunk as explicit audit events with the following properties:
* *auditSource* - "frontend"
* *auditType* - "DeskproSurvey"
* *details* 
    * *helpful* - the respose to the question about user satisfaction
    * *speed* - the response about satisfaction with the speed of DCST reply
    * *improve* - contents of the textual field with improvement suggestions
    * *ticketId* - the reference of the case (same as in email)
    * *serviceId* - an identifier of the service - same as provided in the *Get help with this page* page

This functionality is used by Deskpro and shouldn't be used by any of the service.
Emails received from Deskpro should contain link in the following format:

`https://www.tax.service.gov.uk/contact/survey?ticketId={deskproTicketKey}&serviceId={serviceId}`

This link then redirects user to the standalone page where he can fill in the survey.

[[Back to the top]](#top)

## Providing Beta feedback about services <a name="providing-beta-feedback-about-services"></a>

![alt tag](docs/beta-feedback.png)

The form consists of the following fields:
- service rating (radio button group with 5 values)
- user's name
- user's email address
- additional comments (optional)

Feedback responses are forwarded to Deskpro with subject *"Beta feedback submission"*

This functionality can be used in two modes:
* form displayed as a separate, standalone page
* form included in the underlying page, retrieved by partial and initially hidden

If you want to display this form as a standalone page, you should render such a link on your page:
* if user is unauthenticated - `https://www.development.tax.service.gov.uk/contact/beta-feedback-unauthenticated?service=${serviceId}&additinal parameters`
* if user is authenticated - `https://www.development.tax.service.gov.uk/contact/beta-feedback?service=${serviceId}&additional parameters`

Customization flags:
* *service* - consuming services should specify their identifier as a 'service' parameter of requests to contact-frontend. Value of this parameter will be later passed to Splunk and would allow to properly analyze feedback
* *canOmitComments* - consuming services can decide that 'comments' field is optional. In order to to that, consuming service have to add 'canOmitComments=true' field to the requst
* *backURL* - (only for standalone page). Beta feedback form can contain 'Back' button redirecting user back to consuming service. In order to achieve that, the consuming service has to specify destination URL.

If you want to embed feedback form on your page, you have to create endpoints in your frontend service that redirect users requests to contact-frontend and wrap HTML code
returned in a response in your services layout. Three requests need to be handled:

a) GET endpoint to show the form. This should result in making backend GET call to the endpoint 
`https://contact-frontend.public.mdtp/contact/beta-feedback/form?{params}`. Where params should consist of:
 * *submitUrl* - url that should be used by user to make POST to submit the form. This form should be handled by the endpoint described below.
 * *service* - consuming services should specify their identifier as a 'service' parameter of requests to contact-frontend. Value of this parameter will be later passed to Splunk and would allow to properly analyze feedback
 * *canOmitComments* - consuming services can decide that 'comments' field is optional. In order to to that, consuming service have to add 'canOmitComments=true' field to the requst
 * *csrfToken* - CSRF token generated from cookies of consuming service. This parameter will be added automatically by [play-partials](https://github.com/hmrc/play-partials) library and service itself shound't add it manually
This endpoint will return HTML partial than can be then embedded in the page layout.

b) POST endpoint to submit the form. This should return in making backent POST call to the endpoint
`https://contact-frontend.public.mdtp/contact/beta-feedback/form?resubmitUrl`. Where *resubmitUrl* is a public facing URL to this endpoint

In case form submission has succeeded, this endpoint returns HTTP 200 response containing identifier of the ticket
that has been created. In such case the consuming service should redirect user to the endpoint that displays confirmation page (descripted below).

In case form submission fails, this endpoint returns HTTP 400 response containing HTML snippet containing the form with errors higlighted.
This snippet has to be displayed again to the user.

c) GET endpoint to display a confirmation of the successful submission. This should result in making GET call to the endpoint
`https://contact-frontend.public.mdtp/contact/beta-feedback-confirmation` which would then return HTML partial
with confirmation message that should be decorated with the layout of consuming service.

Handling of partials can be simplified by using [play-partials](https://github.com/hmrc/play-partials) library.

Good example how to integrate feedback form with the service can be found in this repository: [business-rates-valuation-frontend](https://github.com/hmrc/business-rates-valuation-frontend)

[[Back to the top]](#top)

# Integration guide <a name="integration-guide"></a>

Below you can find brief description how to use forms provided by contact-frontend in your service

Detailed integration guide can be found on the [Confluence](https://confluence.tools.tax.service.gov.uk/display/PlatDev/Customer+Contact+Services%3A+Integration+Guide) 

[[Back to the top]](#top)

## Cross-Origin Resource Sharing (CORS) <a name="cross-origin-resource-sharing-cors"></a>

When contact forms are embedded on the service's pages, the client's browser communicates with contact-frontend using AJAX requests.
This might cause problems when the service runs on other domain that the one used by contact-frontend (which is `www.tax.service.gov.uk`). In such case user's browser will block cross-domain AJAX requests, considering them as suspicous.
If you want to use contact-frontend in the service that runs on other domain, this can be done by explicitly specifying that other domain in configuration of contact-frontend. Contact-frontend service will then use [CORS](https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS) (Cross-Origin Resource Sharing) to allow browser to accept such cross-domain requests.

To achieve that the service uses standard  [CORS Filter](https://www.playframework.com/documentation/2.5.x/CorsFilter) provided by Play Framework.
Configuration is defined in `contact-frontend.yaml` within the evenronment specific `app-config`. Here is the example configuration:
```
play.filters.cors.allowedOrigins.0: "https://ewf.companieshouse.gov.uk"
play.filters.cors.allowedOrigins.1: "https://www.qa.tax.service.gov.uk"
```

[[Back to the top]](#top)

## Creating own customer contact forms <a name="creating-own-customer-contact-forms"></a>

Currently it's not possible to customize forms in ways other than described above. If you have business
requirement to customize customer contact form, please get in touch with PlatOps team ([#team-ddcops](https://hmrcdigital.slack.com/messages/C0HUAN03S))

[[Back to the top]](#top)

# Other relevant information <a name="other-relevant-details"></a>

## User details attached to the ticket <a name="user-details-attached-to-the-ticket"></a>

In addition to the information provided by the user, the service collects the following context data:
* *HTTP Referrer header* - this should be the same as the URL of the page on which the user initiated contact journey. Deskpro uses it to classify issues.
* *HTTP UserAgent header* - this tells what browser user has
* *user tax identifiers* - if the user has been logged on, some of his tax identifiers will be attached to the tickets. These identifiers can be later seen in DeskPro
* *whether user's browser uses Javascript* 
* *user's id* - as provided in HeaderCarrier object
* *sessionId* - as provided in HeaderCarrier object
 
Here is the list of supported identifiers:

* NIN
* UTR
* VAT registration number
* PAYE reference

[[Back to the top]](#top)

## Location of Javascript code used by the service <a name="location-of-javascript-code-used-by-the-service"></a>

Part of the logic of contact-frontend (mainly related to reporting a problem) is performed by javascript code located in [assets-frontend](https://github.com/hmrc/assets-frontend/blob/master/assets/javascripts/modules/reportAProblem.js) project.

When making changes in asset-frontend service be aware that it might take long time to adopt these changes by service maintainers.

[[Back to the top]](#top)

## Related projects, useful links: <a name="appendix__linx"></a>

* [hmrc-deskpro](https://github.com/hmrc/hmrc-deskpro/) - Backend service responsible for forwarding requests from contact-frontend to DeskPro
* [contact-acceptance-tests](https://github.com/hmrc/contact-acceptance-tests/) - Acceptance tests of CCS subsystem
* [deskpro-performance-tests](https://github.com/hmrc/deskPro-performance-tests) - Performance tests of CCS subsystem combined with performance tests of DeskPro agent journey
* [deskpro-mods](https://github.com/hmrc/deskpro-mods) - Modifications of Deskpro which add a button allowing Support Team to lookup for tax identifier of a request sender 

## Slack <a name="appendix__links__slack"></a>
* [#team-plat-services](https://hmrcdigital.slack.com/messages/C705QD804/)
* [#team-ddcops](https://hmrcdigital.slack.com/messages/C0HUAN03S) - DDCOps is responsible for DeskPro maintenance

[[Back to the top]](#top)

