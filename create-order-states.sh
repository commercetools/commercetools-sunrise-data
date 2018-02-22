#!/bin/bash

# prerequisites:
# brew install jq  (a json commandline processor)

set -e

PROJECT_KEY=""
ACCESS_TOKEN=""

# Don't touch from HERE
ENVIRONMENT="api.sphere.io"
STATE_TYPE="LineItemState"

create_state() {
  local key=$1
  local name_en=$2
  local name_de=$3
  local initial="false"
  if [ -n "$4" ]; then
    initial="true"
  fi
  local data="{\"key\":\"${key}\",\"type\":\"${STATE_TYPE}\",\"initial\":${initial},\"name\":{\"en\":\"${name_en}\",\"de\":\"${name_de}\"}}"
  _RET=$(curl -k -s -X POST -H "Authorization: Bearer ${ACCESS_TOKEN}" --data "${data}" "https://${ENVIRONMENT}/${PROJECT_KEY}/states" | jq -r .id)
}

add_transition() {
  local filename="/tmp/state.json"
  local fromId=$1
  local toId=$2

  curl -k -s -H "Authorization: Bearer ${ACCESS_TOKEN}" "https://${ENVIRONMENT}/${PROJECT_KEY}/states/${fromId}" >${filename}
  local version=$(cat ${filename} | jq -r .version )

  set +e
  grep -q .transitions /tmp/state.json
  local has_transitions=$?
  set -e

  echo "{\"version\":${version},\"actions\":[{\"action\":\"setTransitions\",\"transitions\":[" >/tmp/action.json
  echo "{\"typeId\":\"state\",\"id\":\"${toId}\"}" >>/tmp/action.json
  if [ "${has_transitions}" = "0" ]; then
    echo "," >>/tmp/action.json
  fi
  cat ${filename} | jq .transitions | tail -n +2 >>/tmp/action.json
  if [ "${has_transitions}" = "1" ]; then
    echo "]" >>/tmp/action.json
  fi
  echo "}]}" >>/tmp/action.json

  # cat /tmp/action.json
  curl -k -s -X POST -H "Authorization: Bearer ${ACCESS_TOKEN}" --data @/tmp/action.json "https://${ENVIRONMENT}/${PROJECT_KEY}/states/${fromId}"
}

### to here ;)

create_state "readyToShip" "Ready to Ship" "Versandfertig"
READY_TO_SHIP_ID=${_RET}

create_state "backorder" "In replenishment" "Wird nachbestellt"
BACKORDER_ID=${_RET}

create_state "shipped" "Shipped" "Versandt"
SHIPPED_ID=${_RET}

create_state "canceled" "Canceled" "Storniert"
CANCELED_ID=${_RET}

create_state "picking" "Picking" "Picking"
PICKING_ID=${_RET}

create_state "returned" "Returned" "Retourniert"
RETURNED_ID=${_RET}

create_state "returnApproved" "Return approved" "Retoure akzeptiert"
RETURN_APPROVED_ID=${_RET}

create_state "returnNotApproved" "Return not approved" "Retoure nicht akzeptiert"
RETURN_NOT_APPROVED_ID=${_RET}

create_state "closed" "Closed" "Abgeschlossen"
CLOSED_ID=${_RET}

create_state "lost" "Lost" "Verloren gegangen"
LOST_ID=${_RET}

create_state "lossApproved" "Loss Approved" "Verlust bestätigt"
LOSS_APPROVED_ID=${_RET}

create_state "lossNotApproved" "Loss not Approved" "Wieder gefunden"
LOSS_NOT_APPROVED_ID=${_RET}

INITIAL_ID=$(curl -k -s -H "Authorization: Bearer ${ACCESS_TOKEN}" "https://${ENVIRONMENT}/${PROJECT_KEY}/states?where=key%3D%22Initial%22" | jq -r .results[0].id)
add_transition "${INITIAL_ID}" "${PICKING_ID}"
add_transition "${INITIAL_ID}" "${BACKORDER_ID}"

add_transition "${PICKING_ID}" "${READY_TO_SHIP_ID}"
add_transition "${PICKING_ID}" "${BACKORDER_ID}"

add_transition "${BACKORDER_ID}" "${CANCELED_ID}"
add_transition "${BACKORDER_ID}" "${PICKING_ID}"

add_transition "${READY_TO_SHIP_ID}" "${SHIPPED_ID}"

add_transition "${SHIPPED_ID}" "${RETURNED_ID}"
add_transition "${SHIPPED_ID}" "${LOST_ID}"
add_transition "${SHIPPED_ID}" "${CLOSED_ID}"

add_transition "${RETURNED_ID}" "${RETURN_APPROVED_ID}"
add_transition "${RETURNED_ID}" "${RETURN_NOT_APPROVED_ID}"

add_transition "${RETURN_APPROVED_ID}" "${CLOSED_ID}"

add_transition "${LOST_ID}" "${LOSS_APPROVED_ID}"
add_transition "${LOST_ID}" "${LOSS_NOT_APPROVED_ID}"

add_transition "${LOSS_APPROVED_ID}" "${CLOSED_ID}"

add_transition "${LOSS_NOT_APPROVED_ID}" "${CLOSED_ID}"

echo "##### The workflow:"
curl -k -s -H "Authorization: Bearer ${ACCESS_TOKEN}" "https://${ENVIRONMENT}/${PROJECT_KEY}/states" | jq .
