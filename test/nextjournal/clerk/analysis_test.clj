(ns nextjournal.clerk.analysis-test
  (:require [clojure.test :refer :all]
            [matcher-combinators.test :refer [match?]]
            [matcher-combinators.matchers :as m]
            [nextjournal.clerk.analysis :as a]
            [weavejester.dependency :as dep]))


(defmacro with-ns-binding [ns-sym & body]
  `(binding [*ns* (find-ns ~ns-sym)]
     ~@body))


(deftest no-cache?
  (testing "are variables set to no-cache?"
    (is (not (a/no-cache? '(rand-int 10))))
    (is (not (a/no-cache? '(def random-thing (rand-int 1000)))))
    (is (not (a/no-cache? '(defn random-thing [] (rand-int 1000)))))
    (is (a/no-cache? '(def ^:nextjournal.clerk/no-cache random-thing (rand-int 1000))))
    (is (a/no-cache? '(defn ^:nextjournal.clerk/no-cache random-thing [] (rand-int 1000))))
    (is (a/no-cache? '(defn ^{:nextjournal.clerk/no-cache true} random-thing [] (rand-int 1000))))
    (is (not (a/no-cache? '[defn ^:nextjournal.clerk/no-cache trick [] 1]))))

  (testing "is evaluating namespace set to no-cache?"
    (is (not (a/no-cache? '(rand-int 10))))

    (with-ns-binding 'nextjournal.clerk.analysis
      (is (a/no-cache? '(rand-int 10))))))

(deftest var-dependencies
  (is (match? #{'clojure.string/includes?
                'rewrite-clj.parser/parse-string-all}
              (a/var-dependencies '(defn foo
                                     ([] (foo "s"))
                                     ([s] (clojure.string/includes? (rewrite-clj.parser/parse-string-all s) "hi")))))))

(deftest analyze
  (testing "quoted forms aren't confused with variable dependencies"
    (is (match? {:deps #{`inc}}
                (a/analyze '(do inc))))
    (is (empty? (:deps (a/analyze '(do 'inc))))))

  (testing "locals that shadow existing vars shouldn't show up in the deps"
    (is (empty? (:deps (a/analyze '(let [+ 2] +))))))

  (testing "symbol referring to a java class"
    (is (match? {:deps       #{'io.methvin.watcher.PathUtils}}
                (a/analyze 'io.methvin.watcher.PathUtils))))

  (testing "namespaced symbol referring to a java thing"
    (is (match? {:deps       #{'io.methvin.watcher.hashing.FileHasher/DEFAULT_FILE_HASHER}}
                (a/analyze 'io.methvin.watcher.hashing.FileHasher/DEFAULT_FILE_HASHER))))

  (is (match? {:ns-effect? false
               :var 'nextjournal.clerk.analysis/foo
               :deps       #{'rewrite-clj.parser/parse-string-all
                             'clojure.string/includes?}}
              (with-ns-binding 'nextjournal.clerk.analysis
                (a/analyze '(defn foo [s]
                              (clojure.string/includes? (rewrite-clj.parser/parse-string-all s) "hi"))))))

  (is (match? {:ns-effect?   false
               :var 'nextjournal.clerk.analysis/segments
               :deps         #{'clojure.string/split
                               'clojure.string/join}}
              (with-ns-binding 'nextjournal.clerk.analysis
                (a/analyze '(defn segments [s] (let [segments (clojure.string/split s)]
                                                 (clojure.string/join segments)))))))

  (is (match? {:form       '(in-ns 'user)
               :ns-effect? true
               :deps       #{'clojure.core/in-ns}}
              (a/analyze '(in-ns 'user))))

  (is (match? {:ns-effect? true
               :deps       (m/embeds #{'clojure.core/in-ns})}
              (a/analyze '(do (ns foo)))))

  (is (match? {:ns-effect? false
               :deps       #{'clojure.core/inc}}
              (a/analyze '(def my-inc inc)))))


(deftest symbol->jar
  (is (a/symbol->jar 'io.methvin.watcher.PathUtils))
  (is (a/symbol->jar 'io.methvin.watcher.PathUtils/cast))
  (testing "does not resolve jdk builtins"
    (is (not (a/symbol->jar 'java.net.http.HttpClient/newHttpClient)))))


(deftest analyze-file
  (is (match? (m/equals
               {:graph {:dependencies {'(ns example-notebook) set?}
                        :dependents   map?}
                :doc {:doc [{:type :code
                             :text "^:nextjournal.clerk/no-cache\n(ns example-notebook)"
                             :ns?  true}
                            {:type :markdown
                             :doc  {:type    :doc
                                    :content [{:type :heading
                                               :content [{:type :text, :text "📶 Sorting"}]
                                               :heading-level 1}]
                                    :toc     {:type :toc
                                              :content
                                              [{:level 1,
                                                :type :toc
                                                :title "📶 Sorting"
                                                :node
                                                {:type :heading
                                                 :content [{:type :text, :text "📶 Sorting"}]
                                                 :heading-level 1}
                                                :path [:content 0]}]}}}
                            {:type :code
                             :text "#{3 1 2}"}]
                      :visibility #{:show}}
                :->analysis-info {'(ns example-notebook) {:file "resources/tests/example_notebook.clj",
                                                          :form '(ns example-notebook),
                                                          :deps set?}
                                  #{1 3 2} {:file "resources/tests/example_notebook.clj",
                                            :form '#{1 3 2},
                                            :deps nil}}})
              (a/analyze-file {:markdown? true}
                              {:graph (dep/graph)}
                              "resources/tests/example_notebook.clj"))))
