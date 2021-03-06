@working
Feature: Disposal to Trade - transaction failure codes and messages
  As a
  motor trader
  I want to know if my on-line transaction is successful or if it will need further investigation
  So that I can inform the customer of when to expect confirmation of disposal

  Scenario:
    Given details are entered that correspond to a vehicle that has a valid clean record and has no markers or error codes
    When this is submitted along with any other mandatory information
    Then the next step in the dispose transaction "Summary" is shown
    And a message is displayed "a letter will be sent to the name and address on the V5C registration certificate (logbook) within 4 weeks."

  Scenario:
    Given details are entered that correspond to a vehicle that has a valid record but does have markers or error codes
    When this is submitted along with any other mandatory information
    Then the next step in the dispose transaction "Buying a vehicle into trade: failure" is shown
    And a message is displayed "We are unable to process the transaction at this time. Please send the V5C/3 to the following address"
