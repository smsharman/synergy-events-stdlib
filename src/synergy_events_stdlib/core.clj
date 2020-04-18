(ns synergy-events-stdlib.core
  (:require [synergy-specs.events :as synspec]
            [cognitect.aws.client.api :as aws]
            [clojure.data.json :as json]
            [taoensso.timbre :as timbre
             :refer [log trace debug info warn error fatal report
                     logf tracef debugf infof warnf errorf fatalf reportf
                     spy get-env]]
            [clojure.spec.alpha :as s]))

(def eventTopicParameters {
                           :arn-prefix "synergyDispatchTopicArnRoot"
                           :event-store-topic "synergyEventStoreTopic"
                           })

(defn getEventTopicParameters [thisSSM]
  "Look up values in the SSM parameter store to be later used by the routing table"
  (let [snsPrefix (get-in (aws/invoke thisSSM {:op :GetParameter
                                               :request {:Name (get eventTopicParameters :arn-prefix)}})
                          [:Parameter :Value])
        evStoreTopic (get-in (aws/invoke thisSSM {:op :GetParameter
                                                  :request {:Name (get eventTopicParameters :event-store-topic)}})
                             [:Parameter :Value])
        ]
    ;; //TODO: add error handling so if for any reason we can't get the values, this is noted
    {:snsPrefix snsPrefix :eventStoreTopic evStoreTopic}))

(defn setEventStoreTopic [parameter-map eventStoreAtom]
  "Set the eventStoreTopic atom with the required value"
  (swap! eventStoreAtom str (get parameter-map :eventStoreTopic)))

(defn setArnPrefix [parameter-map arnPrefixAtom]
  "Set the snsArnPrefix atom with the required value"
  (swap! arnPrefixAtom str (get parameter-map :snsPrefix)))

(defn gen-status-map
  "Generate a status map from the values provided"
  [status-code status-message return-value]
  (let [return-status-map {:status status-code :description status-message :return-value return-value}]
    return-status-map))

(defn set-up-topic-table [arnPrefixAtom eventStoreTopicAtom thisSSM]
  "Set up reference atoms based on items retrieved from AWS SSM"
  (reset! arnPrefixAtom "")
  (reset! eventStoreTopicAtom "")
  (info "Routing table not found - setting up (probably first run for this Lambda instance")
  (let [route-paraneters (getEventTopicParameters thisSSM)]
    (setArnPrefix route-paraneters arnPrefixAtom)
    (setEventStoreTopic route-paraneters eventStoreTopicAtom)))


(defn send-to-topic
  ([thisTopic thisEvent arnPrefix thisSNS]
   (send-to-topic thisTopic thisEvent arnPrefix thisSNS ""))
  ([topic event arnPrefix thisSNS note]
   (let [thisEventId (get event ::synspec/eventId)
         jsonEvent (json/write-str event)
         eventSNS (str arnPrefix topic)
         snsSendResult (aws/invoke thisSNS {:op :Publish :request {:TopicArn eventSNS
                                                                   :Message  jsonEvent}})]
     (if (nil? (get snsSendResult :MessageId))
       (do
         (info "Error dispatching event to topic : " topic " (" note ") : " event)
         (gen-status-map false "error-dispatching-to-topic" {:eventId thisEventId
                                                             :error snsSendResult}))
       (do
         (info "Dispatching event to topic : " eventSNS " (" note ") : " event)
         (gen-status-map true "dispatched-to-topic" {:eventId   thisEventId
                                                     :messageId (get snsSendResult :MessageId)}))))))

(defn validate-message [inbound-message]
  (if (s/valid? ::synspec/synergyEvent inbound-message)
    (gen-status-map true "valid-inbound-message" {})
    (gen-status-map false "invalid-inbound-message" (s/explain-data ::synspec/synergyEvent inbound-message))))


(defn check-event-type [event]
  "Deduce what type of AWS event triggered the Lambda, based on message field content"
  (let [checkRecordType (get event :Records)
        evCheck1 (get (first (get event :Records)) :eventSource)
        evCheck2 (get (first (get event :Records)) :EventSource)
        srcCheck (get event :source)
        routeKeyCheck (get event :routeKey)
        ]
    (cond (and (not (nil? checkRecordType)) (= evCheck1 "aws:sqs"))
          "SQS"
          (and (not (nil? checkRecordType)) (= evCheck1 "aws:s3"))
          "S3"
          (and (not (nil? checkRecordType)) (= evCheck2 "aws:sns"))
          "SNS"
          (and (nil? checkRecordType) (= srcCheck "aws.events"))
          "Cloudwatch"
          (and (nil? checkRecordType) (not (nil? routeKeyCheck)))
          "APIGateway"
          :else
          "NOTKNOWN")))

(defn get-event-data [event src-type]
  "Extract the data from an inbound message based on the event type"
  (cond (= src-type "SNS")
        (get (get (first (get event :Records)) :Sns) :Message)
        (= src-type "SQS")
        (get (first (get event :Records)) :body)
        (= src-type "S3")
        (first (get event :Records))
        (= src-type "Cloudwatch")
        event
        (= src-type "APIGateway")
        {:routeKey (get event :routeKey)
         :body (get event :body)}
        :else
        nil))



