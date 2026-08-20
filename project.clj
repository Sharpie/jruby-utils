(def i18n-version "1.0.5")

(defproject org.openvoxproject/jruby-utils "7.0.3-SNAPSHOT"
  :description "A library for working with JRuby"
  :url "https://github.com/openvoxproject/jruby-utils"
  :license {:name "Apache License, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}

  :min-lein-version "2.12.0"

  :pedantic? :abort

  :source-paths ["src/clj"]
  :java-source-paths ["src/java"]
  :test-paths ["test/unit" "test/integration"]

  ;; Generally, try to keep version pins in :managed-dependencies and the libraries
  ;; this project actually uses in :dependencies, inheriting the version from
  ;; :managed-dependencies. This prevents endless version conflicts due to deps of deps.
  ;; Renovate should keep the versions largely in sync between projects.
  :managed-dependencies [[org.clojure/clojure "1.12.5"]
                         [org.clojure/java.jmx "1.1.1"]
                         [org.clojure/tools.logging "1.3.1"]
                         [clj-commons/fs "1.6.312"]
                         [commons-io "2.22.0"]
                         [commons-codec "1.22.1"]
                         [org.bouncycastle/bcpkix-jdk18on "1.85"]
                         [org.openvoxproject/i18n ~i18n-version]
                         [org.openvoxproject/jruby-deps "10.1.1.0-1"]
                         [org.openvoxproject/kitchensink "3.5.8"]
                         [org.openvoxproject/kitchensink "3.5.8" :classifier "test"]
                         [org.openvoxproject/ring-middleware "2.2.1"]
                         [org.openvoxproject/trapperkeeper "5.0.5"]
                         [org.openvoxproject/trapperkeeper "5.0.5" :classifier "test"]
                         [org.tcrawley/dynapath "1.1.0"]
                         [ring/ring-core "1.15.5"]
                         [prismatic/schema "1.4.2"]
                         [slingshot "0.12.2"]]

  :dependencies [[org.clojure/clojure]
                 [org.clojure/java.jmx]
                 [org.clojure/tools.logging]

                 [clj-commons/fs]
                 [org.openvoxproject/i18n]
                 [org.openvoxproject/jruby-deps]
                 [org.openvoxproject/kitchensink]
                 [org.openvoxproject/ring-middleware]
                 [org.openvoxproject/trapperkeeper]
                 [prismatic/schema]
                 [ring/ring-core]
                 [slingshot]]

  :deploy-repositories [["releases" {:url "https://clojars.org/repo"
                                     :username :env/CLOJARS_USERNAME
                                     :password :env/CLOJARS_PASSWORD
                                     :sign-releases false}]]

  ;; By declaring a classifier here and a corresponding profile below we'll get an additional jar
  ;; during `lein jar` that has all the code in the test/ directory. Downstream projects can then
  ;; depend on this test jar using a :classifier in their :dependencies to reuse the test utility
  ;; code that we have.
  :classifiers [["test" :testutils]]

  :profiles {:dev {:dependencies  [[org.openvoxproject/kitchensink :classifier "test" :scope "test"]
                                   [org.openvoxproject/trapperkeeper :classifier "test" :scope "test"]
                                   [org.bouncycastle/bcpkix-jdk18on]
                                   [org.tcrawley/dynapath]]
                   :jvm-opts ~(let [version (System/getProperty "java.specification.version")
                                    [major minor _] (clojure.string/split version #"\.")]
                                (concat
                                  ["-XX:+UseG1GC"
                                   "-Xms1G"
                                   "-Xmx2G"]
                                  (if (>= 17 (java.lang.Integer/parseInt major))
                                    ["--add-opens" "java.base/sun.nio.ch=ALL-UNNAMED" "--add-opens" "java.base/java.io=ALL-UNNAMED"]
                                    [])))}
             :testutils {:source-paths ^:replace ["test/unit" "test/integration"]}}

  :plugins [[org.openvoxproject/i18n ~i18n-version :hooks false]])
