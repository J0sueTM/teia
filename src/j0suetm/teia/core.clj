(ns j0suetm.teia.core
  "Teia is a Simple, Fast and Reliable HyperMedia library.")

(defmacro defcmp
  "Helper macro that defines a component.

  Simply wraps `j0suetm.teia.component/->Component`."
  [cmp-name args body]
  `(j0suetm.teia.component/->Component
    (keyword '~cmp-name)
    (fn ~args
      ~@body)))
