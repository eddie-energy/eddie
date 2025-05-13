package energy.eddie.regionconnector.cds.client;

public class JsonResponses {
    // language=JSON
    public static final String OAUTH_METADATA_RESPONSE = """
            {
              "authorization_details_types_supported": [
                "string"
              ],
              "authorization_endpoint": "https://example.com/",
              "cds_client_settings_api": "https://example.com/",
              "cds_client_updates_api": "https://example.com/",
              "cds_clients_api": "https://example.com/",
              "cds_customerdata_accounts_api": "https://example.com/",
              "cds_customerdata_aggregations_api": "https://example.com/",
              "cds_customerdata_billsections_api": "https://example.com/",
              "cds_customerdata_billstatements_api": "https://example.com/",
              "cds_customerdata_eacs_api": "https://example.com/",
              "cds_customerdata_meterdevices_api": "https://example.com/",
              "cds_customerdata_servicecontracts_api": "https://example.com/",
              "cds_customerdata_servicepoints_api": "https://example.com/",
              "cds_customerdata_usagesegments_api": "https://example.com/",
              "cds_directory_api": "https://example.com/",
              "cds_grants_api": "https://example.com/",
              "cds_human_directory": "https://example.com/",
              "cds_human_registration": "https://example.com/",
              "cds_oauth_version": "string",
              "cds_registration_fields": {},
              "cds_scope_credentials_api": "https://example.com/",
              "cds_scope_descriptions": {
                "client_admin": {
                  "id": "client_admin",
                  "name": "client_admin",
                  "description": "string",
                  "authorization_details_fields": [],
                  "documentation": "https://example.com/",
                  "grant_types_supported": [
                    "client_credentials"
                  ],
                  "registration_optional": [],
                  "registration_requirements": [],
                  "response_types_supported": [],
                  "token_endpoint_auth_methods_supported": ["endpoint_method"]
                }
              },
              "cds_test_accounts": "https://example.com/",
              "code_challenge_methods_supported": [
                "string"
              ],
              "grant_types_supported": [
                "client_credentials"
              ],
              "introspection_endpoint": "https://example.com/",
              "issuer": "https://example.com/",
              "op_policy_uri": "https://example.com/",
              "op_tos_uri": "https://example.com/",
              "pushed_authorization_request_endpoint": "https://example.com/",
              "registration_endpoint": "https://example.com/",
              "response_types_supported": [
                "string"
              ],
              "revocation_endpoint": "https://example.com/",
              "scopes_supported": [
                "client_admin"
              ],
              "service_documentation": "https://example.com/",
              "token_endpoint": "https://example.com/",
              "token_endpoint_auth_methods_supported": [
                "string"
              ]
            }
            """;
    // language=JSON
    public static final String CDS_METADATA_RESPONSE = """
            {
              "cds_metadata_version": "v1",
              "cds_metadata_url": "https://example.com/",
              "created": "2025-02-04T08:38:35.846Z",
              "updated": "2025-02-04T08:38:35.846Z",
              "name": "string",
              "description": "string",
              "website": "https://example.com/",
              "documentation": "https://example.com/",
              "support": "https://example.com/",
              "capabilities": [ "coverage", "oauth" ],
              "coverage": "https://example.com/",
              "related_metadata": [ "https://example.com/" ],
              "commodity_types": [ "electricity" ],
              "infrastructure_types": [ ],
              "oauth_metadata": "https://example.com/"
            }
            """;
}
