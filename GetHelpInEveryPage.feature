Feature: Get Help on this page present in many pages

  In order to get some help about a particular service
  As a Tax Payer
  I want to be able to submit a problem report on each page


  Scenario: Pages contain a 'Get help with this page' section
    Given a GET call to '/get-help-with-this-page' returns:
    """
    STUB
    """
    When I go to the 'Feedback' page
    Then I see the 'Get help with this page' section containing:
     | STUB |


  Scenario: No section is shown if the call to 'Get help with this page' section times out
    Given a GET call to '/get-help-with-this-page' returns after 1 second:
    """
    STUB
    """
    When I go to the 'Feedback' page
    Then I don't see the 'Get help with this page' section


  Scenario Outline: No section is shown if the call to load 'Get help with this page' section fails
    Given a GET call to '/get-help-with-this-page' returns status <status>
    When I go to the 'Feedback' page
    Then I don't see the 'Get help with this page' section
  Examples:
    | status |
    | ...    |
    | 404    |
    | 500    |