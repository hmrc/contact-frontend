Feature: Feedback about the beta

  In order to make my views known about the beta
  As a Tax Payer
  I want to leave my feedback


  Background:
    Given I go to the 'Feedback' page


  Scenario: Submit feedback successfully
    When I fill the feedback form correctly
    And I send the feedback form
    Then I see:
      | Thank you                        |
      | Your feedback has been received. |
    And the Deskpro endpoint '/deskpro/ticket' has received the following payload:
    """
    {}
    """

    Scenario: Feedback form sent successfully when signed in



  Scenario: All fields are mandatory
    When I fill the form with empty values
    And I try to send the feedback form
    Then I am on the 'Feedback' page
    And I see:
      | Tell us what you think of the service. |
      | Please provide your name.              |
      | Enter a valid email address.           |
      | Enter your comments.                   |
    And the Deskpro enpoint '/deskpro/ticket' has not been hit


  Scenario: Fields have a size limit
    Given the 'name' cannot be longer than 70 characters
    And the 'email' cannot be longer than 255 characters
    And the 'comment' cannot be longer than 2000 characters
    When I fill the form with values that are too long
    And I try to send the feedback form
    Then I am on the 'Feedback' page
    And I see:
      | The email cannot be longer than 255 characters    |
      | Your name cannot be longer than 70 characters     |
      | The comment cannot be longer than 2000 characters |


  Scenario: Invalid email address
    When I fill the form with an invalid email address
    And I try to send the feedback form
    Then I am on the 'Feedback' page
    And I see:
      | Enter a valid email address |


  Scenario: Deskpro times out
    Given the call to Deskpro endpoint '/deskpro/ticket' will take 10 seconds
    When I fill the feedback form correctly
    And I try to send the feedback form
    Then I am on the 'Feedback' page
    And I see:
      | .... |


  Scenario Outline: Deskpro unavailable
    Given the call to Deskpro endpoint '/deskpro/ticket' will fail with status <status>
    When I fill the contact form correctly
    And I try to send the feedback form
    Then I am on the 'Feedback' page
    And I see:
      | .... |
  Examples:
    | status |
    | 404    |
    | 500    |

