(ns markright.parser
  (:require [om.next :as om :refer-macros [defui]]
            [electron.ipc :as ipc]
            ))

(def app-state (atom {:app/text "## Welcome to MarkRight\n\n![logo][1]\n\nThis is a minimalistic GFM markdown editor written in om.next.\n\nChanges to the document will be reflected in real time on the right ->\n\nPerfect for writing READMEs :)\n\n\n[1]: https://raw.githubusercontent.com/dvcrn/markright/master/resources/markright-banner.png" :app/force-overwrite false :app/filepath "" :app/saved-text "nil"}))

(defmulti read om/dispatch)
(defmethod read :default
  [{:keys [state]} k _]
  (let [st @state]
    (find st k)
    (if-let [[_ v] (find st k)]
      {:value v}
      {:value :not-found})))

(defmulti mutate om/dispatch)
(defmethod mutate 'app/html
  [{:keys [state]} _ {:keys [html]}]
  {:value [:app/html]
   :action #(swap! state assoc-in [:app/html] html)})

(defmethod mutate 'app/text
  [{:keys [state]} _ {:keys [text]}]
  {:value [:app/text]
   :action (swap! state assoc-in [:app/text] text)})

(defmethod mutate 'app/transact-overwrite
  [{:keys [state]} _ {:keys [text]}]
  {:value [:app/force-overwrite]
   :action (swap! state assoc-in [:app/force-overwrite] false)})

(def reconciler
  (om/reconciler
    {:state app-state
     :parser (om/parser {:read read :mutate mutate :force-overwrite false})}))

(defmethod ipc/process-call :get-current-file
  [msg reply]
  (reply (get @app-state :app/filepath)))

(defmethod ipc/process-call :get-current-content
  [msg reply]
  (reply (get @app-state :app/text)))

(defmethod ipc/process-cast :load-file
  [{:keys [file content]}]
  (swap! app-state assoc
    :app/force-overwrite true
    :app/text content
    :app/saved-text content
    :app/filepath file))

(defmethod ipc/process-cast :set-current-file
  [{:keys [file content]}]
  (swap! app-state assoc
         :app/saved-text content
         :app/filepath file))

(comment
  (let [content]
    (if (nil? state-path)
      (save-file-as!)
      (do
        (swap! parser/app-state assoc :app/saved-text content)))))
