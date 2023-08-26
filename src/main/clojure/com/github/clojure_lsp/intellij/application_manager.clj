(ns com.github.clojure-lsp.intellij.application-manager
  (:import
   [com.intellij.openapi.application ApplicationManager ModalityState]
   [com.intellij.openapi.command CommandProcessor]
   [com.intellij.openapi.command UndoConfirmationPolicy]
   [com.intellij.openapi.project Project]
   [com.intellij.openapi.util Computable]))

(defn invoke-later! [invoke-fn]
  (let [p (promise)]
    (.invokeLater
     (ApplicationManager/getApplication)
     (reify Runnable
       (run [_]
         (deliver p (invoke-fn))))
     (ModalityState/any))
    p))

(defn write-action! [run-fn]
  (let [p (promise)]
    (.runWriteAction
     (ApplicationManager/getApplication)
     (reify Computable
       (compute [_]
         (let [result (run-fn)]
           (deliver p result)
           result))))
    p))

(defn execute-command! [^String name ^Project project command-fn]
  (let [p (promise)]
    (.executeCommand
     (CommandProcessor/getInstance)
     project
     (reify Runnable
       (run [_]
         (let [result (command-fn)]
           (deliver p result)
           result)))
     name
     "Clojure LSP"
     UndoConfirmationPolicy/DEFAULT
     false)
    p))