Feature: Get help with this page

  In order to get some help about a particular service
  As a Tax Payer
  I want to be able to submit a problem report 


  Background: 
    Given I go to the 'Get help with this service' test page


  Scenario: Problem report is not visible on load
    Then I see 'Get help with this page'
    And I don't see the problem report


  Scenario: Problem report submitted successfully
    Given I click on the 'Get help with this page' link
    When I fill the report form with correct information
    And I send the report form
    Then I see:
      | Thank you |
      | Your message has been sent, and the team will get back to you within 2 working days. |
    And the Deskpro endpoint '/deskpro/ticket' has received the following request:
    """
    {}
    """

    Scenario: Signed in


  Scenario: All fields are mandatory
    When I fill the report form with empty values
    And I try to send the report form
    Then I remain on the same page
    And I see:
      | Please provide your name.                    |
      | Please provide your email address.           |
      | Please enter details of what you were doing. |
      | Please enter details of what went wrong.     |
    And the Deskpro enpoint '/deskpro/ticket' has not been hit


  Scenario: Fields have a size limit and prevent users from typing more
    Given the 'name' cannot be longer than 70 characters
    And the 'email' cannot be longer than 255 characters
    And the 'what you were doing' cannot be longer than 1000 characters
    And the 'what went wrong' cannot be longer than 1000 characters
    When I try to fill the form with values that are too long
    Then the 'name' field contains 70 characters
    And the 'email' field contains 255 characters
    And the 'what you were doing' field contains 1000 characters
    And the 'what went wrong' field contains 1000 characters


    Scenario: Invalid email address
    When I fill the form with an invalid email address
    And I try to send the feedback form
    Then I remain on the same page
    And I see:
      | Enter a valid email address |


  Scenario: Deskpro times out
    Given the call to Deskpro endpoint '/deskpro/ticket' will take 10 seconds
    When I fill the report form correctly
    And I try to send the report form
    Then I remain on the same page
    And I see:
      | .... |


  Scenario Outline: Deskpro unavailable
    Given the call to Deskpro endpoint '/deskpro/ticket' will fail with status <status>
    When I fill the report form correctly
    And I try to send the report form
    Then I remain on the same page
    And I see:
      | .... |
  Examples:
    | status |
    | 404    |
    | 500    |


