# honeysql-talk

Source code and examples for a talk about HoneySQL that I gave to Chicago Clojure on 2016-07-20.

## Usage

### Generate the Presentation

`lein run -m honeysql-talk.slides` will generate the static HTML presentation. Visit `resources/index.html` to view it.

### HoneySQL Examples

There are some tests showing how HoneySQL can be used to solve the basic exercises from pgexercises.com.

### HoneySQL Utilities

There exist some utility functions that I have found useful in `honeysql-talk.extras`.

### PG Docker and REPL

To start up a postgres instance (you'll need to stop any other local postgres instances):

```bash

cd docker
docker build -t "honeysql-talk:db" .
docker run --name honeysql-talk -e POSTGRES_USER=honeysql -e POSTGRES_PASSWORD=honeysql -d -p 5432:5432 honeysql-talk:db

```

Start a repl (maybe use CIDER or `lein repl`)

```clj

;; get to the demo repl
(require 'honeysql-talk.repl)
(in-ns 'honeysql-talk.repl)

;; run some queries
(query-runner db (-> (select :*)
                     (from :cd.members)
                     (limit 1)))
```

## License

### Presentation
Copyright © 2016 Brandon Adams <emidln@gmail.com> Clojure code distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.

### SQL Exercises
Copyright © 2016 Alisdar Owens <alisdair@zaltys.net>. SQL taken from pgexercises.com licensed under CC BY-SA 3.0 (http://creativecommons.org/licenses/by-sa/3.0/).

### RevealJS
Copyright (C) 2016 Hakim El Hattab, http://hakim.se

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
