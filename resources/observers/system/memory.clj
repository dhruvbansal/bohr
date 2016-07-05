(def memory-usage-annotations
  {
   :free         { :desc "Free memory"}
   :active       { :desc "Recently used memory"}
   :inactive     { :desc "Less recently used memory"}
   :slab         { :desc "In-kernel data structures cache"}
   :page-tables  { :desc "Memory dedicated to lowest level of page tables"}
   :vmalloc.used { :desc "Total vmalloc memory used"}
   :buffers      { :desc "Temporary memory for raw disk blocks"}
   :cached       { :desc "Page cache (not including swap.cached)"}
   :swap.cached  { :desc "Page cache also still present in swap"}
   :anon-pages   { :desc "Non-file backed pages mapped into use-space page tables"}
   :total        { :desc "Total usable memory"}
   :swap.total   { :desc "Total swap space"}
   :swap.free    { :desc "Swap space free"}})

;; See http://stackoverflow.com/questions/658411/entries-in-proc-meminfo for details.
;; and https://github.com/OpenTSDB/tcollector/issues/156
(defn- memory-usage-linux []
  (parse-properties
   (procfile-contents "meminfo")
    :converter #(* 1024 (Long/parseLong (string/replace % #" +kB" "")))
    :translation
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
  (parse-properties
   (sh-output "vm_stat")
   :converter #(* 4096 (Long/parseLong (string/replace % #"\." "")))
   :translation
   {"Pages free" :free
    "Pages inactive" :inactive
    "Pages active" :active}))

(defn- memory-usage []
  (annotate
   (case-os
    "Linux" (memory-usage-linux)
    "Mac"   (memory-usage-mac))
   memory-usage-annotations))

(defn- util-from-usage [usage util-names]
  (let [util-usages
        (map
         #(get (get usage %) :value)
         util-names)

        total-usage
        (apply + util-usages)

        normalized-usages
        (map
         #(* 100.0 (float (/ % total-usage)))
         util-usages)]
    (annotate
     (zipmap util-names normalized-usages)
     memory-usage-annotations)))

(defn- memory-util [usage]
  (case-os
   "Linux" (util-from-usage usage [:free :active :inactive :slab :page-tables :vmalloc.used])
   "Mac"   (util-from-usage usage [:free :inactive :active])))
  
(defn- cache-util [usage]
  (case-os
   "Linux" (util-from-usage usage [:buffers :cached :swap.cached :anon-pages])
   "Mac"   {}))

(observe :memory :ttl 5 :tags ["system" "memory"] :prefix "mem"
         (let [usage (memory-usage)]
           (submit-many usage :units "B" :prefix "usage")
           (submit-many (memory-util usage) :units "%" :prefix "util")
           (submit-many (cache-util usage) :units "%" :prefix "cache.util")))
