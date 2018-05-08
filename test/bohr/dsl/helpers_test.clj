(ns bohr.dsl.helpers-test
  (:require [clojure.test :refer :all]
            [clojure.string :as string]
            [bohr.dsl.helpers :refer :all]))

(deftest http-get-test
	(testing "HTTP GET should GET a URL and return the string"
		(is
			(= 
				(http-get "https://jsonplaceholder.typicode.com/posts/1")
				"{\n  \"userId\": 1,\n  \"id\": 1,\n  \"title\": \"sunt aut facere repellat provident occaecati excepturi optio reprehenderit\",\n  \"body\": \"quia et suscipit\\nsuscipit recusandae consequuntur expedita et cum\\nreprehenderit molestiae ut ut quas totam\\nnostrum rerum est autem sunt rem eveniet architecto\"\n}"
				))))

(deftest http-get-json-test
	(testing "HTTP GET should GET a URL and return the json"
		(is
			(= 
				(get (http-get-json "https://jsonplaceholder.typicode.com/posts/1") :title)
				"sunt aut facere repellat provident occaecati excepturi optio reprehenderit"
				))))


(deftest http-post-test
	(testing "HTTP POST should POST json to a URL and return the string"
		(is
			(= 
				(http-post "https://jsonplaceholder.typicode.com/posts" {:title "foo" :body "bar" :userId 1} )
				"{\n  \"title\": \"foo\",\n  \"body\": \"bar\",\n  \"userId\": 1,\n  \"id\": 101\n}" 
				))))

(deftest http-post-json-test
	(testing "HTTP POST should POST json to a URL and return json"
		(is
			(= 
				(get (http-post-json "https://jsonplaceholder.typicode.com/posts" {:title "foo" :body "bar" :userId 1} ) :title)
				"foo"
				))))
