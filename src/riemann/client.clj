(ns riemann.client
  "Network client for connecting to a Riemann server. Usage:
  
  (def c (tcp-client :host \"monitoring.local\"))
 
  (send-event c {:service \"fridge\" 
                 :state \"running\" 
                 :metric 2.0
                 :tags [\"joke\"]})

  (query c \"tagged \\\"joke\\\"\")
  => [{:service \"fridge\" ... }]

  (close c)

  Clients are mildly resistant to failure; they will attempt to reconnect a
  dropped connection once before giving up. They're backed by a
  RiemannRetryingTcpClient."

  (:import [com.aphyr.riemann.client RiemannRetryingTcpClient 
                                     RiemannUDPClient 
                                     AbstractRiemannClient]
           [java.net InetSocketAddress]
           [java.io IOException])
  (:use clojure.tools.logging))

(defn- unix-time
    "The current unix epoch time in seconds, taken from System/currentTimeMillis."
    []
    (/ (System/currentTimeMillis) 1000))

(defn- decode-pb-event
  "Transforms a java protobuf Event to a map."
  [^com.aphyr.riemann.Proto$Event e]
  (let [rough
        {:host (when (.hasHost e) (.getHost e))
         :service (when (.hasService e) (.getService e))
         :state (when (.hasState e) (.getState e))
         :description (when (.hasDescription e) (.getDescription e))
         :metric (when (.hasMetricF e) (.getMetricF e))
         :tags (when (< 0 (.getTagsCount e)) (vec (.getTagsList e)))
         :time (if (.hasTime e) (.getTime e) (unix-time))
         :ttl (when (.hasTtl e) (.getTtl e))}]
    (select-keys rough (for [[k v] rough :when (not= nil v)] k))))

(defn query
  "Query the server for events in the index. Returns a list of events."
  [^AbstractRiemannClient client string]
  (map decode-pb-event (.query client string)))

(defn send-event
  "Send an event over client."
  ([client event]
   (send-event client event true))
  ([^AbstractRiemannClient client event ack]
   (let [e (.event client)]
     (.host e (:host event))
     (when-let [s (:service event)] (.service e s))
     (when-let [s (:state event)] (.state e s))
     (when-let [d (:description event)] (.description e d))
     (when-let [m (:metric event)] (.metric e (float m)))
     (when-let [^java.util.List t (:tags event)] (.tags e t))
     (when-let [t (:time event)] (.time e (long t)))
     (when-let [t (:ttl event)] (.ttl e (float t)))

     (if ack
       (.sendWithAck e)
       (.send e)))))

(defn tcp-client 
  "Create a new TCP client. Example:

  (tcp-client)
  (tcp-client :host \"foo\" :port 5555)"
  [& { :keys [^String host ^Integer port]
       :or {port 5555
            host "localhost"}
       :as opts}]
  (doto (RiemannRetryingTcpClient. (InetSocketAddress. host port))
    (.connect)))

(defn udp-client
  "Create a new UDP client. Example:

  (udp-client)
  (udp-client :host \"foo\" :port 5555)"
  [& {:keys [^String host ^Integer port]
      :or {port 5555
            host "localhost"}
      :as opts}]
  (doto (RiemannUDPClient. (InetSocketAddress. host port))
    (.connect)))

(defn close-client
  "Close a client."
  [^AbstractRiemannClient client]
  (.disconnect client))

(defn reconnect-client
  "Reconnect a client."
  [client]
  (.reconnect client))
