(ns clj-pdfbooklet.core
  (:gen-class))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))


(defn make-page-range
  "Make vector of ints from n to m inclusive.
   n,m needs to be positive intiger.
   n needs to be < than m."
  [n m]
  {:pre  [(pos? n), (pos? m), (> m n)]
   :post [(vector? %)]}
  (into [] (range n (+ 1 m))))
