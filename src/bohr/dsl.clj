(ns bohr.dsl)

(defn dsl!
  "Evaluate string as Bohr DSL."
  [string]
  (load-string string))
