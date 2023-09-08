(ns com.github.clojure-lsp.intellij.extension.definition
  (:gen-class
   :name com.github.clojure_lsp.intellij.extension.Definition
   :extends com.intellij.codeInsight.navigation.actions.GotoDeclarationHandlerBase)
  (:require
   [clojure.string :as string]
   [com.github.clojure-lsp.intellij.client :as lsp-client]
   [com.github.clojure-lsp.intellij.db :as db]
   [com.github.clojure-lsp.intellij.editor :as editor]
   [com.github.clojure-lsp.intellij.file-system :as file-system]
   [com.github.clojure-lsp.intellij.psi :as psi]
   [com.github.ericdallo.clj4intellij.app-manager :as app-manager])
  (:import
   [com.intellij.openapi.editor Document Editor]
   [com.intellij.openapi.project Project]
   [com.intellij.openapi.util TextRange]
   [com.intellij.openapi.vfs VirtualFile]
   [com.intellij.psi PsiElement]
   [com.intellij.psi PsiElement]))

(set! *warn-on-reflection* true)

(defn ^:private definition->psi-element
  [^VirtualFile v-file ^Project project definition]
  @(app-manager/invoke-later!
    {:invoke-fn
     (fn []
       (let [editor ^Editor (editor/v-file->editor v-file project)
             document ^Document (.getDocument editor)
             {:keys [range]} definition
             start ^int (editor/position->point (:start range) document)
             end ^int (editor/position->point (:end range) document)
             name (.getText document (TextRange. start end))]
         (psi/->LSPPsiElement name project (editor/virtual->psi-file v-file project) start end)))}))

(defn -getGotoDeclarationTarget [_ ^PsiElement element ^Editor editor]
  (when-let [client (and (= :connected (:status @db/db*))
                         (:client @db/db*))]
    (let [[line character] (:start (editor/text-range->range (.getTextRange element) editor))
          project ^Project (.getProject editor)]
      (when-let [definition @(lsp-client/request! client [:textDocument/definition
                                                          {:text-document {:uri (editor/editor->uri editor)}
                                                           :position {:line line
                                                                      :character character}}])]
        (let [{:keys [uri]} definition]
          (if (string/starts-with? uri "jar:")
            (let [text @(lsp-client/request! client [:clojure/dependencyContents {:uri uri}])
                  sub-path (last (re-find #"^(jar|zip):(file:.+)!(/.+)" uri))
                  ;; TODO FIXME not working when needs to call create-temp-file
                  v-file (file-system/create-temp-file project sub-path text)]
              (definition->psi-element v-file project definition))
            (definition->psi-element (editor/uri->v-file uri) project definition)))))))
