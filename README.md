# synergy-events-stdlib

This library provides common functions for the Synergy Integration Architecture for retrieving and storing config
info, sending events to topics and so on

## Usage

set-up-topic-table [arnPrefixAtom eventStoreTopicAtom thisSSM] - set the default ARN and Event Store atoms passed in 
using values retrieved from AWS SSM. thisSSM is a Cognitect AWS library ssm instance.

set-up-route-table [routeTableAtom thisSSM thisS3] - look up S3 bucket and routing table filename from AWS SSM, retrieve
the routing table JSON, convert to a map and assign that map to the atom passed as routeTableAtom. thisSSM and thisS3 are
Cognitect AWS ssm and s3 clients

gen-status-map [status-code status-message return-value] - pass in a map of status code (true/false), a status string
e.g. "failed-validation" and an arbitrary map of status indicators; return a standard status map in format:

{:status true/false :status-message "message" :return-value {:key1 "val2" :key2 "val2"}

send-to-topic [topic event arnPrefix thisSNS note] - send the standard Synergy message format event **event** 
to the topic identified by **arnPrefix:topic** using a Cognitect AWS sns client instantiated into thisSNS. The value of
**note** is used in the log written as the message is dispatched.  

send-to-topic [topic event arnPrefix thisSNS] - as above, but with the **note** field set to ""

validate-message [inbound-message] - validate the Synergy namespaced format message passed in against the standard 
message spec. Return a status map (as above) with the result - if the message does not validate, then the return-value 
includes the output of the explain? function to explain the validation failure

check-event-type [event] - pass in an arriving event, and this function will identify it as one of the following SQS, SNS, 
S3, Cloudwatch (timer) or HTTP API (from the API gateway)

get-event-data [event src-type] - pass in the source event and the event-type as above, and this function returns the body 
content of the event passed in

## License

Copyright © 2020 Hackthorn Imagineering Ltd
