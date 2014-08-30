(ns grimoire.datastore
  (:refer-clojure :exclude [replace])
  (:require [clojure.java.io :as io]
            [grimoire.util :refer :all]
            [clojure.string :refer [lower-case upper-case replace]]))

;; Intended file structure output
;;--------------------------------------------------------------------
;; /$GROUPID/$ARTIFACT/$VERSION/release-notes.md
;; /$GROUPID/$ARTIFACT/$VERSION/ns/$NAMESPACE/ns-notes.md
;; /$GROUPID/$ARTIFACT/$VERSION/ns/$NAMESPACE/sym/$SYMBOL/name.txt
;; /$GROUPID/$ARTIFACT/$VERSION/ns/$NAMESPACE/sym/$SYMBOL/type.txt
;; /$GROUPID/$ARTIFACT/$VERSION/ns/$NAMESPACE/sym/$SYMBOL/arities.txt
;; /$GROUPID/$ARTIFACT/$VERSION/ns/$NAMESPACE/sym/$SYMBOL/docstring.md
;; /$GROUPID/$ARTIFACT/$VERSION/ns/$NAMESPACE/sym/$SYMBOL/extended-docstring.md
;; /$GROUPID/$ARTIFACT/$VERSION/ns/$NAMESPACE/sym/$SYMBOL/source.clj
;; /$GROUPID/$ARTIFACT/$VERSION/ns/$NAMESPACE/sym/$SYMBOL/related.txt
;; /$GROUPID/$ARTIFACT/$VERSION/ns/$NAMESPACE/sym/$SYMBOL/ex/$EXAMPLE_ID.clj

(defn file->ns [fpath]
  (-> fpath
      (replace #".clj$" "")
      (replace #"_" "-")
      (replace #"/" ".")))

(defn write-docs
  [groupid artifactid version
   {:keys [namespace raw-symbol symbol type arglists doc src examples related]}]
  (let [sym-dir (io/file groupid artifactid version "ns" namespace "sym" raw-symbol)]
    (.mkdir sym-dir)

    ;; write the name file
    (let [name-file (io/file sym-dir "name.txt")]
      (when-not (.exists name-file)
        (spit name-file raw-symbol)))

    ;; write type file
    (let [type-file (io/file sym-dir "type.txt")]
      (when-not (.exists type-file)
        (spit type-file type)))

    ;; write arities file
    (let [arities-file (io/file sym-dir "arities.txt")]
      (when-not (.exists arities-file)
        (doseq [l arglists]
          (spit arities-file
                (str l "\n")
                :append true))))

    ;; write docstring file
    (let [doc-file (io/file sym-dir "docstring.md")]
      (when-not (.exists doc-file)
        (spit doc-file doc)))

    ;; touch extended docstring
    (let [extended-doc-file (io/file sym-dir "extended-docstring.md")]
      (when-not (.exists extended-doc-file)
        (spit extended-doc-file "No user documentation! Care to add some?\n")))

    ;; write source file
    (when src
      ;; write source file
      (let [src-file (io/file sym-dir "source.clj")]
        (when-not (.exists src-file)
          (spit src-file src))))

    ;; write related file
    (let [related-file (io/file sym-dir "related.txt")]
      (when-not (.exists related-file)
        (when related
          (doseq [{:keys [file name] :as el} @related]
            (let [file (or file "clojure/core.clj")]
              (spit related-file
                    (str (file->ns file) "/" name "\n")
                    :append true)))
          (spit related-file ""))))

    ;; write examples from clojuredocs
    (let [ex-dir (io/file sym-dir "examples")]
      (when-not (.exists ex-dir)
        (.mkdir ex-dir)

        (when examples
          (doseq [{:keys [body] :as e} @examples]
            (let [fname (str (Math/abs (hash body)) ".clj")
                  f     (io/file ex-dir fname)]
              (spit f (-> body (replace #"</?pre>" ""))))))))))