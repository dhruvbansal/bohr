(def interface-annotations
  {
   :in.data     { :desc "Data received"   :units "B" :attrs { :agg "last" :counter true }}
   :in.packets  { :desc "Number of packets received" :attrs { :agg "last" :counter true }}
   :out.data    { :desc "Data sent"       :units "B" :attrs { :agg "last" :counter true }}
   :out.packets { :desc "Number of packets sent"     :attrs { :agg "last" :counter true }}
   })
  
(defn- interfaces-linux []
  (map-table
   :name
   (parse-table
    (procfile-contents "net/dev")
    [[:name          identity]
     [:in.data       :long]
     [:in.packets    :long]
     [nil            identity]
     [nil            identity]
     [nil            identity]
     [nil            identity]
     [nil            identity]
     [nil            identity]
     [:out.data      :long]
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

(observe :net :period 10 :prefix "net"
         (doseq [[interface-name interface] (seq (interfaces))]
           (submit-many interface :attrs { :interface interface-name})))
