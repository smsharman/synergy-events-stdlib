(ns synergy-events-stdlib.core-test
  (:require [clojure.test :refer :all]
            [synergy-events-stdlib.core :refer :all]))

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

(deftest check-event-type-test
   (is (= "SNS" (check-event-type sns-test-message))))

(def sns-event-data {:orgId "1",
                     :eventAction "event1",
                     :eventId "7a5a815b-2e52-4d40-bec8-c3fc06edeb36",
                     :eventVersion 1,
                     :eventTimestamp "2020-04-17T11:23:10.904Z",
                     :userId "1",
                     :eventData {:key1 "value1", :key2 "value2"},
                     :parentId "7a5a815b-2e52-4d40-bec8-c3fc06edeb36",
                     :originId "7a5a815b-2e52-4d40-bec8-c3fc06edeb36"})

(deftest get-event-data-test
  (is (= sns-event-data (get-event-data sns-test-message "SNS"))))

(def status-map {:status false, :description "error-dispatching-to-topic", :return-value {:eventId 1234, :error "snsSendResult"}})

(deftest gen-status-map-test
  (is (= status-map (#'synergy-events-stdlib.core/gen-status-map false "error-dispatching-to-topic" {:eventId 1234
                                                                                                     :error   "snsSendResult"}))))
(def lambda-return {:status 200, :message "All's well that ends well."})

(deftest generate-lambda-return-test
  (is (= lambda-return (generate-lambda-return 200 "All's well that ends well."))))
