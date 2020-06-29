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
  [page-start page-end]
  (loop [first-page page-start
         pages-to-print (+ (- page-end page-start) 1)
         front-offset 0
         back-offset 1
         pages-in-order [page-end]]
    (if (>= (count pages-in-order) pages-to-print)
      (->> pages-in-order
           (take pages-to-print) ; take only pages you want to print
           (partition-all 2))
      (recur
       first-page
       pages-to-print
       (+ front-offset 2)
       (+ back-offset 2)
       (conj pages-in-order
             (+ first-page front-offset)
             (+ first-page front-offset 1)
             (- (+ first-page pages-to-print) 1 back-offset)
             (- (+ first-page pages-to-print) 1 back-offset 1))))))

(defn get-desired-page-order
  [no-of-pages-in-booklet total-no-of-pages]
  (let [booklet-ranges (common/make-booklet-ranges no-of-pages-in-booklet total-no-of-pages)]
    (->> booklet-ranges
         (map #(booklet-page-order (first %) (last %)))
         flatten)))

(defn make-whole-book
  [pdf-file-in pdf-file-out booklet-size & {:keys [no-of-files]}]
  (interop/rearrange-pages-pdf pdf-file-in
                               (->> (get-desired-page-order booklet-size (interop/get-number-of-pages pdf-file-in))
                                    (map #(dec %)))
                               pdf-file-out))


(defn make-book
  [pdf-in dir-out booklet-length]
  (let [pdf-in pdf-in
        dir-out dir-out
        total-no-of-pages (interop/get-number-of-pages pdf-in)
        booklets (->> (get-desired-page-order booklet-length total-no-of-pages)
                      (map #(dec %))
                      (partition-all 2))
        f-names (for [x (range 100 (+ 100 (count booklets)))] (str dir-out "/" x ".pdf"))]
    (->> (pmap #(interop/arrange-pages-side-by-side pdf-in (first %1) (second %1) %2) booklets f-names)
         frequencies)))
