# Removing ability to load contact forms via play-partials on the 29 September 2023

We will turn off support for loading contact forms from contact-frontend via play-partials on the 29 September 2023.
Before then you need to make sure your contact links take users directly to our hosted contact forms rather than
displaying the forms inline on your pages

## What comms have you sent?

* We've updated the platform changes table in Jira
* We've raised this in the fornightly platform and live services catch-up
* We've reached out directly to some teams with heavy usage of this integration
* We've published a techspace blog post that we've shared in the announcements channel of the HMRC Digital Slack

## What are play-partials?

play-partials is a library for loading fragments of HTML from other services to embed within your service.

# Why are you removing support for loading contact forms via play-partials?

Relatively few services are still loading contact forms inline, because we don’t provide a way to do it using the
play-frontend-hmrc component library. The inline forms have been deprecated for some years, and are listed as deprecated
in the documentation for contact-frontend.

Continuing to support inline forms makes it harder for PlatUI to maintain and makes changes to the forms themselves -
and gives an inconsistent experience for tax users

In reviewing the support for this, PlatUI also think it’s likely some services are loading the contact forms with every
page request (regardless of whether the user has clicked the contact link), so even though it’s used by a relatively
small number of services, the requests are making up a large proportion of the traffic to the contact-frontend service.

# How can I tell if my service is loading contact forms via play-partials?

You can rule out your service if you have no dependency on play-partials, play-ui, and assets-frontend. But by itself
having these dependencies does not mean you’re loading the contact forms via the deprecated mechanism that we’re
removing.

From the pages of your service, loading contact forms via play-partials would be evident because when you click on a
link to a contact form - like the “Is this page not working properly?” link at the bottom of every page, the contact
form would be shown within your services page (often below the link)

Instead, what you would see if your service was linking to the contact forms directly (which is the desired behaviour),
is that after clicking the link your browser takes you to the following page:

https://www.tax.service.gov.uk/contact/report-technical-problem

> **Note**
> we’ve omitted some of the query parameters you will see on the end of the link for clarity. If you’re using
> play-frontend-hmrc’s HmrcReportTechnicalIssueHelper component then we construct the correct link and with all the url
> parameters for you automatically. If not, you can see the details of the url parameters you should provide when linking
> a user to a contact form in the documentation for contact-frontend.)

# What do I need to do before September if my service is loading contact forms via play-partials?

It should only require small changes to your service’s layout template, you will already have links to our contact forms
wherever you’re using them, and we’re asking you to change them so those links take users directly to our hosted contact
forms.

If you’re using our play-frontend-hmrc component library, we have the HmrcReportTechnicalIssueHelper component that
constructs the “Is this page not working properly?” link for you with the correct url parameters so that tickets sent by
the form are correctly associated with your service and the page the user has reported the issue from.

If you’re not able to use the helper from play-frontend-hmrc then you can construct the links yourself by referring to
the documentation for contact-frontend and the implementation of the components in play-frontend-hmrc.

> **Warning**
> When you’re making changes like this, make sure you check with the Digital Technical Support Team (DTST, who receive
> all tickets) that, following any changes you make, they are able to see tickets created correctly for your service in
> Deskpro in the staging environment.

# What will happen in September?

At the beginning of September PlatUI will disable support in the non-production environments, to give services a chance
to catch any unexpected issues.

We will monitor traffic to contact-frontend and reach out to any services still requesting partial contact forms before
we disable support in production on the 29th at the end of the month.

# How can I get help?

If you have any more questions, or want to receive updates about this, we’ve created the channel in the HMRC Digital
Slack #event-deprecate-partial-contact-forms
