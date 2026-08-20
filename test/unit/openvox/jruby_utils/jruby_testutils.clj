(ns openvox.jruby-utils.jruby-testutils
  "Utility functions for JRuby tests."
  (:require [clojure.java.io :as io]
            [clojure.string :as str])
  (:import (java.net URLClassLoader URL)))

(defn with-restored-system-properties
  "Creates a test fixture that looks up named Java properties, caches their
  values, runs the test case, then restores any cached values."
  [& properties]
  (fn [f]
    (let [saved-properties (into {} (map #(do [% (System/getProperty %)])) properties)]
      (try
        (f)
        (finally
          (doseq [[k v] saved-properties]
            (if v
              (System/setProperty k v)
              (System/clearProperty k))))))))


(def ^:dynamic *loader* nil)

(defn gen-isolated-loader
  "Create an isolated class loader, populated with a copy of the JVM
  classpath.

  delegated-prefixes is a, possibly empty, vector of namespaces to
  delegate to the main classloader instead of loading in isolation.
  This can be used to preserve some global behavior, such as logger
  configuration."
  ^ClassLoader [delegated-prefixes]
  (let [urls (->> (str/split (System/getProperty "java.class.path")
                              (re-pattern (System/getProperty "path.separator")))
                   (map #(-> (io/file %) .toURI .toURL))
                   (into-array URL))
        parent (.getContextClassLoader (Thread/currentThread))]
    (proxy [URLClassLoader] [urls nil]
      (loadClass
        ([class-name] (.loadClass this class-name false))
        ([class-name resolve?]
         (if (some #(str/starts-with? class-name %) delegated-prefixes)
           (.loadClass parent class-name)
           (proxy-super loadClass class-name resolve?)))))))

(defn with-isolated-classloader
  "Creates a temporary Java classloader, assigns it to *loader* and then
  runs the test case with that variable in-context. This enables testing
  class variables that are initialized at load-time with different
  system property combinations. Java reflection must be used to invoke
  constructors and other static methods of classes loaded into the temporary
  classloader."
  [& delegated-prefixes]
  (fn [f]
    (with-open [loader (gen-isolated-loader delegated-prefixes)]
      (binding [*loader* loader]
        (f)))))
