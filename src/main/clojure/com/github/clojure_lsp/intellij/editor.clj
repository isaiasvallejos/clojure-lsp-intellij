(ns com.github.clojure-lsp.intellij.editor
  (:require
   [clojure-lsp.shared :as lsp.shared]
   [com.github.clojure-lsp.intellij.editor :as editor]
   [com.github.ericdallo.clj4intellij.app-manager :as app-manager]
   [com.github.ericdallo.clj4intellij.util :as util])
  (:import
   [com.intellij.openapi.editor Document Editor]
   [com.intellij.openapi.fileEditor
    FileDocumentManager]
   [com.intellij.openapi.project Project ProjectLocator]
   [com.intellij.openapi.util TextRange]
   [com.intellij.openapi.util.text StringUtil]
   [com.intellij.openapi.vfs VirtualFile]
   [com.intellij.psi PsiFile]
   [com.intellij.psi PsiManager]))

(set! *warn-on-reflection* true)

(defn offset->cursor-position [^Editor editor offset]
  (let [text (.getCharsSequence (.getDocument editor))
        line-col (StringUtil/offsetToLineColumn text offset)]
    [(.line line-col) (.column line-col)]))

(defn position->offset [text line character]
  (StringUtil/lineColToOffset text line character))

(defn document+position->offset ^Integer [{:keys [line character]} ^Document document]
  (position->offset (.getText document) line character))

(defn range->text-range ^TextRange [range ^Document document]
  (TextRange/create (document+position->offset (:start range) document)
                    (document+position->offset (:end range) document)))

(defn text-range->range [^TextRange range ^Editor editor]
  {:start (offset->cursor-position editor (.getStartOffset range))
   :end (offset->cursor-position editor (.getEndOffset range))})

(defn uri->psi-file ^PsiFile [^String uri ^Project project]
  (.findFile (PsiManager/getInstance project)
             (util/uri->v-file uri)))

(defn editor->uri [^Editor editor]
  ;; TODO sanitize URL, encode, etc
  (.getUrl (.getFile (FileDocumentManager/getInstance) (.getDocument editor))))

(defn filename->project-relative-filename [filename ^Project project]
  (lsp.shared/relativize-filepath
   filename
   (.getBasePath project)))

(defn virtual->psi-file ^PsiFile [^VirtualFile v-file ^Project project]
  (.findFile (PsiManager/getInstance project) v-file))

(defn v-file->project ^Project [^VirtualFile v-file]
  (.guessProjectForFile (ProjectLocator/getInstance)
                        v-file))

(defn apply-workspace-edit ^Boolean
  [^Project project label move-caret? {:keys [document-changes]}]
  ;; TODO Handle resourceOperations like creating, renaming and deleting files
  ;; TODO Improve to check version to known if file changed
  (app-manager/invoke-later!
   {:invoke-fn
    (fn []
      (app-manager/write-action!
       {:run-fn
        (fn []
          (app-manager/execute-command!
           {:name label
            :project project
            :command-fn
            (fn []
              (doseq [{{:keys [uri]} :text-document
                       :keys [edits]} document-changes
                      :let [editor (util/uri->editor uri project false)
                            document (.getDocument editor)
                            sorted-edits (sort-by (comp #(document+position->offset % document) :start :range) > edits)]]
                (doseq [{:keys [new-text range]} sorted-edits
                        :let [start (document+position->offset (:start range) document)
                              end (document+position->offset (:end range) document)]]
                  (cond
                    (>= end 0)
                    (if (<= (- end start) 0)
                      (.insertString document start new-text)
                      (.replaceString document start end new-text))

                    (= 0 start)
                    (.setText document new-text)

                    (> start 0)
                    (.insertString document start new-text)

                    :else
                    nil)
                  (when move-caret?
                    (.moveToOffset (.getCaretModel editor)
                                   (+ (count new-text) start))))
                (.saveDocument (FileDocumentManager/getInstance) document)))}))}))}))
