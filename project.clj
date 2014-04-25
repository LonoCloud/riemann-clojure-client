(defproject riemann-clojure-client "0.2.9"
  :description "Clojure client for the Riemann monitoring system"
  :url "https://github.com/aphyr/riemann-clojure-client"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.0"]
                 [org.clojure/tools.logging "0.2.3"]
                 [lonocloud/riemann-java-client "0.2.9"]]
  :plugins [[s3-wagon-private "1.1.2"]]
  :repositories [["lonocloud-releases" {:url "s3://lonocloud-maven/repo/releases/"
                                        :username :env
                                        :passphrase :env
                                        :sign-releases false}
                  "lonocloud-snapshots" {:url "s3://lonocloud-maven/repo/snapshots/"
                                         :username :env
                                         :passphrase :env
                                         :sign-releases false}]])
