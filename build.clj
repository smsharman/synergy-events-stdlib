(ns build
  "Synergy Events Standard Library build script.

  Run tests:
    clj -X:test

  Build uberjar:
    clj -T:build uberjar

  Deploy to Clojars (signed release):
    clj -T:build deploy

  For more information run:
  clj -A:deps -T:build help/doc"
  (:require [clojure.tools.build.api :as b]
            [org.corfield.build :as bb]
            [deps-deploy.deps-deploy :as dd]))

(def lib 'synergy-events-stdlib/synergy-events-stdlib)
(def version (format "1.0.%s" (b/git-count-revs nil)))
(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"}))
(def uber-file (format "target/%s-%s-standalone.jar" (name lib) version))

(defn clean [_]
  (b/delete {:path "target"}))

(defn prep [_]
  (b/write-pom {:class-dir class-dir
                :lib lib
                :version version
                :basis basis
                :src-dirs ["src"]})
  (b/copy-dir {:src-dirs ["src"]
               :target-dir class-dir}))

(defn uber [_]
  (b/compile-clj {:basis basis
                  :src-dirs ["src"]
                  :class-dir class-dir})
  (b/uber {:class-dir class-dir
           :uber-file uber-file
           :basis basis}))

(defn uberjar "Run the pipeline of tests and build the uberjar." [opts]
      (-> opts
          (assoc :lib lib :version version)
          (bb/run-tests)
          (clean)
          (prep)
          (uber)
          ))

(defn deploy-to-clojars [opts]
      (let [class-dir (str "target/classes")]
           (dd/deploy (merge {:installer :remote :artifact uber-file
                              :pom-file (b/pom-path {:lib lib :class-dir class-dir})
                              :sign-releases? true}
                             opts)))
      opts)

(defn deploy "Deploy the JAR to Clojars." [opts]
      (-> opts
          (assoc :lib lib :version version :jar-file uber-file)
          (deploy-to-clojars)))