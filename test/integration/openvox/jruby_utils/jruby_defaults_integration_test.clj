(ns openvox.jruby-utils.jruby-defaults-integration-test
  (:require [clojure.test :refer :all]
            [openvox.jruby-utils.jruby-testutils :as testutils]
            [openvox.jruby-utils.jruby-defaults :as jruby-defaults])
  (:import (clojure.lang Reflector)))

(defn get-bytecode-mode
  "Create a JRuby interpreter inside of an isolated classloader and then return
  the bytcode mode for its JIT compiler."
  [classloader]
  (let [scope-class     (.loadClass classloader "org.jruby.embed.LocalContextScope")
        singlethread    (.get (.getField scope-class "SINGLETHREAD") nil)
        container-class (.loadClass classloader "com.puppetlabs.jruby_utils.jruby.InternalScriptingContainer")
        container       (Reflector/invokeConstructor container-class (into-array Object [singlethread]))
        runtime         (.getRuntime (.getProvider container))
        visitor-class   (.loadClass classloader "org.jruby.ir.targets.JVMVisitor")
        visitor         (Reflector/invokeStaticMethod visitor-class "newForJIT" (into-array Object [runtime]))]
    (str (.getBytecodeMode visitor))))

(use-fixtures :each
  (apply testutils/with-restored-system-properties
    (keys jruby-defaults/defaults))
  ;; Populates *loader* variable.
  (testutils/with-isolated-classloader))

(deftest default-compile-invokedynamic-is-mixed-test
  (testing "with no property override, JRuby's JIT compiler operates in MIXED mode."
    (System/clearProperty "jruby.compile.invokedynamic")
    (jruby-defaults/set-jruby-property-defaults!)
    (is (= "MIXED" (get-bytecode-mode testutils/*loader*)))))

(deftest override-compile-invokedynamic-is-honored-test
  (testing "an explicit -Djruby.compile.invokedynamic=true, JRuby's JIT compiler operates in INDY mode."
    (System/setProperty "jruby.compile.invokedynamic" "true")
    (jruby-defaults/set-jruby-property-defaults!)
    (is (= "INDY" (get-bytecode-mode testutils/*loader*)))))
