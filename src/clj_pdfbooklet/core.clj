(ns clj-pdfbooklet.core
  (:gen-class)
  (:require [clojure.string :as str]
            [clojure.tools.cli :refer [parse-opts]]
            [taoensso.timbre :as timbre :refer [errorf]]
            [clj-pdfbooklet.common :as common]
            [clj-pdfbooklet.transform :as transform]))

;(def pdf-input "c:/Users/walki/Downloads/some_pdf.pdf")
;(def pdf-output "c:/Users/walki/Downloads/pdf_test/wynik_1.pdf")

(def cli-options
  [["-i" "--input-pdf PDF" "PDF document you want to split"]
   ["-o" "--output-dir DIR" "Directory where splitted documents will be written"]
   ["-s" "--size CHUNK" "Size of the chunk" :parse-fn #(Integer/parseInt %) :default 16]
   ["-h" "--help"]])

(defn help [options]
  (->> ["clj-pdfbooklet is a command line tool for making pdfbooklets ready to print."
        ""
        "Usage: java -jar clj-pdfbooklet-0.1.0-SNAPSHOT-standalone.jar [options]"
        ""
        "Options:"
        options
        ""]
       (str/join \newline)))

; (make-booklet-ranges 16 (get-number-of-pages pdf-input))
; (-main "--input-pdf" "c:/Users/walki/Sync/SHARED/books/Site Reliability Engineering.pdf" "--output-dir" "c:/Users/walki/Downloads" "--size" "256")

(defn -main
  [& args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (cond
      (:help options) (common/exit 0 (help summary))
      (not= (count options) 3) (prn (str "Not enough options provided, usage:\n\n" (help summary)))
      (not= (count errors) 0) (prn (str "CLI arguments parsing failed, usage:\n\n" (help summary)))
      :else
      (try
        (let [input-doc (->> options :input-pdf)
              output-dir (->> options :output-dir)
              chunk-size (->> options :size)
              no-of-success-files (-> (transform/split-whole-book input-doc output-dir chunk-size)
                                      frequencies
                                      (get true))]
          (println (str "Successfully produced " no-of-success-files " files."))
          ;; (shutdown-agents)
          ) ; shutdown agents thread pool so that program exits quickly
        (catch Exception e
          (timbre/errorf "Something went wrong: %s" (.getMessage ^Exception e)))))))
