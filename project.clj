(defproject synergy-events-stdlib "0.1.8"
  :description "Synergy Event Integration Architecture standard library"
  :url "http://synergyxm.ai/synergy-events-stdlib"
  :license {:name "Hackthorn Innovation Ltd"
            :url ""}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/data.json "0.2.6"]
                 [synergy-specs "0.1.9"]
                 [org.clojure/tools.reader "1.3.2"]
                 [com.taoensso/timbre "4.10.0"]
                 [com.cognitect.aws/api "0.8.456"]
                 [com.cognitect.aws/endpoints "1.1.11.753"]
                 [com.cognitect.aws/sns "773.2.578.0"]
                 [com.cognitect.aws/ssm "794.2.640.0"]]
  :repl-options {:init-ns synergy-events-stdlib.core})
