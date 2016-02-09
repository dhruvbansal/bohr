(defn- used-properties-for [filesystem]
  (filter
   #(re-find #"\.used$" (name %))
   (keys filesystem)))

(defn- units-of-property [property]
  (first
   (string/split (name property) #"\." 2)))

(defn- make-property-name [unit state pct]
  (if pct
    (keyword (format "%s.%s.pct" (name unit) (name state)))
    (keyword (format "%s.%s" (name unit) (name state)))))

(defn- make-metric-name [unit state filesystem pct]
  (format "%s[%s]"
          (name (make-property-name unit state pct))
          (get filesystem :name)))

(defn- pct-unit-in-states-map [filesystem unit]
  (let [free-property  (make-property-name unit :free  false)
        used-property  (make-property-name unit :used  false)
        total-property (make-property-name unit :total false)

        free  (get filesystem free-property)
        used  (get filesystem used-property)
        total (get filesystem total-property (+ free used))

        free-pct (* 100.0 (float (if (< 0 total) (/ free total) 0)))
        used-pct (* 100.0 (float (if (< 0 total) (/ used total) 0)))
        
        free-pct-property (make-property-name unit :free true)
        used-pct-property (make-property-name unit :used true)]
    {
     total-property
     total
     free-pct-property
     free-pct
     used-pct-property
     used-pct
     }))

(defn- with-pct-properties [filesystem]
  (apply
   merge
   filesystem
   (map
    #(pct-unit-in-states-map filesystem (units-of-property %))
    (used-properties-for filesystem))))

(defn- parsed-df-output [command converters]
  (into
   {}
   (map
    (fn [filesystem] [(get filesystem :name) filesystem])
    (filter
     (fn [filesystem] (re-find #"^/" (get filesystem :name)))
     (parse-table
      (sh-output (format "df %s" command))
      converters
      :start-at 2
      :transform-object with-pct-properties)))))

(defn- filesystems-linux-blocks []
  (parsed-df-output
   "-l -k"
   [[:name        identity]
    [:bytes.total #(* 1024 (Integer/parseInt %))]
    [:bytes.used  #(* 1024 (Integer/parseInt %))]
    [:bytes.free  #(* 1024 (Integer/parseInt %))]
    [nil          identity]
    [:mountpoint  identity]]))

(defn- filesystems-linux-inodes []
  (parsed-df-output
   "-l -i"
   [[:name         identity]
    [:inodes.total #(Integer/parseInt %)]
    [:inodes.used  #(Integer/parseInt %)]
    [:inodes.free  #(Integer/parseInt %)]
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
    [:bytes.total #(* 1024 (Integer/parseInt %))]
    [:bytes.used  #(* 1024 (Integer/parseInt %))]
    [:bytes.free  #(* 1024 (Integer/parseInt %))]
    [nil          identity]
    [:inodes.used #(Integer/parseInt %)]
    [:inodes.free #(Integer/parseInt %)]
    [nil          identity]
    [:mountpoint  identity]]))

(defn- observe-filesystem [filesystem]
  (doseq [unit [:bytes :inodes]]
    (doseq [state [:free :used :total]]
      (submit
       (make-metric-name unit state filesystem false)
       (get filesystem (make-property-name unit state false))
       :unit "B")
      (if (not (= :total state))
        (submit
         (make-metric-name unit state filesystem true)
         (get filesystem (make-property-name unit state true))
         :unit "%")))))

(defn- filesystems []
  (case-os
   "Linux" (filesystems-linux)
   "Mac"   (filesystems-mac)))

(observe :fs :ttl 5 :tags ["system" "fs"] :prefix "fs"
         (doseq [filesystem (filesystems)] (observe-filesystem filesystem)))
