(def interface-annotations
  {
   :in.bytes    { :desc "Number of bytes received"   :tags ["counter"] :units "B"}
   :in.packets  { :desc "Number of packets received" :tags ["counter"]}
   :out.bytes   { :desc "Number of bytes sent"       :tags ["counter"] :units "B"}
   :out.packets { :desc "Number of packets sent"     :tags ["counter"]}
   })
  
(defn- interfaces-linux []
  (map-table
   :name
   (parse-table
    (procfile-contents "net/dev")
    [[:name          identity]
     [:in.bytes      :long]
     [:in.packets    :long]
     [nil            identity]
     [nil            identity]
     [nil            identity]
     [nil            identity]
     [nil            identity]
     [nil            identity]
     [:out.bytes     :long]
     [:out.packets   :long]
     [nil            identity]
     [nil            identity]
     [nil            identity]
     [nil            identity]
     [nil            identity]
     [nil            identity]]
    :start-at 3
    :col-sep #":?\s+"
    :row-filter
    #(not (re-find #"(lo)" (get % :name))))
   :delete-key true
   :transform-row #(annotate % interface-annotations)))

(defn- interfaces []
  (case-os
   "Linux" (interfaces-linux)))

(observe :net :period 5 :prefix "net"
         (doseq [[interface-name interface] (seq (interfaces))]
           (submit-many interface :attributes { :interface interface-name})))
