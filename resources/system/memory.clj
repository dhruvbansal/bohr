;; See http://stackoverflow.com/questions/658411/entries-in-proc-meminfo for details.
;; and https://github.com/OpenTSDB/tcollector/issues/156
(defn- memory-usage-linux []
  (translate
   (parse-two-columns
    (procfile-contents "meminfo")
    :value-function #(* 1024 (Long/parseLong (string/replace % #" +kB" ""))))
   {"MemFree"     :free
    "Active"      :active
    "Inactive"    :inactive
    "Slab"        :slab
    "PageTables"  :page-tables
    "VmallocUsed" :vmalloc.used    
    
    "Buffers"     :buffers
    "Cached"      :cached
    "SwapCached"  :swap.cached
    "AnonPages"   :anon-pages
    
    "MemTotal"    :total
    "SwapTotal"   :swap.total
    "SwapFree"    :swap.free}))


;; See http://apple.stackexchange.com/questions/81581/why-does-free-active-inactive-speculative-wired-not-equal-total-ram
(defn- memory-usage-mac []
  (translate
   (parse-two-columns
    (sh-output "vm_stat")
    :value-function #(* 4096 (Long/parseLong (string/replace % #"\." ""))))
   {"Pages free" :free
    "Pages inactive" :inactive
    "Pages active" :active
    "Pages speculative" :speculative
    "Pages throttled" :throttled
    "Pages wired down" :wired-down}))

(defn- util-from-usage [usage util-names]
  (let [util-usages
        (map
         #(get usage %)
         util-names)

        total-usage
        (apply + util-usages)

        normalized-usages
        (map
         #(* 100.0 (float (/ % total-usage)))
         util-usages)]
    (zipmap util-names normalized-usages)))

(defn- memory-util-linux [usage]
  (util-from-usage usage [:free :active :inactive :slab :page-tables :vmalloc.used]))

(defn- memory-util-mac [usage]
  (util-from-usage usage [:free :inactive :active :speculative :throttled :wired-down]))

(defn- cache-util-linux [usage]
  (util-from-usage usage [:buffers :cached :swap.cached :anon-pages]))

(observe :memory :ttl 5 :tags ["system" "memory"] :prefix "mem"
         (case-os
          "Linux"
          (let [usage (memory-usage-linux)]
            (submit-values usage :units "B" :prefix "usage")
            (submit-values (memory-util-linux usage) :units "%" :prefix "util")
            (submit-values (cache-util-linux usage) :units "%" :prefix "cache.util"))

          "Mac"
          (let [usage (memory-usage-mac)]
            (submit-values usage :units "B" :prefix "usage")
            (submit-values (memory-util-mac usage) :units "%" :prefix "util"))))
