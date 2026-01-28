#!/bin/bash

set -euo pipefail

function usage() {
  cat <<End-of-message
Please set the following URLS:
- API_URL
- AUTH_URL
- CORE_URL
- CORE_CLIENT_ID
- CORE_CLIENT_SECRET

Examples:
- Staging
export API_URL=https://api.escemo.com
export AUTH_URL=https://auth.escemo.com
export CORE_URL=https://core.escemo.com
export CORE_CLIENT_ID=cucumber
export CORE_CLIENT_SECRET=<you can use the one from the CI pipeline>

- Local
export API_URL=http://localhost:12000
export AUTH_URL=http://localhost:7777
export CORE_URL=http://localhost:4400
export CORE_CLIENT_ID=cucumber.100.0
export CORE_CLIENT_SECRET=cornichonCORNICHONcornichonCORNICHON

End-of-message
  exit -1
}

# the created project is configured for the JVM SDK tests
# but can be used for other usages
# check mandatory arguments
[ -z ${API_URL+x} ] && usage
[ -z ${AUTH_URL+x} ] && usage
[ -z ${CORE_URL+x} ] && usage
[ -z ${CORE_CLIENT_ID+x} ] && usage
[ -z ${CORE_CLIENT_SECRET+x} ] && usage

echo API_URL=$API_URL
echo AUTH_URL=$AUTH_URL
echo CORE_URL=$CORE_URL

# Optional name prefix from command line argument
ORIGINAL_ARG="${1:-}"
NAME_PREFIX="$ORIGINAL_ARG"

export RND_PROJECT_CMP="$(perl -e 'print int rand 1000000000000000, "\n";')"

# Truncate NAME_PREFIX if needed to keep final project key under 36 chars
# Format: "sv-poc-${NAME_PREFIX}-${RND_PROJECT_CMP}"
# sv-poc- = 7 chars, RND_PROJECT_CMP = 15 chars, dashes = 2 chars = 24 chars reserved
# So NAME_PREFIX can be max 36 - 24 = 12 chars
if [ -n "$NAME_PREFIX" ] && [ ${#NAME_PREFIX} -gt 12 ]; then
  echo "Warning: Argument '$ORIGINAL_ARG' (${#ORIGINAL_ARG} chars) is too long. Truncating to 12 chars..."
  NAME_PREFIX="${NAME_PREFIX:0:12}"
  echo "Truncated to: '$NAME_PREFIX'"
fi

#################################################################################################
# Create one oauth token for core

core_oauth_token_resp=`curl --insecure -s -u $CORE_CLIENT_ID:$CORE_CLIENT_SECRET $AUTH_URL/oauth/token -X POST -d "grant_type=client_credentials" -d "scope=manage_users_and_organizations manage_projects manage_oauth_clients"`
core_oauth_token=`echo $core_oauth_token_resp | jq -r .access_token`



###########################################################################
# Create User

cat > project-create-user.json << EndOfMessage
{
  "firstName": "Test$RND_PROJECT_CMP",
  "lastName": "User$RND_PROJECT_CMP",
  "email": "email.$RND_PROJECT_CMP@testing.foo",
  "password": "complexPass456",
  "language": "en"
}
EndOfMessage

user_id=`curl --insecure -s -H "Content-Type: application/json" -H "Authorization: Bearer $core_oauth_token" --data @project-create-user.json -X POST $CORE_URL/users | jq -r .id`
echo "User ID: $user_id"

###########################################################################
# Create Org

cat > project-create-org.json << EndOfMessage
{
  "name": "TestOrg$RND_PROJECT_CMP",
  "owner": {"typeId": "user", "id": "$user_id"}
}
EndOfMessage

org_id=`curl --insecure -s -H "Content-Type: application/json" -H "Authorization: Bearer $core_oauth_token" --data @project-create-org.json -X POST $CORE_URL/organizations | jq -r .id`
echo "Organisation ID: $org_id"

###########################################################################
# Create Project

prj_key="sv-poc-${NAME_PREFIX:+$NAME_PREFIX-}$RND_PROJECT_CMP"

cat > project-create-prj.json << EndOfMessage
{
  "key": "$prj_key",
  "name": "$prj_key",
  "languages": ["en", "de", "de-AT"],
  "countries": ["FR", "DK", "DE", "IT", "GB", "US"],
  "trialUntil": "2250-04",
  "owner": {"typeId": "organization", "id": "$org_id"},
  "plan": "Standard",
  "currencies": ["EUR", "USD"],
  "messagesEnabled": true,
  "forceAttributeMigrationStatus": {"attributeMigrationStatus": "Finished"},
  "variantMode": "StandaloneVariants"
}
EndOfMessage

echo "------| Creating project"
cat project-create-prj.json | jq .

prj_resp=`curl --fail-with-body --insecure -s -H "Content-Type: application/json" -H "Authorization: Bearer $core_oauth_token" --data @project-create-prj.json -X POST $CORE_URL/projects`

# Check if response contains an error
if echo "$prj_resp" | jq -e '.errors' > /dev/null 2>&1; then
  echo "Error creating project:"
  echo "$prj_resp" | jq .
  exit 1
fi

created_prj_key=`echo $prj_resp | jq -r .key`
prj_id=`echo $prj_resp | jq -r .id`

echo "Project Key: $prj_key (from response: $created_prj_key)"
echo "Project ID: $prj_id"

###########################################################################
# Create Client

cat > project-create-client.json << EndOfMessage
{
  "name": "$prj_key-client",
  "owner": {"typeId": "organization", "id": "$org_id"},
  "user": {"typeId": "user", "id": "$user_id"},
  "permissions": [
    {"key": "manage_project", "projectKey" : "$prj_key"},
    {"key": "manage_my_profile", "projectKey" : "$prj_key"},
    {"key": "manage_subscriptions", "projectKey" : "$prj_key"},
    {"key": "manage_api_clients", "projectKey" : "$prj_key"},

    {"key": "manage_orders", "projectKey" : "$prj_key"},
    {"key": "manage_my_orders", "projectKey" : "$prj_key"},
    {"key": "view_orders", "projectKey" : "$prj_key"},

    {"key": "manage_payments", "projectKey" : "$prj_key"},
    {"key": "view_payments", "projectKey" : "$prj_key"},

    {"key": "manage_types", "projectKey" : "$prj_key"},
    {"key": "view_types", "projectKey" : "$prj_key"},

    {"key": "manage_customers", "projectKey" : "$prj_key"},
    {"key": "view_customers", "projectKey" : "$prj_key"},

    {"key": "manage_products", "projectKey" : "$prj_key"},
    {"key": "view_products", "projectKey" : "$prj_key"},

    {"key": "manage_tax_categories", "projectKey" : "$prj_key"},
    {"key": "view_tax_categories", "projectKey" : "$prj_key"},

    {"key": "create_anonymous_token", "projectKey" : "$prj_key"}
  ]
}
EndOfMessage


client_resp=`curl --insecure -s -H "Content-Type: application/json" -H "Authorization: Bearer $core_oauth_token" --data @project-create-client.json -X POST $CORE_URL/confidential-clients`
client_id=`echo $client_resp | jq -r .id`
client_secret=`echo $client_resp | jq -r .secret`

###########################################################################
# Create Client for MC

cat > project-create-mc-client.json << EndOfMessage
{
  "name": "mc-client",
  "owner": {"typeId": "organization", "id": "$org_id"},
  "permissions" : [
    { "key" : "view_projects" },
    { "key" : "view_users_and_organizations" },

    { "key" : "get_permission_for_any_project", "projectPermission" : "view_project_settings" },

    { "key" : "get_permission_for_any_project", "projectPermission" : "manage_project" },

    { "key" : "get_permission_for_any_project", "projectPermission" : "manage_products" },
    { "key" : "get_permission_for_any_project", "projectPermission" : "view_products" },

    { "key" : "get_permission_for_any_project", "projectPermission" : "manage_tax_categories" },
    { "key" : "get_permission_for_any_project", "projectPermission" : "view_tax_categories" },

    { "key" : "get_permission_for_any_project", "projectPermission" : "manage_customers" },
    { "key" : "get_permission_for_any_project", "projectPermission" : "view_customers" },

    { "key" : "get_permission_for_any_project", "projectPermission" : "manage_orders" },
    { "key" : "get_permission_for_any_project", "projectPermission" : "view_orders" },

    { "key" : "get_permission_for_any_project", "projectPermission" : "manage_shopping_lists" },
    { "key" : "get_permission_for_any_project", "projectPermission" : "view_shopping_lists" },

    { "key" : "get_permission_for_any_project", "projectPermission" : "manage_types" },
    { "key" : "get_permission_for_any_project", "projectPermission" : "view_types" },

    { "key" : "get_permission_for_any_project", "projectPermission" : "manage_payments" },
    { "key" : "get_permission_for_any_project", "projectPermission" : "view_payments" }
  ]
}
EndOfMessage


mc_client_resp=`curl --insecure -s -H "Content-Type: application/json" -H "Authorization: Bearer $core_oauth_token" --data @project-create-mc-client.json -X POST $CORE_URL/confidential-clients`
mc_client_id=`echo $mc_client_resp | jq -r .id`
mc_client_secret=`echo $mc_client_resp | jq -r .secret`

###########################################################################
# Create import configuration

cat > integrationtest.properties << EndOfMessage
projectKey=$prj_key
clientId=$client_id
clientSecret=$client_secret
apiUrl=$API_URL
authUrl=$AUTH_URL
EndOfMessage

cat > sunrise-imrt-conf.json << EndOfMessage
{
  "commercetools": {
    "projectKey": "$prj_key",
    "clientId": "$client_id",
    "clientSecret": "$client_secret",
    "authUrl": "$AUTH_URL",
    "apiUrl": "$API_URL"
  },
  "jobs": [
    {
      "name": "productsDeleteJob"
    },
    {
      "name": "categoriesDeleteJob"
    },
    {
      "name": "productTypesDeleteJob"
    },
    {
      "name": "productTypeCreateJob",
      "resource": "https://raw.githubusercontent.com/sphereio/commercetools-sunrise-data/master/product-types/product-types.json"
    },
    {
      "name": "categoriesCreateJob",
      "resource": "https://raw.githubusercontent.com/sphereio/commercetools-sunrise-data/master/categories/categories.csv"
    },
    {
      "name": "productsCreateJob",
      "maxProducts": 4000,
      "resource": "https://raw.githubusercontent.com/sphereio/commercetools-sunrise-data/master/products/products.csv"
    }
  ]
}
EndOfMessage


#################################################################################################
# Create one oauth token for projects WS

oauth_token_resp=`curl --insecure -s -u $client_id:$client_secret $AUTH_URL/oauth/token -X POST -d "grant_type=client_credentials" -d "scope=manage_project:$prj_key"`
oauth_token=`echo $oauth_token_resp | jq -r .access_token`

#################################################################################################
# Create a "manage_my_projects" oauth token for core WS

my_projects_core_oauth_token_resp=`curl --insecure -s -u $CORE_CLIENT_ID:$CORE_CLIENT_SECRET $AUTH_URL/oauth/internal/token -X POST -d "grant_type=client_credentials" -d "scope=manage_my_projects manage_my_organizations user_id:$user_id"`
my_projects_core_oauth_token=`echo $my_projects_core_oauth_token_resp | jq -r .access_token`


echo "Client ID: $client_id"
echo "Client Secret: $client_secret"
echo "MC Client ID: $mc_client_id"
echo "MC Client Secret: $mc_client_secret"
echo "User email: email.$RND_PROJECT_CMP@testing.foo"
echo "User pwd: complexPass456"
echo "OAuth token: $oauth_token"
echo "Get all products: curl --insecure $API_URL/$prj_key/products -H \"Authorization: Bearer $oauth_token\""
echo "Core-WS Graphiql: $CORE_URL/graphql/graphiql?token=$core_oauth_token"
echo "Core-WS Graphiql (manage_my_projects): $CORE_URL/graphql/graphiql?token=$my_projects_core_oauth_token"
echo "Projects-WS Graphiql: $API_URL/graphql/graphiql?projectKey=$prj_key&token=$oauth_token"
echo
echo "variables for other scripts:"
echo "export API_URL=$API_URL"
echo "export TOKEN=$oauth_token"
echo "export AUTH_HEADER=\"Authorization: Bearer $oauth_token\""
echo "export PROJECT_KEY=$prj_key"
echo
echo "CTP environment variables:"
echo "export CTP_PROJECT_KEY=$prj_key"
echo "export CTP_CLIENT_ID=$client_id"
echo "export CTP_CLIENT_SECRET=$client_secret"
echo "export CTP_AUTH_URL=$AUTH_URL"
echo "export CTP_API_URL=$API_URL"
echo

# Generate Postman environment
postman_env=$(cat << EndOfMessage
{
	"id": "$(uuidgen)",
	"name": "$prj_key",
	"values": [
		{
			"key": "host",
			"value": "$API_URL",
			"type": "text",
			"enabled": true
		},
		{
			"key": "auth_url",
			"value": "$AUTH_URL",
			"type": "text",
			"enabled": true
		},
		{
			"key": "client_id",
			"value": "$client_id",
			"type": "text",
			"enabled": true
		},
		{
			"key": "client_secret",
			"value": "$client_secret",
			"type": "text",
			"enabled": true
		},
		{
			"key": "project-key",
			"value": "$prj_key",
			"type": "any",
			"enabled": true
		},
		{
			"key": "ctp_access_token",
			"value": "",
			"type": "any",
			"enabled": true
		}
	]
}
EndOfMessage
)

# Save to file if NAME_PREFIX is provided, otherwise print to stdout
if [ -n "$NAME_PREFIX" ]; then
	postman_file="${NAME_PREFIX}.postman_environment.json"
	echo "$postman_env" > "$postman_file"
	echo "Postman environment saved to: $postman_file"
else
	echo "Postman environment format:"
	echo "$postman_env"
fi
