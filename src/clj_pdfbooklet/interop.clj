(ns clj-pdfbooklet.interop
  (:require [clojure.java.io :as io]
            [taoensso.timbre :as timbre :refer [errorf]]
            [clj-pdfbooklet.common :as common])
  (:import java.io.File
           java.awt.geom.AffineTransform
           org.apache.pdfbox.pdmodel.PDDocument
           org.apache.pdfbox.pdmodel.PDPage
           org.apache.pdfbox.multipdf.Splitter
           org.apache.pdfbox.pdmodel.common.PDRectangle
           org.apache.pdfbox.cos.COSDictionary
           org.apache.pdfbox.multipdf.LayerUtility
           org.apache.pdfbox.cos.COSName
           org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject))

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
  "Given `pdf-file-or-path` extracts pages from `start` to an `end` and
  writes to `file-output-name`. Returns true on success."
  [pdf-file-or-path start end file-output-name]
  (try
    (let [^File       pdf-file (io/as-file pdf-file-or-path)
          ^PDDocument doc      (PDDocument/load pdf-file)
          ^Splitter   splitter (Splitter.)
          splitter-start (.setStartPage splitter start)
          splitter-end   (.setEndPage splitter end)
          splitter-at    (.setSplitAtPage splitter end)
          pd-doc         (-> (.split splitter doc) first)]
      (.save pd-doc (io/as-file file-output-name))
      (.close pd-doc)
      (.close doc))
    true ; return true on success instead of default nil
    (catch Exception e
      (timbre/errorf "Something went wrong while splitting `%s` message: %s"
                     pdf-file-or-path (.getMessage ^Exception e)))))


(defn rearrange-pages-pdf
  "Given `pdf-file-in` and `ordered-page-list` of integers, rearrange
  pages according to that list and write to `pdf-file-out`. Returns
  `true` on success."
  [pdf-file-in ordered-page-list pdf-file-out]
  (try
    (let [^File       pdf-file (io/as-file pdf-file-in)
          ^PDDocument src-doc (PDDocument/load pdf-file)
          ^PDDocument target-doc (PDDocument.)
          ^PDDocument catalog (.getDocumentCatalog src-doc)
          ^PDDocument src-pages (.getPages src-doc)

          extractet-pages (->> ordered-page-list
                               (map #(.get src-pages %)) ; extract pages in order defined in ordered-page-list
                               (into []))

          _ (doseq [page extractet-pages] ; populate new document with extracted pages
              (.addPage target-doc page))]

      (.save target-doc (io/as-file pdf-file-out))
      (.close target-doc)
      (.close src-doc)
      true)
    (catch Exception e
      (timbre/errorf "Something went wrong while rearranging `%s` message: %s"
                     pdf-file-in (.getMessage ^Exception e)))))


(def pdf-in "c:/Users/walki/Sync/SHARED/books/new_books/Node-Javascript/Eloquent_JavaScript.pdf")
(def pdf-out "c:/Users/walki/Downloads/wynik/2.pdf")


; Below function is direct translation of solution from:
; https://stackoverflow.com/questions/12093408/pdfbox-merge-2-portrait-pages-onto-a-single-side-by-side-landscape-page

(defn arrange-pages-side-by-side
  [pdf-file-in page-1-index page-2-index
   pdf-file-out
   ]
  (try
    (let [^File       pdf-file (io/as-file pdf-file-in)
          ^PDDocument src-doc (PDDocument/load pdf-file)
          ^PDDocument target-doc (PDDocument.)
          ^PDDocument catalog (.getDocumentCatalog src-doc)
          ^PDDocument src-pages (.getPages src-doc)

          ordered-page-list [page-1-index page-2-index]

          ;; extract all pages that need to be processed
          extractet-pages (->> ordered-page-list
                                (map #(.get src-pages %))
                                (into []))

          ;; pages to be glued together
          ^PDPage pdf-1-page (first extractet-pages)
          ^PDPage pdf-2-page (second extractet-pages)

          ;; output PDF frames
          ^PDRectangle pdf-1-frame (.getCropBox pdf-1-page)
          ^PDRectangle pdf-2-frame (.getCropBox pdf-2-page)
          ^PDRectangle out-pdf-frame (PDRectangle. (+ (.getWidth pdf-1-frame)
                                                      (.getWidth pdf-2-frame)),
                                                   (max (.getHeight pdf-1-frame)
                                                        (.getHeight pdf-2-frame)))

          ;; output page
          ^COSDictionary dict (COSDictionary.)

          final-dict (doto dict
                       (.setItem COSName/TYPE, COSName/PAGE)
                       (.setItem COSName/MEDIA_BOX, out-pdf-frame)
                       (.setItem COSName/CROP_BOX, out-pdf-frame)
                       (.setItem COSName/ART_BOX, out-pdf-frame))

          ^PDPage out-pdf-page (PDPage. final-dict)
          wynik (.addPage target-doc out-pdf-page)

          ^LayerUtility layer-utility (LayerUtility. target-doc)
          ^PDFormXObject form-pdf-1 (.importPageAsForm layer-utility src-doc pdf-1-page)
          ^PDFormXObject form-pdf-2 (.importPageAsForm layer-utility src-doc pdf-2-page)

          ^AffineTransform af-left (AffineTransform.)
          left-side (.appendFormAsLayer layer-utility out-pdf-page form-pdf-1, af-left, "left")

          ^AffineTransform af-right (AffineTransform/getTranslateInstance (.getWidth pdf-1-frame) 0.0)
          right-side (.appendFormAsLayer layer-utility out-pdf-page form-pdf-2, af-right, "right")
          ]
      (.save target-doc (io/as-file pdf-file-out))
      (.close target-doc)
      (.close src-doc)
      true)
    (catch Exception e
      (timbre/errorf "Something went wrong while merging pages side by side in the file `%s` message: %s"
                     pdf-file-in (.getMessage ^Exception e)))))
