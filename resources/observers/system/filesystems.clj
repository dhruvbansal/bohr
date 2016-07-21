(defn- used-properties-for [filesystem]
  (filter
   #(re-find #"\.used$" (name %))
   (keys filesystem)))

(defn- units-of-property [property]
  (first
   (string/split (name property) #"\." 2)))

(defn- make-property-name [unit state relative]
  (if relative
    (keyword (format "%s.%s.relative" (name unit) (name state)))
    (keyword (format "%s.%s" (name unit) (name state)))))

(defn- make-metric-name [unit state relative]
  (name (make-property-name unit state relative)))

(defn- make-metric-description [filesystem unit state relative]
  (format
   (str "%s %s on %s" (if relative " (relative)" ""))
   (string/capitalize (name state))
   (name unit)
   (get filesystem :name)))

(defn- relative-unit-in-states-map [filesystem unit]
  (let [free-property  (make-property-name unit :free  false)
        used-property  (make-property-name unit :used  false)
        total-property (make-property-name unit :total false)

        free  (get filesystem free-property)
        used  (get filesystem used-property)
        total (get filesystem total-property (+ free used))

        free-relative (* 100.0 (float (if (< 0 total) (/ free total) 0)))
        used-relative (* 100.0 (float (if (< 0 total) (/ used total) 0)))
        
        free-relative-property (make-property-name unit :free true)
        used-relative-property (make-property-name unit :used true)]
    {
     total-property
     total
     free-relative-property
     free-relative
     used-relative-property
     used-relative
     }))

(defn- with-relative-properties [filesystem]
  (apply
   merge
   filesystem
   (map
    #(relative-unit-in-states-map filesystem (units-of-property %))
    (used-properties-for filesystem))))

(defn- parsed-df-output [command converters]
  (map-table
   :name
   (parse-table
    (sh-output (format "df %s" command))
    converters
    :start-at 2
    :row-filter #(re-find #"^/" (get % :name))
    :transform-row with-relative-properties)))

(defn- filesystems-linux-blocks []
  (parsed-df-output
   "-l -k"
   [[:name        identity]
    [:space.total 1024]
    [:space.used  1024]
    [:space.free  1024]
    [nil          identity]
    [:mountpoint  identity]]))

(defn- filesystems-linux-inodes []
  (parsed-df-output
   "-l -i"
   [[:name         identity]
    [:inodes.total :long]
    [:inodes.used  :long]
    [:inodes.free  :long]
    [nil         identity]
    [:mountpoint identity]]))

(defn- filesystems-linux []
  (let [blocks (filesystems-linux-blocks)
        inodes (filesystems-linux-inodes)]
    (map
     #(merge (get blocks %) (get inodes %))
     (distinct
      (concat
       (keys blocks)
       (keys inodes))))))

(defn- filesystems-mac []
  (parsed-df-output
   "-l -k"
   [[:name        identity]
    [:space.total 1024]
    [:space.used  1024]
    [:space.free  1024]
    [nil          identity]
    [:inodes.used :long]
    [:inodes.free :long]
    [nil          identity]
    [:mountpoint  identity]]))

(defn- observe-filesystem [filesystem]
  (doseq [unit [:space :inodes]]
    (doseq [state [:free :used :total]]
      (submit
       (make-metric-name unit state false)
       (get filesystem (make-property-name unit state false))
       :desc (make-metric-description filesystem unit state false)
       :units (if (= unit :space) "B")
       :attrs { :device (get filesystem :name) :agg "mean"})
      (if (not (= :total state))
        (submit
         (make-metric-name unit state true)
         (get filesystem (make-property-name unit state true))
         :desc (make-metric-description filesystem unit state true)
         :units "%"
         :attrs { :device (get filesystem :name) :agg "mean"})))))

(defn- filesystems []
  (case-os
   "Linux" (filesystems-linux)
   "Mac"   (filesystems-mac)))

(observe :fs :period 10 :prefix "fs"
         (doseq [filesystem (filesystems)] (observe-filesystem filesystem)))
