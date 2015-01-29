@tag
Feature:
  As a Motor Trader
  I want to receive an appropriate error message when I enter an invalid document reference number
  so that I can correct it and move to the next step in the transaction

  Scenario:
    Given I am on the complete and confirm page
    When  I click on confirm sale button without any validation errors
    Then  I should be taken to succesful summary page
    And   I can see the details about the vehicle registration number doc ref no date of sale and transaction id

  Scenario:
    Given I am on the succesful summary page
    And   I can see the buy another vehicle and finish button
    When  I click on buy another vehicle button
    Then  I should be taken to vehicle look up page

  Scenario:
    Given I am on the complete and confirm page with failure data
    When  I click on confirm sale button without any validation errors
    Then  I should be taken to failure  page
    And   I can see the details of transaction id with failure screen
