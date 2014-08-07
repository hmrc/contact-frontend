Feature: Get help with this page without Javascript

  In order to get some help about a particular service
  As a Tax Payer
  I want to be able to submit a problem report 


  Background: 
    Given my browser has JavaScript disabled
    And I go to the 'Get help with this service' test page


  Scenario: Problem report is visible on load
    Then I see 'Get help with this page'
    And I see the problem report


  Scenario: Problem report submitted successfully
    When I fill the report form with correct information
    And I send the report form
    Then I am on the 'Thank you? (same behaviour as now)' page
    And I see:
      | Thank you |
      | Your message has been sent, and the team will get back to you within 2 working days. |
    And the Deskpro endpoint '/deskpro/ticket' has received the following request:
    """
    {} <- javascript off
    """


    Scenario: Deskpro is not available (go back to where you were?)