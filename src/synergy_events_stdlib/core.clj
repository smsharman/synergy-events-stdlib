(ns synergy-events-stdlib.core
  (:require [synergy-specs.events :as synspec]
            [cognitect.aws.client.api :as aws]
            [cheshire.core :as json]
            [taoensso.timbre
             :refer [log trace debug info warn error fatal report
                     logf tracef debugf infof warnf errorf fatalf reportf
                     spy get-env]]
            [clojure.spec.alpha :as s]
            [clojure.java.io :as io]))

(def eventTopicParameters {
                           :arn-prefix "synergyDispatchTopicArnRoot"
                           :event-store-topic "synergyEventStoreTopic"
                           })

(def routeTableParameters {
                           :bucket "synergyDispatchBucket"
                           :filename "synergyDispatchRoutingTable"
                           })

(defn- invoke-aws-get-parameter-value [thisSSM table parameter]
  (let [aws-response (aws/invoke thisSSM {:op      :GetParameter
                                          :request {:Name (get table parameter)}})]
    (get-in aws-response [:Parameter :Value]))
  )

(defn- get-route-table-parameters-from-SSM
  "Look up values in the SSM parameter store to be later used by the routing table"
  [thisSSM]
  (let [tableBucket (invoke-aws-get-parameter-value thisSSM routeTableParameters :bucket)
        tableFilename (invoke-aws-get-parameter-value thisSSM routeTableParameters :filename)]
    ;; //TODO: add error handling so if for any reason we can't get the values, this is noted
    {:tableBucket tableBucket :tableFilename tableFilename}))

(defn- load-route-table
  "Load routing table from S3, input is a map with :tableBucket :tableFilename and :snsPrefix"
  [parameter-map thisRouteTable thisS3]
  ;; Get file from S3 and turn it into a map
  (let [tableBucket (get parameter-map :tableBucket)
        tableFilename (get parameter-map :tableFilename)
        rawRouteMap (json/parse-stream (io/reader
                                 (get (aws/invoke thisS3 {:op :GetObject
                                                      :request {:Bucket tableBucket :Key tableFilename}}) :Body)) true)]
    (swap! thisRouteTable merge @thisRouteTable rawRouteMap)))

(defn- get-event-topic-parameters
  "Look up values in the SSM parameter store to be later used by the routing table"
  [thisSSM]
  (let [snsPrefix (invoke-aws-get-parameter-value thisSSM eventTopicParameters :arn-prefix)
        evStoreTopic (invoke-aws-get-parameter-value thisSSM eventTopicParameters :event-store-topic)]
    ;; //TODO: add error handling so if for any reason we can't get the values, this is noted
    {:snsPrefix snsPrefix :eventStoreTopic evStoreTopic}))

(defn- set-event-store-topic
  "Set the eventStoreTopic atom with the required value"
  [parameter-map eventStoreAtom]
  (swap! eventStoreAtom str (get parameter-map :eventStoreTopic)))

(defn- set-arn-prefix
  "Set the snsArnPrefix atom with the required value"
  [parameter-map arnPrefixAtom]
  (swap! arnPrefixAtom str (get parameter-map :snsPrefix)))

(defn generate-lambda-return
  "Generate a simple Lambda status return"
  [statuscode message]
  {:status statuscode :message message})

(defn gen-status-map
  "Generate a status map from the values provided"
  [status-code status-message return-value]
   {:status status-code :description status-message :return-value return-value})

(defn set-up-topic-table
  "Set up reference atoms based on items retrieved from AWS SSM"
  [arnPrefixAtom eventStoreTopicAtom thisSSM]
  (reset! arnPrefixAtom "")
  (reset! eventStoreTopicAtom "")
  (info "Topic atoms not found - setting up (probably first run for this Lambda instance")
  (let [route-parameters (get-event-topic-parameters thisSSM)]
    (set-arn-prefix route-parameters arnPrefixAtom)
    (set-event-store-topic route-parameters eventStoreTopicAtom)))

(defn set-up-route-table [routeTableAtom thisSSM thisS3]
  (reset! routeTableAtom {})
  (info "Routing table not found - setting up (probably first run for this Lambda instance")
  (let [route-parameters (get-route-table-parameters-from-SSM thisSSM)]
    (load-route-table route-parameters routeTableAtom thisS3)))


(defn send-to-topic
  ([topic event arnPrefix thisSNS]
   (send-to-topic topic event arnPrefix thisSNS ""))
  ([topic event arnPrefix thisSNS note]
   (let [thisEventId (get event ::synspec/eventId)
         jsonEvent (json/generate-string event {:pretty true})
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


(defn check-event-type
  "Deduce what type of AWS event triggered the Lambda, based on message field content"
  [event]
  (let [checkRecordType (get event :Records)
        evCheck1 (-> event (get :Records) first (get :eventSource))
        evCheck2 (-> event (get :Records) first (get :EventSource))
        srcCheck (get event :source)
        routeKeyCheck (get event :routeKey)]
    (if (not (nil? checkRecordType))
      (cond (= evCheck1 "aws:sqs")
            :sqs
            (= evCheck1 "aws:s3")
            :s3
            (= evCheck2 "aws:sns")
            :sns
            (= srcCheck "aws.events")
            :cloudwatch
            (not (nil? routeKeyCheck))
            :api-gateway
            :else
            :not-known
            )
      :not-known)))

(defn get-event-data
  "Extract the data from an inbound message based on the event type"
  [event src-type]
  (case src-type
    :sns (-> event (get :Records) first (get-in [:Sns :Message]))
    :sqs (-> event (get :Records) first (get :body))
    :s3 (-> event (get :Records) first)
    :cloudwatch event
    :api-gateway {:routeKey (get event :routeKey)
                  :body (get event :body)}
    nil
    ))



