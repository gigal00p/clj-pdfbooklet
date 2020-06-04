(ns clj-pdfbooklet.core
  (:gen-class)
  (:require [clojure.reflect :as r]
            [clojure.string :as str]
            [clojure.tools.cli :refer [parse-opts]]
            [taoensso.timbre :as timbre :refer [log trace debug info warn error errorf fatal report]]
            [clojure.java.io :as io])
  (:use [clojure.pprint :only [print-table]])
  (:import java.io.File
           org.apache.pdfbox.pdmodel.PDDocument
           org.apache.pdfbox.multipdf.Splitter))

(defn exit [status msg]
  (println msg)
  (System/exit status))

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
      (do (error (str "Passed path: `" path "` is not a directory"))))))

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

(defn get-number-of-pages
  "Try get the number of pages of PDF file.
   Returns int on success or nil on failure."
  [pdf-file-or-path]
  (try
    (let [^File pdf-file (io/as-file pdf-file-or-path)
          doc (PDDocument/load pdf-file)
          no-of-pages (.getNumberOfPages doc)]
      (.close doc)
        no-of-pages)
       (catch Exception e
         (timbre/errorf "Something went wrong while parsing `%s` message: %s" pdf-file-or-path (.getMessage ^Exception e))
         nil)))

(defn no-of-pages-to-add
  "Calculates how many pages need to be added till the end of PDF
   document - `n` so that it is divisible by `m` without remainder."
  [n m]
  {:pre [(pos? n), (pos? m)]}
  (if (= (mod n m) 0)
    0
    (- m (mod n m))))

(defn first-last
  [list]
  [(first list)(last list)])

(def pdf-input "c:/Users/walki/Downloads/pdf_test/input.pdf")
(def pdf-output "c:/Users/walki/Downloads/pdf_test/wynik_1.pdf")

(defn print-object-methods
  [object]
  (->> object
       (r/reflect)
       (:members)
       (print-table)))

(defn split-pdf-page
  [pdf-file-or-path start end file-output-name]
  (try
    (let [^File       pdf-file (io/as-file pdf-file-or-path)
          ^PDDocument doc (PDDocument/load pdf-file)
          ^Splitter   splitter (Splitter.)]
      (.setStartPage splitter start)
      (.setEndPage splitter end)
      (.setSplitAtPage splitter end)
      (-> (.split splitter doc) 
          first
          (.save (io/as-file file-output-name)))
      (.close doc))
    (catch Exception e
      (timbre/errorf "Something went wrong while splitting `%s` message: %s" pdf-file-or-path (.getMessage ^Exception e)))))
      

(defn make-booklet-ranges
  [booklet-no-of-pages total-no-of-pages]
  (->> (partition-all booklet-no-of-pages (make-page-range 1 total-no-of-pages))
       (map #(first-last %))))

; (make-booklet-ranges 16 (get-number-of-pages pdf-input))

(defn split-whole-book
  "Split pdf on multiple smaller files"
  [fname-in fname-out booklet-length]
  (let [base-name (first (clojure.string/split fname-in #".pdf"))
        ranges (make-booklet-ranges booklet-length (get-number-of-pages fname-in))
        f-names (for [x (range 100 (+ 100 (count ranges)))] (str fname-out "/" x ".pdf"))]
    (doall (pmap #(split-pdf-page fname-in (first %1) (last %1) %2) ranges f-names))))

(def cli-options
  [["-i" "--input-pdf PDF" "PDF document you want to split"]
   ["-o" "--output-dir DIR" "Directory where splitted documents will be written"]
   ["-s" "--size CHUNK" "Size of the chunk" :parse-fn #(Integer/parseInt %) :default 16]
   ["-h" "--help"]])


(defn help [options]
  (->> ["clj-pdfbooklet is a command line tool for splitting pdf documents into smaller pdf docs."
        ""
        "Usage: java -jar clj-pdfbooklet-0.1.0-SNAPSHOT-standalone.jar [options]"
        ""
        "Options:"
        options
        ""]
       (str/join \newline)))

; (-main "--input-pdf" "c:/Users/walki/Sync/SHARED/books/Site Reliability Engineering.pdf" "--output-dir" "c:/Users/walki/Downloads" "--size" "256")

(defn -main
  [& args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (cond
      (:help options) (exit 0 (help summary))
      (not= (count options) 3) (prn (str "Not enough options provided, usage:\n\n" (help summary)))
      (not= (count errors) 0) (prn (str "CLI arguments parsing failed, usage:\n\n" (help summary)))
      :else
      (try
        (let [input-doc (->> options :input-pdf)
              output-dir (->> options :output-dir)
              chunk-size (->> options :size)]
          (split-whole-book input-doc output-dir chunk-size)
          ;(shutdown-agents)
          ) ; shutdown agents thread pool so that program exits quickly
        (catch Exception e
          (timbre/errorf "Something went wrong: %s" (.getMessage ^Exception e)))))))
