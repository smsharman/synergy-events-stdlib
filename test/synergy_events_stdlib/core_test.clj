(ns synergy-events-stdlib.core-test
  (:require [clojure.test :refer :all]
            [synergy-events-stdlib.core :refer :all]
            [cognitect.aws.client.api :as aws]
            [clojure.data.json :as json]))

(def ns-valid-message {
                       :synergy-specs.events/eventId "7a5a815b-2e52-4d40-bec8-c3fc06edeb36"
                       :synergy-specs.events/parentId "7a5a815b-2e52-4d40-bec8-c3fc06edeb36"
                       :synergy-specs.events/originId "7a5a815b-2e52-4d40-bec8-c3fc06edeb36"
                       :synergy-specs.events/userId "1"
                       :synergy-specs.events/orgId "1"
                       :synergy-specs.events/eventVersion 1
                       :synergy-specs.events/eventAction "event1"
                       :synergy-specs.events/eventData {
                                                        :key1 "value1"
                                                        :key2 "value2"
                                                        }
                       :synergy-specs.events/eventTimestamp "2020-04-17T11:23:10.904Z"
                       })

(def ns-invalid-message-no-origin-id {
                       :synergy-specs.events/eventId "7a5a815b-2e52-4d40-bec8-c3fc06edeb36"
                       :synergy-specs.events/parentId "7a5a815b-2e52-4d40-bec8-c3fc06edeb36"
                       :synergy-specs.events/userId "1"
                       :synergy-specs.events/orgId "1"
                       :synergy-specs.events/eventVersion 1
                       :synergy-specs.events/eventAction "event1"
                       :synergy-specs.events/eventData {
                                                        :key1 "value1"
                                                        :key2 "value2"
                                                        }
                       :synergy-specs.events/eventTimestamp "2020-04-17T11:23:10.904Z"
                       })

(deftest valid-validate-method
  (let [valid-message (validate-message ns-valid-message)]
    (is (true? (get valid-message :status)))
    (is (= "valid-inbound-message" (get valid-message :description)))))

(deftest invalid-validate-method
  (let [invalid-message (validate-message ns-invalid-message-no-origin-id)]
    (is (false? (get invalid-message :status)))
    (is (= "invalid-inbound-message" (get invalid-message :description)))))

(def sns-test-message {:Records [{:EventSource "aws:sns",
                                  :EventSubscriptionArn "arn:aws:sns:eu-west-1:123456789012:sns-lambda:21be56ed-a058-49f5-8c98-aedd2564c486",
                                  :EventVersion "1.0",
                                  :Sns {:UnsubscribeURL "https://sns.eu-west-1.amazonaws.com/?Action=Unsubscribe&SubscriptionArn=arn:aws:sns:eu-west-1:979590819078:testNormaliserTopic:e4d45966-e432-450a-ae16-fc21e7cf7393",
                                        :Signature "dxSKIozdhw4fjPmraXnBL+DDEHWuOQmfRixnNgU2+2noOV3WwuhbXVa+0860ylkssBZp5RumyV1m886C5mqZdBeQEIMcPS65m4RzbLv66MG1HSPzTP479yWo4oZ0YsBq4SS89tM0Xg9h/5nRNEp/vKr0xLS70bJgM4coxU8oBRACAKjZxnHcuJPKTCeVccC288xqdHLfFxFwOcmI9rDoKg7GX2rY7BEv09O4LoA0cF3UpsNsGlCP/nBwBBvHKrh8yCQ9bOgR0VbfpS0YoRyMX3RGk2aUR0pVnZiAASbhZBYozyatVSdEzjV6C1afer8/IpCh9o8fvk99B4udkzKjNw==",
                                        :Message {:orgId "1",
                                                  :eventAction "event1",
                                                  :eventId "7a5a815b-2e52-4d40-bec8-c3fc06edeb36",
                                                  :eventVersion 1,
                                                  :eventTimestamp "2020-04-17T11:23:10.904Z",
                                                  :userId "1",
                                                  :eventData {:key1 "value1", :key2 "value2"},
                                                  :parentId "7a5a815b-2e52-4d40-bec8-c3fc06edeb36",
                                                  :originId "7a5a815b-2e52-4d40-bec8-c3fc06edeb36"},
                                        :TopicArn "arn:aws:sns:eu-west-1:979590819078:testNormaliserTopic",
                                        :SignatureVersion "1",
                                        :MessageId "27918845-4417-58a5-b00e-d1c641ca02f9",
                                        :SigningCertURL "https://sns.eu-west-1.amazonaws.com/SimpleNotificationService-010a507c1833636cd94bdb98bd93083a.pem",
                                        :Type "Notification",
                                        :Timestamp "2021-08-03T08:51:41.699Z",
                                        :MessageAttributes {:Test {:Type "String", :Value "TestString"},
                                                            :TestBinary {:Type "Binary", :Value "TestBinary"}}}}]})

(def sqs-test-message {:Records [
                                 {
                                  :messageId "059f36b4-87a3-44ab-83d2-661975830a7d",
                                  :receiptHandle "AQEBwJnKyrHigUMZj6rYigCgxlaS3SLy0a...",
                                  :body {:orgId "1",
                                         :eventAction "event1",
                                         :eventId "7a5a815b-2e52-4d40-bec8-c3fc06edeb36",
                                         :eventVersion 1,
                                         :eventTimestamp "2020-04-17T11:23:10.904Z",
                                         :userId "1",
                                         :eventData {:key1 "value1", :key2 "value2"},
                                         :parentId "7a5a815b-2e52-4d40-bec8-c3fc06edeb36",
                                         :originId "7a5a815b-2e52-4d40-bec8-c3fc06edeb36"},
                                  :eventSource "aws:sqs",
                                  :awsRegion "us-east-2"
                                  }
                                 ]
                       })

(def s3-test-message {:Records
                      [{:eventName         "ObjectCreated:Put",
                        :awsRegion         "us-east-2",
                        :responseElements  {:x-amz-request-id "D82B88E5F771F645",
                                            :x-amz-id-2       "vlR7PnpV2Ce81l0PRw6jlUpck7Jo5ZsQjryTjKlc5aLWGVHPZLj5NeC6qMa0emYBDXOo6QBU0Wo="},
                        :requestParameters {:sourceIPAddress "205.255.255.255"},
                        :userIdentity      {:principalId "AWS:AIDAINPONIXQXHT3IKHL2"},
                        :eventVersion      2.1, :eventTime "2019-09-03T19:37:27.192Z",
                        :eventSource       "aws:s3",
                        :s3                {:s3SchemaVersion 1.0, :configurationId "828aa6fc-f7b5-4305-8584-487c791949c1",
                                            :bucket          {:name "lambda-artifacts-deafc19498e3f2df", :ownerIdentity {:principalId "A3I5XTEXAMAI3E"}, :arn "arn:aws:s3:::lambda-artifacts-deafc19498e3f2df"}, :object {:key "b21b84d653bb07b05b1e6b33684dc11b", :size 1305107, :eTag "b21b84d653bb07b05b1e6b33684dc11b", :sequencer "0C0F6F405D6ED209E1"}}}]}
  )

(deftest check-event-type-test
 (is (= :sns (check-event-type sns-test-message)))
 (is (= :sqs (check-event-type sqs-test-message)))
 (is (= :s3 (check-event-type s3-test-message)))
 (is (= :not-known (check-event-type {})))
 (is (= :not-known (check-event-type {:Records {}})))
 )

(def sns-event-data {:orgId          "1",
                    :eventAction    "event1",
                    :eventId        "7a5a815b-2e52-4d40-bec8-c3fc06edeb36",
                    :eventVersion   1,
                    :eventTimestamp "2020-04-17T11:23:10.904Z",
                    :userId         "1",
                    :eventData      {:key1 "value1", :key2 "value2"},
                    :parentId       "7a5a815b-2e52-4d40-bec8-c3fc06edeb36",
                    :originId       "7a5a815b-2e52-4d40-bec8-c3fc06edeb36"})

(def s3-event-data {:eventName         "ObjectCreated:Put",
                    :awsRegion         "us-east-2",
                    :responseElements  {:x-amz-request-id "D82B88E5F771F645",
                                        :x-amz-id-2       "vlR7PnpV2Ce81l0PRw6jlUpck7Jo5ZsQjryTjKlc5aLWGVHPZLj5NeC6qMa0emYBDXOo6QBU0Wo="},
                    :requestParameters {:sourceIPAddress "205.255.255.255"},
                    :userIdentity      {:principalId "AWS:AIDAINPONIXQXHT3IKHL2"},
                    :eventVersion      2.1, :eventTime "2019-09-03T19:37:27.192Z",
                    :eventSource       "aws:s3",
                    :s3                {:s3SchemaVersion 1.0,
                                        :configurationId "828aa6fc-f7b5-4305-8584-487c791949c1",
                                        :bucket          {:name "lambda-artifacts-deafc19498e3f2df",
                                                          :ownerIdentity {:principalId "A3I5XTEXAMAI3E"},
                                                          :arn "arn:aws:s3:::lambda-artifacts-deafc19498e3f2df"},
                                        :object {:key "b21b84d653bb07b05b1e6b33684dc11b",
                                                 :size 1305107,
                                                 :eTag "b21b84d653bb07b05b1e6b33684dc11b",
                                                 :sequencer "0C0F6F405D6ED209E1"}}})

(deftest get-event-data-test
 (is (= sns-event-data (get-event-data sns-test-message :sns)))
 (is (= sns-event-data (get-event-data sqs-test-message :sqs)))
 (is (= s3-event-data (get-event-data s3-test-message :s3)))
 )

(def status-map {:status false, :description " error-dispatching-to-topic ", :return-value {:eventId 1234, :error " snsSendResult "}})

(deftest gen-status-map-test
 (is (= status-map (#'synergy-events-stdlib.core/gen-status-map false " error-dispatching-to-topic " {:eventId 1234
                                                                                                    :error   " snsSendResult "}))))
(def lambda-return {:status 200, :message " All's well that ends well. "})

(deftest generate-lambda-return-test
 (is (= lambda-return (generate-lambda-return 200 " All's well that ends well. "))))

(comment
 (def ssm (aws/client {:api :ssm}))

 (#'synergy-events-stdlib.core/get-event-topic-parameters ssm)

 (#'synergy-events-stdlib.core/get-route-table-parameters-from-SSM ssm))

(comment
 (let [event (json/read-str " {\n \"eventVersion \": \"1.05 \", \n \"userIdentity \": {\n \"type \": \"IAMUser \", \n \"principalId \": \"AKIAIOSFODNN7EXAMPLE \", \n \"arn \": \"arn:aws:iam::123456789012:user/Mary_Major \", \n \"accountId \": \"123456789012 \", \n \"userName \": \"Mary_Major \" \n}, \n \"eventTime \": \"2019-06-10T17:14:09Z \", \n \"eventSource \": \"signin.amazonaws.com \", \n \"eventName \": \"ConsoleLogin \", \n \"awsRegion \": \"us-east-1 \", \n \"sourceIPAddress \": \"203.0.113.67 \", \n \"userAgent \": \"Mozilla/5.0 (Windows NT 10.0 ; Win64; x64; rv:60.0) Gecko/20100101 Firefox/60.0\",\n    \"requestParameters\": null,\n    \"responseElements\": {\n        \"ConsoleLogin\": \"Success\"\n    },\n    \"additionalEventData\": {\n        \"LoginTo\": \"https://console.aws.amazon.com/console/home?state=hashArgs%23&isauthcode=true\",\n        \"MobileVersion\": \"No\",\n        \"MFAUsed\": \"No\"\n    },\n    \"eventID\": \"2681fc29-EXAMPLE\",\n    \"eventType\": \"AwsConsoleSignIn\",\n    \"recipientAccountId\": \"123456789012\"\n}" :key-fn keyword)]
   (println event))

 (let [event (json/read (io/reader "test/resources/sns-input-message.json") :key-fn keyword)]
   (println event))

 (let [event (-> "test/resources/sns-input-message.json" slurp (json/read-str :key-fn keyword))]
   (println event)))