(defproject borsuk "0.0.1"
  :description "A ClojureScript Riemann dashboard"
  :dependencies [[com.cemerick/url "0.1.1"]
                 [figwheel "0.2.5-SNAPSHOT"]
                 [org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2850"]
                 [org.clojure/core.async "0.1.256.0-1bf8cf-alpha"]
                 [org.omcljs/om "0.8.8"]
                 [prismatic/om-tools "0.3.10"]]

  :node-dependencies [[source-map-support "0.2.8"]]

  :plugins [[lein-cljsbuild "1.0.4"]
            [lein-npm "0.4.0"]]

  :source-paths ["src" "target/classes"]

  :clean-targets ["out" "out-adv"]

  :cljsbuild {:builds [{:id "dev"
                        :source-paths ["src"]
                        :compiler {:main borsuk.core
                                   :output-to "resources/public/js/compiled/borsuk.js"
                                   :output-dir "resources/public/js/compiled/out"
                                   :optimizations :none
                                   :cache-analysis true
                                   :asset-path "js/compiled/out"
                                   :source-map true}}]}

  :figwheel {:http-server-root "public"
             :css-dirs ["resources/public/css"]
             :nrepl-port 7888})
