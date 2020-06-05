(ns clj-pdfbooklet.common
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.reflect :as r]
            [taoensso.timbre :as timbre :refer [error]])
  (:use [clojure.pprint :only [print-table]]))

(defn get-full-path-files-in-dir
  "Returns absolute path of files in the passed directory.
   :recursively? keyword controls whether walk will be done recursively"
  [path & {:keys [recursively?]}]
  (let [file (io/file path)]
    (if (.isDirectory file)
      (do
        (if (= true recursively?)
          (file-seq file)
          (->> file
               .listFiles)))
      (do (timbre/error (str "Passed path: `" path "` is not a directory"))))))

(defn files-to-process
  "Returns paths to csv profiles files produced by xsv table tool."
  [dir bool]
  (->> (get-full-path-files-in-dir dir :recursively? bool)
       (map #(.getAbsolutePath %))
       (filter #(str/ends-with? % ".pdf"))))

(defn make-page-range
  "Make vector of ints from n to m inclusive.
   n,m needs to be positive intiger.
   n needs to be < than m."
  [n m]
  {:pre  [(pos? n), (pos? m), (> m n)]
   :post [(vector? %)]}
  (into [] (range n (+ 1 m))))

(defn make-booklet-ranges
  [booklet-no-of-pages total-no-of-pages]
  (->> (partition-all booklet-no-of-pages (make-page-range 1 total-no-of-pages))
       (map #(first-last %))))

(defn print-object-methods
  "Given an object, print its methods."
  [object]
  (->> object
       (r/reflect)
       (:members)
       (print-table)))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn first-last
  [list]
  [(first list) (last list)])

(defn no-of-pages-to-add
  "Calculates how many pages need to be added till the end of PDF
   document - `n` so that it is divisible by `m` without remainder."
  [n m]
  {:pre [(pos? n), (pos? m)]}
  (if (= (mod n m) 0)
    0
    (- m (mod n m))))
