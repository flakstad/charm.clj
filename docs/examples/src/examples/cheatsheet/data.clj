(ns examples.cheatsheet.data
  "Cheatsheet structure and ClojureDocs data loading.

   Data comes from two sources:
   - Cheatsheet structure: section/subsection/group hierarchy
   - ClojureDocs export: docstrings, arglists, examples, see-alsos"
  (:require
   [clojure.edn :as edn]
   [clojure.string :as str]))

;; ---------------------------------------------------------------------------
;; ClojureDocs Data Loading
;; ---------------------------------------------------------------------------

(def clojuredocs-edn-url
  "https://github.com/clojure-emacs/clojuredocs-export-edn/raw/master/exports/export.compact.edn")

(defonce docs-cache (atom nil))

(defn load-docs!
  "Download and cache ClojureDocs data. Returns nil (side-effecting cmd)."
  []
  (when-not @docs-cache
    (let [data (-> clojuredocs-edn-url slurp edn/read-string)]
      (reset! docs-cache data)))
  nil)

(defn lookup
  "Look up documentation for a qualified symbol.
   Returns {:arglists [...] :doc \"...\" :examples [...] :see-alsos [...]} or nil."
  [qualified-sym]
  (when-let [docs @docs-cache]
    ;; Keys in the export are qualified keywords like :clojure.core/map
    (get docs (keyword (namespace qualified-sym) (name qualified-sym)))))

(defn all-symbols
  "Get all documented symbol names from the cache."
  []
  (when-let [docs @docs-cache]
    (keys docs)))

;; ---------------------------------------------------------------------------
;; Cheatsheet Structure
;; ---------------------------------------------------------------------------

;; Structure: vector of sections
;; Each section: {:name "..." :subsections [...]}
;; Each subsection: {:name "..." :groups [...]}
;; Each group: {:label "..." :fns [qualified-symbol ...]}

(def sections
  [{:name "Primitives"
    :subsections
    [{:name "Numbers"
      :groups
      [{:label "Arithmetic"
        :fns '[clojure.core/+ clojure.core/- clojure.core/* clojure.core// clojure.core/quot clojure.core/rem clojure.core/mod clojure.core/inc clojure.core/dec clojure.core/max clojure.core/min]}
       {:label "Compare"
        :fns '[clojure.core/== clojure.core/< clojure.core/> clojure.core/<= clojure.core/>= clojure.core/compare]}
       {:label "Cast"
        :fns '[clojure.core/byte clojure.core/short clojure.core/int clojure.core/long clojure.core/float clojure.core/double clojure.core/bigdec clojure.core/bigint clojure.core/num clojure.core/rationalize]}
       {:label "Test"
        :fns '[clojure.core/zero? clojure.core/pos? clojure.core/neg? clojure.core/even? clojure.core/odd? clojure.core/number? clojure.core/integer? clojure.core/ratio? clojure.core/decimal? clojure.core/float?]}
       {:label "Random"
        :fns '[clojure.core/rand clojure.core/rand-int]}]}
     {:name "Strings"
      :groups
      [{:label "Create"
        :fns '[clojure.core/str clojure.core/format]}
       {:label "Use"
        :fns '[clojure.core/count clojure.core/get clojure.core/subs clojure.core/compare clojure.string/join clojure.string/split clojure.string/split-lines clojure.string/replace clojure.string/reverse]}
       {:label "Regex"
        :fns '[clojure.core/re-find clojure.core/re-seq clojure.core/re-matches clojure.core/re-pattern clojure.core/re-matcher clojure.core/re-groups]}
       {:label "Letters"
        :fns '[clojure.string/capitalize clojure.string/lower-case clojure.string/upper-case]}
       {:label "Trim"
        :fns '[clojure.string/trim clojure.string/trim-newline clojure.string/triml clojure.string/trimr]}
       {:label "Test"
        :fns '[clojure.core/string? clojure.string/blank? clojure.string/starts-with? clojure.string/ends-with? clojure.string/includes?]}]}
     {:name "Other"
      :groups
      [{:label "Characters"
        :fns '[clojure.core/char clojure.core/char?]}
       {:label "Keywords"
        :fns '[clojure.core/keyword clojure.core/keyword? clojure.core/find-keyword]}
       {:label "Symbols"
        :fns '[clojure.core/symbol clojure.core/symbol? clojure.core/gensym]}]}]}

   {:name "Collections"
    :subsections
    [{:name "Generic Ops"
      :groups
      [{:label "Content tests"
        :fns '[clojure.core/distinct? clojure.core/empty? clojure.core/every? clojure.core/not-every? clojure.core/some clojure.core/not-any?]}
       {:label "Capabilities"
        :fns '[clojure.core/sequential? clojure.core/associative? clojure.core/sorted? clojure.core/counted? clojure.core/reversible?]}
       {:label "Type tests"
        :fns '[clojure.core/coll? clojure.core/list? clojure.core/vector? clojure.core/set? clojure.core/map? clojure.core/seq?]}]}
     {:name "Lists"
      :groups
      [{:label "Create"
        :fns '[clojure.core/list clojure.core/list*]}
       {:label "Examine"
        :fns '[clojure.core/first clojure.core/nth clojure.core/peek]}
       {:label "Change"
        :fns '[clojure.core/cons clojure.core/conj clojure.core/rest clojure.core/pop]}]}
     {:name "Vectors"
      :groups
      [{:label "Create"
        :fns '[clojure.core/vector clojure.core/vec clojure.core/vector-of clojure.core/mapv clojure.core/filterv]}
       {:label "Examine"
        :fns '[clojure.core/nth clojure.core/get clojure.core/peek]}
       {:label "Change"
        :fns '[clojure.core/assoc clojure.core/pop clojure.core/subvec clojure.core/replace clojure.core/conj clojure.core/rseq]}
       {:label "Ops"
        :fns '[clojure.core/reduce-kv]}]}
     {:name "Sets"
      :groups
      [{:label "Create"
        :fns '[clojure.core/set clojure.core/hash-set clojure.core/sorted-set clojure.core/sorted-set-by]}
       {:label "Examine"
        :fns '[clojure.core/get clojure.core/contains?]}
       {:label "Change"
        :fns '[clojure.core/conj clojure.core/disj]}
       {:label "Ops"
        :fns '[clojure.set/union clojure.set/difference clojure.set/intersection clojure.set/select]}
       {:label "Test"
        :fns '[clojure.set/subset? clojure.set/superset?]}]}
     {:name "Maps"
      :groups
      [{:label "Create"
        :fns '[clojure.core/hash-map clojure.core/array-map clojure.core/zipmap clojure.core/sorted-map clojure.core/sorted-map-by clojure.core/frequencies clojure.core/group-by]}
       {:label "Examine"
        :fns '[clojure.core/get clojure.core/get-in clojure.core/contains? clojure.core/find clojure.core/keys clojure.core/vals]}
       {:label "Change"
        :fns '[clojure.core/assoc clojure.core/assoc-in clojure.core/dissoc clojure.core/merge clojure.core/merge-with clojure.core/select-keys clojure.core/update clojure.core/update-in clojure.core/update-keys clojure.core/update-vals]}
       {:label "Ops"
        :fns '[clojure.core/reduce-kv]}
       {:label "Entry"
        :fns '[clojure.core/key clojure.core/val]}]}]}

   {:name "Sequences"
    :subsections
    [{:name "Create"
      :groups
      [{:label "From coll"
        :fns '[clojure.core/seq clojure.core/vals clojure.core/keys clojure.core/rseq clojure.core/subseq clojure.core/rsubseq clojure.core/sequence]}
       {:label "Producer fn"
        :fns '[clojure.core/lazy-seq clojure.core/repeatedly clojure.core/iterate clojure.core/iteration]}
       {:label "Constant"
        :fns '[clojure.core/repeat clojure.core/range]}
       {:label "From other"
        :fns '[clojure.core/file-seq clojure.core/line-seq clojure.core/re-seq clojure.core/tree-seq]}]}
     {:name "Seq in, Seq out"
      :groups
      [{:label "Get shorter"
        :fns '[clojure.core/distinct clojure.core/filter clojure.core/remove clojure.core/take-nth clojure.core/for clojure.core/dedupe clojure.core/random-sample]}
       {:label "Get longer"
        :fns '[clojure.core/cons clojure.core/conj clojure.core/concat clojure.core/lazy-cat clojure.core/mapcat clojure.core/cycle clojure.core/interleave clojure.core/interpose]}
       {:label "Tail-items"
        :fns '[clojure.core/rest clojure.core/nthrest clojure.core/next clojure.core/fnext clojure.core/nnext clojure.core/drop clojure.core/drop-while clojure.core/take-last]}
       {:label "Head-items"
        :fns '[clojure.core/take clojure.core/take-while clojure.core/butlast clojure.core/drop-last]}
       {:label "Rearrange"
        :fns '[clojure.core/reverse clojure.core/sort clojure.core/sort-by clojure.core/compare clojure.core/shuffle]}
       {:label "Process"
        :fns '[clojure.core/map clojure.core/pmap clojure.core/map-indexed clojure.core/mapcat clojure.core/for clojure.core/replace]}]}
     {:name "Using a Seq"
      :groups
      [{:label "Extract"
        :fns '[clojure.core/first clojure.core/second clojure.core/last clojure.core/rest clojure.core/next clojure.core/ffirst clojure.core/fnext clojure.core/nth clojure.core/rand-nth]}
       {:label "Construct"
        :fns '[clojure.core/zipmap clojure.core/into clojure.core/reduce clojure.core/reductions clojure.core/set clojure.core/vec clojure.core/mapv clojure.core/filterv]}
       {:label "Pass to fn"
        :fns '[clojure.core/apply]}
       {:label "Search"
        :fns '[clojure.core/some clojure.core/filter]}
       {:label "Force"
        :fns '[clojure.core/doseq clojure.core/dorun clojure.core/doall clojure.core/run!]}]}]}

   {:name "Functions"
    :subsections
    [{:name "Create"
      :groups
      [{:label "Define"
        :fns '[clojure.core/fn clojure.core/defn clojure.core/defn- clojure.core/identity clojure.core/constantly]}
       {:label "Compose"
        :fns '[clojure.core/comp clojure.core/complement clojure.core/partial clojure.core/juxt clojure.core/memoize clojure.core/fnil clojure.core/every-pred clojure.core/some-fn]}]}
     {:name "Call"
      :groups
      [{:label "Direct"
        :fns '[clojure.core/apply clojure.core/-> clojure.core/->> clojure.core/trampoline]}
       {:label "Threading"
        :fns '[clojure.core/as-> clojure.core/cond-> clojure.core/cond->> clojure.core/some-> clojure.core/some->>]}]}
     {:name "Test"
      :groups
      [{:label "Predicates"
        :fns '[clojure.core/fn? clojure.core/ifn?]}]}]}

   {:name "Macros"
    :subsections
    [{:name "Create"
      :groups
      [{:label "Define"
        :fns '[clojure.core/defmacro clojure.core/macroexpand-1 clojure.core/macroexpand]}]}
     {:name "Branch"
      :groups
      [{:label "If/When"
        :fns '[clojure.core/and clojure.core/or clojure.core/when clojure.core/when-not clojure.core/when-let clojure.core/when-first clojure.core/if-not clojure.core/if-let clojure.core/when-some clojure.core/if-some]}
       {:label "Cond"
        :fns '[clojure.core/cond clojure.core/condp clojure.core/case]}]}
     {:name "Loop"
      :groups
      [{:label "Iteration"
        :fns '[clojure.core/for clojure.core/doseq clojure.core/dotimes clojure.core/while]}]}
     {:name "Scope"
      :groups
      [{:label "Binding"
        :fns '[clojure.core/binding clojure.core/locking clojure.core/time clojure.core/with-out-str clojure.core/with-redefs]}]}]}

   {:name "Concurrency"
    :subsections
    [{:name "Atoms"
      :groups
      [{:label "Create"
        :fns '[clojure.core/atom]}
       {:label "Change"
        :fns '[clojure.core/swap! clojure.core/reset! clojure.core/compare-and-set! clojure.core/swap-vals! clojure.core/reset-vals!]}]}
     {:name "Refs"
      :groups
      [{:label "Create"
        :fns '[clojure.core/ref]}
       {:label "Examine"
        :fns '[clojure.core/deref]}
       {:label "Transaction"
        :fns '[clojure.core/dosync clojure.core/ensure clojure.core/ref-set clojure.core/alter clojure.core/commute]}]}
     {:name "Agents"
      :groups
      [{:label "Create"
        :fns '[clojure.core/agent]}
       {:label "Change"
        :fns '[clojure.core/send clojure.core/send-off clojure.core/restart-agent]}
       {:label "Block"
        :fns '[clojure.core/await clojure.core/await-for]}]}
     {:name "Futures"
      :groups
      [{:label "Create"
        :fns '[clojure.core/future clojure.core/future-call]}
       {:label "Test"
        :fns '[clojure.core/future-done? clojure.core/future-cancel clojure.core/future-cancelled? clojure.core/future?]}]}
     {:name "Volatiles"
      :groups
      [{:label "Create"
        :fns '[clojure.core/volatile!]}
       {:label "Change"
        :fns '[clojure.core/vreset! clojure.core/vswap!]}
       {:label "Test"
        :fns '[clojure.core/volatile?]}]}]}

   {:name "IO"
    :subsections
    [{:name "Files"
      :groups
      [{:label "Read/Write"
        :fns '[clojure.core/spit clojure.core/slurp]}
       {:label "Misc"
        :fns '[clojure.core/file-seq clojure.core/flush]}]}
     {:name "Print"
      :groups
      [{:label "To *out*"
        :fns '[clojure.core/pr clojure.core/prn clojure.core/print clojure.core/printf clojure.core/println clojure.core/newline]}
       {:label "To string"
        :fns '[clojure.core/format clojure.core/with-out-str clojure.core/pr-str clojure.core/prn-str clojure.core/print-str clojure.core/println-str]}]}
     {:name "Read"
      :groups
      [{:label "From *in*"
        :fns '[clojure.core/read-line]}
       {:label "From string"
        :fns '[clojure.core/with-in-str clojure.core/read-string clojure.edn/read-string]}]}
     {:name "Tap"
      :groups
      [{:label "Tap"
        :fns '[clojure.core/tap> clojure.core/add-tap clojure.core/remove-tap]}]}]}

   {:name "Transducers"
    :subsections
    [{:name "Use"
      :groups
      [{:label "Apply"
        :fns '[clojure.core/into clojure.core/sequence clojure.core/transduce clojure.core/eduction]}
       {:label "Terminate"
        :fns '[clojure.core/reduced clojure.core/reduced? clojure.core/deref]}]}
     {:name "Built-in"
      :groups
      [{:label "Transform"
        :fns '[clojure.core/map clojure.core/mapcat clojure.core/filter clojure.core/remove clojure.core/keep clojure.core/keep-indexed clojure.core/map-indexed clojure.core/replace clojure.core/distinct clojure.core/interpose clojure.core/dedupe clojure.core/cat]}
       {:label "Partition"
        :fns '[clojure.core/partition-by clojure.core/partition-all]}
       {:label "Take/Drop"
        :fns '[clojure.core/take clojure.core/take-while clojure.core/take-nth clojure.core/drop clojure.core/drop-while clojure.core/halt-when]}]}]}

   {:name "Java"
    :subsections
    [{:name "Interop"
      :groups
      [{:label "General"
        :fns '[clojure.core/.. clojure.core/doto clojure.core/new clojure.core/bean clojure.core/import]}
       {:label "Cast"
        :fns '[clojure.core/boolean clojure.core/byte clojure.core/short clojure.core/char clojure.core/int clojure.core/long clojure.core/float clojure.core/double]}
       {:label "Type"
        :fns '[clojure.core/class clojure.core/class? clojure.core/type clojure.core/bases clojure.core/supers clojure.core/instance?]}]}
     {:name "Exceptions"
      :groups
      [{:label "Throw/Catch"
        :fns '[clojure.core/throw clojure.core/try clojure.core/ex-info clojure.core/ex-data clojure.core/ex-message clojure.core/ex-cause]}]}
     {:name "Arrays"
      :groups
      [{:label "Create"
        :fns '[clojure.core/make-array clojure.core/into-array clojure.core/to-array clojure.core/to-array-2d clojure.core/aclone]}
       {:label "Use"
        :fns '[clojure.core/aget clojure.core/aset clojure.core/alength clojure.core/amap clojure.core/areduce]}]}]}

   {:name "Abstractions"
    :subsections
    [{:name "Protocols"
      :groups
      [{:label "Define"
        :fns '[clojure.core/defprotocol clojure.core/extend-type clojure.core/extend-protocol clojure.core/reify]}
       {:label "Test"
        :fns '[clojure.core/satisfies? clojure.core/extends?]}]}
     {:name "Records"
      :groups
      [{:label "Define"
        :fns '[clojure.core/defrecord]}
       {:label "Test"
        :fns '[clojure.core/record?]}]}
     {:name "Multimethods"
      :groups
      [{:label "Define"
        :fns '[clojure.core/defmulti clojure.core/defmethod]}
       {:label "Dispatch"
        :fns '[clojure.core/get-method clojure.core/methods]}
       {:label "Remove"
        :fns '[clojure.core/remove-method clojure.core/remove-all-methods]}
       {:label "Hierarchy"
        :fns '[clojure.core/derive clojure.core/underive clojure.core/isa? clojure.core/parents clojure.core/ancestors clojure.core/descendants]}]}]}])

;; ---------------------------------------------------------------------------
;; Filtering & Flattening
;; ---------------------------------------------------------------------------

(defn filter-sections
  "Filter sections by query string, keeping hierarchy structure.
   Returns sections with only groups containing matching functions.
   Empty subsections and sections are removed."
  [sects query]
  (let [query (str/lower-case (str/trim (or query "")))]
    (if (str/blank? query)
      sects
      (->> sects
           (keep (fn [section]
                   (let [subsections
                         (->> (:subsections section)
                              (keep (fn [subsection]
                                      (let [groups
                                            (->> (:groups subsection)
                                                 (keep (fn [group]
                                                         (let [fns (filterv
                                                                    (fn [sym]
                                                                      (str/includes?
                                                                       (str/lower-case (name sym))
                                                                       query))
                                                                    (:fns group))]
                                                           (when (seq fns)
                                                             (assoc group :fns fns)))))
                                                 vec)]
                                        (when (seq groups)
                                          (assoc subsection :groups groups)))))
                              vec)]
                     (when (seq subsections)
                       (assoc section :subsections subsections)))))
           vec))))

(defn sections->grid
  "Convert sections into a grid: a vector of group rows.
   Each row = {:section ... :subsection ... :label ... :fns [...]}.
   Row index = group index across all sections/subsections."
  [sects]
  (vec (for [section sects
             subsection (:subsections section)
             group (:groups subsection)]
         {:section (:name section)
          :subsection (:name subsection)
          :label (:label group)
          :fns (:fns group)})))
