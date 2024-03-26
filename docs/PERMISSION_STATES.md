# Permission states

A permission request can have different states over its lifetime. As of the 20th of February 2024, there are 15 states
in total.

| State                                            | Description                                                                                                                                     |
|--------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------|
| CREATED                                          | This is the initial state of a permission request and is assigned when it has been created.                                                     |
| VALIDATED                                        | The permission request has been validated successfully and matches the requirements in for the given region. It will contain only valid values. |
| MALFORMED                                        | The permission request was malformed and could not be further processed. If the user tires to re-send, the request will be validated again.     |
| UNABLE_TO_SEND                                   | The permission request could not be sent to the PA. There are several reasons this can happen such as a power outage or network issue.          |
| RECEIVED_PERMISSION_ADMINISTRATOR_RESPONSE       | The permission request has been sent to the PA. Currently the process is waiting for an answer by the PA.                                       |
| PENDING_PERMISSION_ADMINISTRATOR_ACKNOWLEDGEMENT | The permission request is being sent to the permission administrator and waits for an acknowledgement.                                          |
| SENT_TO_PERMISSION_ADMINISTRATOR                 | The PA informed the process that it has received the permission request and that it is being processed currently.                               |
| CANCELLED                                        | As of the 20th of February 2024, this status is not implemented yet.                                                                            |
| TIMED_OUT                                        | A permission request can time out if the user does not interact with the permission, such as accepting/rejecting it.                            |
| ACCEPT                                           | The user accepted the permission request.                                                                                                       |
| REJECTED                                         | The user rejected the permission request.                                                                                                       |
| INVALID                                          | The permission request contains semantic error or the format is incorrect. The permission is therefore invalid.                                 |
| REVOKED                                          | The user revoked the permission via the (portal of the) PA or the PA removed the permission.                                                    |
| TERMINATED                                       | The permission has been terminated by the EP.                                                                                                   |
| FULFILLED                                        | The permission request has been fulfilled (e.g. if all data has been delivered)                                                                 |