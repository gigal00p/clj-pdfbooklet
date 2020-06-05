(ns clj-pdfbooklet.interop
  (:require [clojure.java.io :as io]
            [taoensso.timbre :as timbre :refer [errorf]])
  (:import java.io.File
           org.apache.pdfbox.pdmodel.PDDocument
           org.apache.pdfbox.multipdf.Splitter))

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

(defn split-pdf-page
  "Given pdf-file-or-path extracts pages from start to end and writes to
  file-output-name. Returns true on success."
  [pdf-file-or-path start end file-output-name]
  (try
    (let [^File          pdf-file (io/as-file pdf-file-or-path)
          ^PDDocument    doc (PDDocument/load pdf-file)
          ^Splitter      splitter (Splitter.)
          splitter-start (.setStartPage splitter start)
          splitter-end   (.setEndPage splitter end)
          splitter-at    (.setSplitAtPage splitter end)
          pd-doc         (-> (.split splitter doc) first)]
      (.save pd-doc (io/as-file file-output-name))
      (.close pd-doc)
      (.close doc))
    true ; rerurn true on success
    (catch Exception e
      (timbre/errorf "Something went wrong while splitting `%s` message: %s" pdf-file-or-path (.getMessage ^Exception e)))))
