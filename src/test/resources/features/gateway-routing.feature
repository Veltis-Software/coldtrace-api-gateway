@gateway
Feature: Single entry point that routes each request to its owner microservice
  As the web application
  I want one public entry point
  So that each bounded context answers from its own service without exposing internal contracts

  @US04 @US16 @US20 @US24 @US48 @US22
  Scenario Outline: A public route reaches its owner and preserves the session and correlation headers
    When the web application requests "<path>" with a bearer token and correlation id "route-verification"
    Then the request is answered by the "<owner>" service with the same path, token and correlation id

    Examples:
      | path                                                           | owner        |
      | /api/v1/telemetry/batches                                      | monitoring   |
      | /api/v1/assets/states                                          | monitoring   |
      | /api/v1/session/context                                        | identity     |
      | /api/v1/organization-sign-ups                                  | identity     |
      | /api/v1/organizations/1/users                                  | identity     |
      | /api/v1/organizations/1/assets                                 | asset        |
      | /api/v1/alerts/1/acknowledgement                               | alert        |
      | /api/v1/incidents/1/corrective-actions                         | alert        |
      | /api/v1/organizations/1/incidents/2/ai-resolution-plans/3/approvals | alert   |
      | /api/v1/reports/1/result                                       | report       |
      | /api/v1/organizations/1/maintenance-schedules                  | maintenance  |
      | /api/v1/organizations/1/subscription                           | subscription |
      | /api/v1/organizations/1/dashboard/ai-interpretation            | ai           |

  Scenario Outline: Internal and unknown paths have no public destination
    When an anonymous client requests "<path>"
    Then the gateway answers 404

    Examples:
      | path                                      |
      | /internal/ai/v1/generations/incident-plan |
      | /not-a-public-api                         |
