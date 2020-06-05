(ns clj-pdfbooklet.transform
  (:require [clj-pdfbooklet.interop :as interop]
            [clj-pdfbooklet.common :as common]))

(defn split-whole-book
  "Split pdf on multiple smaller files."
  [fname-in fname-out booklet-length]
  (let [ranges (common/make-booklet-ranges booklet-length (interop/get-number-of-pages fname-in))
        f-names (for [x (range 100 (+ 100 (count ranges)))] (str fname-out "/" x ".pdf"))]
    (doall (pmap #(interop/split-pdf-page fname-in (first %1) (last %1) %2) ranges f-names))))

(defn booklet-page-order
  "Returns actual bookled pages order."
  [pages-to-print]
  (loop [first-page 1
         front-offset 0
         back-offset 1
         pages-in-order [pages-to-print]]
    (if (>= (count pages-in-order) pages-to-print)
      (->> pages-in-order
           (take pages-to-print) ; take only pages you want to print
           (into []))
      (recur
       first-page
       (+ front-offset 2)
       (+ back-offset 2)
       (conj pages-in-order
             (+ first-page front-offset)
             (+ first-page front-offset 1)
             (- (+ first-page pages-to-print) 1 back-offset)
             (- (+ first-page pages-to-print) 1 back-offset 1))))))
