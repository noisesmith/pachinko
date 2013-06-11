(defproject clicky-game "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [aleph "0.3.0-rc1"]
                 [org.clojure/tools.nrepl "0.2.3"]
                 [cheshire "5.2.0"]
                 [noisesmith.pachinko "1.0"]]
  :main clicky-game.server/init)
