# Maintaining contact-frontend and deskpro-ticket-queue

## Prerequisites

* [Mongo DB](https://www.mongodb.com/)

The easiest way to run Mongo is using [docker](https://hub.docker.com/_/mongo).

* [service manager 2](https://github.com/hmrc/sm2)
    
You will need the DESKPRO_TICKET_QUEUE service running locally:

```shell script
sm2 --start DESKPRO_TICKET_QUEUE
```

## To run locally

To start the application locally with the test-only endpoints enabled,

```shell script
./run.sh
```

You should then be able to navigate to the following endpoints:

* http://localhost:9250/contact/contact-hmrc?service=foo
* http://localhost:9250/contact/report-technical-problem?service=foo
* http://localhost:9250/contact/survey?ticketId=ABCD-EFGH-ASDF&serviceId=foo
* http://localhost:9250/contact/beta-feedback?service=foo
* http://localhost:9250/contact/accessibility?service=foo

## Running all unit and integration tests together

To run the Scala unit and integration tests,

```shell script
sbt test it/test
```

## Running UI acceptance tests and accessibility tests

To execute the UI acceptance tests on your local machine, follow the guidance in the [contact-frontend-ui-tests](https://github.com/hmrc/contact-frontend-ui-tests)
repository. These will also run the accessibility tests via [ui-test-runner](https://github.com/hmrc/ui-test-runner).

## ADRs in contact-frontend

We are using MADRs to record architecturally significant decisions in this service. To find out more
visit [MADR](https://github.com/adr/madr)

See our [architectural decision log](adr/index.md) (ADL) for a list of past decisions.

### How to create a new ADR

1. Install [Node](https://nodejs.org/en/download/) if you do not have this already. Node includes
npm.

1. Install `adr-log` if you do not have this already

    ```shell script
    npm install -g adr-log
    ```

1. Copy [template.md](adr/template.md) as NNNN-title-of-decision.md, and fill
in the fields. Do not feel you have to fill in all the fields, only fill in fields
that are strictly necessary. Some decisions will merit more detail than others.

1. To re-generate the table of contents, run

    ```shell script
    ./generate-adl.sh
    ```

## Email validation in contact-frontend

### Background

Validation of email addresses on the server side within `contact-frontend` has some complexity to consider. 
Specifically, there is a known unhappy path that can occur:

1. End user submits a form via `contact-frontend`, with an email addresses that passes validation within 
`contact-frontend` on the server side controller
   
1. Ticket is created in the encrypted Mongo-backed queue within `deskpro-ticket-queue`

1. Ticket is sent asynchronously to Deskpro, which may use different email validation rules. If the ticket has been 
created with an email addresses that does not pass Deskpro's validation rules, it will be repeatedly submitted to 
Deskpro as a scheduled POST until it reaches a maximum number of retries, at which point it is marked as permanently 
failed and deleted after the TTL expires (at time of writing, 200 days).
   
The decision has therefore been taken to align `contact-frontend` with the email validation rules in Deskpro. These are 
written in [PHP Perl Compatible Regular Expressions (PCRE) syntax](https://support.deskpro.com/no/guides/admin-guide/agent-channel-setup/ticket-fields-2/field-validation-and-display#field-validation-and-display_agent-only-fields).

### Translating PCRE regexes to Scala

Most of the regex syntax used by PCRE is standard and does not need any modification to be usable in Scala. However, 
there are a few things of which to be aware.

1. In the Deskpro PCRE regexes in their [provided source code](https://support.deskpro.com/en/kb/articles/can-i-see-the-deskpro-source-code),
the `#` symbol is used as a [delimiter](https://www.php.net/manual/en/regexp.reference.delimiters.php) at the start and 
end of the regex. When converting to Scala, these can be deleted.
```
'#^[a-Z0-9]{1}#'
```
becomes
```scala
"""^[a-Z0-9]{1}""".r
```

2. The PCRE regexes use a final `i` to denote that a regex is case-insensitive. In Scala, this should be leading `(?i)`
```
'#^[hello world]{1}#i'
```
becomes
```scala
"""(?i)^[hello world]{1}""".r
```

3. PCRE regexes may escape the `#` and other special character that may be used as delimiters. These do not need to be 
escaped in Scala.
```
'#^[a-z0-9!\\#$%&\'*+/=?^_`{|}~-]+#i'
```
becomes
```scala
"""(?i)[a-z0-9!#$%&'*+/=?^_`{|}~-]+""".r
```

### Useful resources

[PCRE Manual](https://www.php.net/manual/en/book.pcre.php)

[regular expressions 101 PCRE regex checker](https://regex101.com/)
