(ns openvox.jruby-utils.jruby-defaults
  "Requiring this namespace has one side-effect: it sets Java properties
  read by JRuby settings in order to change default behavior, while retaining
  the ability for users to explicitly overrride settings via JAVA_ARGS.

  This namespace should be required very early in the application lifecycle,
  before any classes are imported from org.jruby, as some properties are read
  once during class initialization and then never consulted again.")

(def defaults
  "Map of String->String indicating defaults to set in JVM properties that
  influence JRuby behavior."
  {
    ;; JRuby 9.4 defaulted to false, JRuby 10.0 changes to true. Using
    ;; InvokeDynamic can ultimately produce faster code, but this comes at the
    ;; cost of a slower, more volatile warm-up period. So, keep the 9.4 default
    ;; of false as OpenVox continually creates anonymous Ruby classes that
    ;; repeatedly trigger expensive compilation.
    ;;
    ;; Re-visit if improvements are made on the JRuby side to reduce cost, or
    ;; on the OpenVox side to reduce the number of ephemeral Ruby classes that
    ;; force the JRuby compiler to repeatedly re-compile.
    "jruby.compile.invokedynamic" "false"

    ;; Set the default JRuby logger implementation to something that is tied
    ;; into TrapperKeeper application configuration and controllable via
    ;; logback.xml. This system property must be set before the
    ;; org.jruby.util.log.LoggerFactory class is loaded as a static field
    ;; initializer binds to the setting value for the rest of the process
    ;; lifetime.
    ;;
    ;; This class was created back in the JRuby 1.7 days when there was no
    ;; SLF4J implementation provided by upstream. JRuby 9 added an official
    ;; SLF4JLogger that is nearly identical to the one in jruby-utils. Switching
    ;; would be a breaking change as the leading "jruby." namespace segment
    ;; would disappear and that would break exsiting LogBack config files
    ;; out in the field. With no clear benefit, this is not worth the break
    ;; unless the two implementations diverge dramatically in functionality.
    "jruby.logger.class"          "com.puppetlabs.jruby_utils.jruby.Slf4jLogger"
  })

(defn set-jruby-property-defaults!
  "Loop over the defaults map and set each in Java properties, if not already
  set by some other mechanism like a `-D` flag. Return a map of properties
  set in this way."
  []
  (let [properties (System/getProperties)]
    (into {} (filter (fn [[k v]] (nil? (.putIfAbsent properties k v))) defaults))))

;; This statement causes the actual side-effect of setting defaults in the Java
;; system properties when this namespace is loaded. The variable holds a map of
;; properties that were modified.
(defonce defaults-set-on-load (set-jruby-property-defaults!))
