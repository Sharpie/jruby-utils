(ns puppetlabs.jruby-utils.slj4j-logger-test
  (:require [clojure.test :refer :all]
            [openvox.jruby-utils.jruby-testutils :as testutils]
            [puppetlabs.trapperkeeper.testutils.logging :as logutils]
            [openvox.jruby-utils.jruby-defaults :as jruby-defaults])
  (:import (clojure.lang Reflector)))

;; Populates *loader* variable. org.slf4j.* is delegated to the parent
;; classloader so log events reach the same LoggerContext with-test-logging
;; attaches its capture appender to.
;;
;; use-fixtures :each does not compose across separate calls -- a later
;; call replaces the fixtures from an earlier one -- so both fixtures must
;; be passed together in a single call.
(use-fixtures :each
  (testutils/with-restored-system-properties "jruby.logger.class")
  (testutils/with-isolated-classloader "org.slf4j."))

(defn get-logger
  "Loads JRuby's LoggerFactory inside of an isolated classloader and returns
  a root Logger instance from it. The isolation is required to test the effects
  of settings that are read by static field initializers run when the LoggerFactory
  class is loaded for the first time."
  [classloader logger-name]
  (-> (.loadClass classloader "org.jruby.util.log.LoggerFactory")
      (Reflector/invokeStaticMethod "getLogger" (into-array Object [logger-name]))))

(deftest slf4j-logger-test
  (let [actual-logger-name "my-test-logger"
        exception-message "exceptionally bad news"
        expected-logger-name (str "jruby." actual-logger-name)
        logger (get-logger testutils/*loader* actual-logger-name)
        actual-log-event (fn [event]
                           (assoc event :exception
                                        (when-let [exception (:exception event)]
                                          (.getMessage exception))))
        expected-log-event (fn [message level exception]
                             {:message message
                              :level level
                              :exception exception
                              :logger expected-logger-name})]
    (testing "name stored in logger"
      (is (= expected-logger-name (.getName logger))))

    (testing "warn with a string and objects"
      (logutils/with-test-logging
       (.warn logger "a {} {} warning" (into-array Object ["strongly" "worded"]))
       (is (logged? "a strongly worded warning" :warn))))
    (testing "warn with an exception"
      (logutils/with-test-logging
       (.warn logger (Exception. exception-message))
       (is (logged?
            #(= (expected-log-event "" :warn exception-message)
                (actual-log-event %))))))
    (testing "warn with a string and an exception"
      (logutils/with-test-logging
       (.warn logger "a warning" (Exception. exception-message))
       (is (logged?
            #(= (expected-log-event "a warning" :warn exception-message)
                (actual-log-event %))))))

    (testing "error with a string and objects"
      (logutils/with-test-logging
       (.error logger "a {} {} error" (into-array Object ["strongly" "worded"]))
       (is (logged? "a strongly worded error" :error))))
    (testing "error with an exception"
      (logutils/with-test-logging
       (.error logger (Exception. exception-message))
       (is (logged?
            #(= (expected-log-event "" :error exception-message)
                (actual-log-event %))))))
    (testing "error with a string and an exception"
      (logutils/with-test-logging
       (.error logger "an error" (Exception. exception-message))
       (is (logged?
            #(= (expected-log-event "an error" :error exception-message)
                (actual-log-event %))))))

    (testing "info with a string and objects"
      (logutils/with-test-logging
       (.info logger
              "some {} {} info"
              (into-array Object ["strongly" "worded"]))
       (is (logged? "some strongly worded info" :info))))
    (testing "info with an exception"
      (logutils/with-test-logging
       (.info logger (Exception. exception-message))
       (is (logged?
            #(= (expected-log-event "" :info exception-message)
                (actual-log-event %))))))
    (testing "info with a string and an exception"
      (logutils/with-test-logging
       (.info logger "some info" (Exception. exception-message))
       (is (logged?
            #(= (expected-log-event "some info" :info exception-message)
                (actual-log-event %))))))

    (testing "debug with a string and objects"
      (logutils/with-test-logging
       (.debug logger
               "some {} {} debug"
               (into-array Object ["strongly" "worded"]))
       (is (logged? "some strongly worded debug" :debug))))
    (testing "info with an exception"
      (logutils/with-test-logging
       (.debug logger (Exception. exception-message))
       (is (logged?
            #(= (expected-log-event "" :debug exception-message)
                (actual-log-event %))))))
    (testing "debug with a string and an exception"
      (logutils/with-test-logging
       (.debug logger "some debug" (Exception. exception-message))
       (is (logged?
            #(= (expected-log-event "some debug" :debug exception-message)
                (actual-log-event %))))))))

(deftest logger-class-is-explicitly-settable
  (testing "The logger class can be explicitly set via Java properties."
    (System/setProperty "jruby.logger.class" "org.jruby.util.log.StandardErrorLogger")
    (jruby-defaults/set-jruby-property-defaults!)
    (is (= "org.jruby.util.log.StandardErrorLogger"
           (-> (get-logger testutils/*loader* "test")
                 (.getClass)
                 (.getName))))))

(deftest logger-class-defaults-to-slf4j
  (testing "The logger class can be explicitly set via Java properties."
    (System/clearProperty "jruby.logger.class")
    (jruby-defaults/set-jruby-property-defaults!)
    (is (= "com.puppetlabs.jruby_utils.jruby.Slf4jLogger"
           (-> (get-logger testutils/*loader* "test")
                 (.getClass)
                 (.getName))))))

(deftest logger-class-defaults-to-no-debug
  (testing "Debug behavor for the SLF4J logger defaults to off."
    (System/clearProperty "jruby.logger.class")
    (jruby-defaults/set-jruby-property-defaults!)
    (is (false? (.isDebugEnabled (get-logger testutils/*loader* "test"))))))

(deftest logger-class-enables-debug-when-requested
  (testing "Debug behavor for the SLF4J logger is enabled when requested."
    (System/clearProperty "jruby.logger.class")
    (jruby-defaults/set-jruby-property-defaults!)
    (let [logger (get-logger testutils/*loader* "test")]
      (.setDebugEnable logger true)
      (is (true? (.isDebugEnabled logger))))))
