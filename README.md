http-perf-tool
==============

This tool runs N simulated users which can send configurable http requests and
handle responses.

Running
-------

```
java -jar http-perf-tool-0.1-jar-with-dependencies.jar <javascript-files>
```

Usually <javascript-files> are 2 files: one with static configuration
(*.conf.js) and one with dynamic one (*.run.js)

For example:

```
java -jar http-perf-tool-0.1-jar-with-dependencies.jar test.conf.js test.run.js
```


Configuration
-------------

Configuration is done with the help of Javascript files.

### Connection

```javascript
connection = {
    url: 'http://localhost:8888/test',
    mandatoryHttpParams: { sessionid: 'user.sessionid' },
    retrieveLinks: true,
    contentCached: true,
}
```

Where
-   mandatoryHttpParams - http parameters which are added to each request
-   retrieveLinks - are links (images, css, js files, etc) from a page retrieved
-   contentCached - are appropriate http headers added to not retrieve the same
    page again in the same session

### Events

Currently, only 2 events supported: onStart and onStop, which should refer to
some actions (see below).

### Mapping

Mapping describes actions which corresponds to one or several http requests
(with parameters), response handlers (which executed *before* and *after*
request), and additional links to retrieve (if these links could not be easily
deducted from a page cntent, like complex way of loading Javascript files).

For example:

```javascript
login: {
    uri: 'login',
    method: 'post',
    params: { username: 'user.name' , password: 'user.password' },
    handlers: {
        after: function() {
            user.authenticated = (content == 'Session created');
            user.sessionid='1234';
            println(user.name + ' user ' + (user.authenticated ? '' : 'not ') +
            'authenticated');
        }
}
```

Please note, each parameter can use implicit "user" object, which fields could
be set in hanlders. Each *after* handler has implicit content variable (which
corresponds to retrieved content) and JQuery like object (which helps to find
content in a html page):

```javascript
after: function() {
	println("name('inp1').val()= " + $.name('inp1').val());
	println("id('state').text()= " + $.id('state').text());
}
```

Currenly only id(), name(), text(), and val() functions supported.

### Statistics

You can collect statistics like successful and failed actions or http requests,
but also cpu/memory/thread information of this tool and remote Java servers.

```javascript
stat = {
    enabled: true,
    local: true,
    remote: { localhost: 9999 },
    http: false,
    actions: false,
    plot: true
}
```

Where:
-   local - is cpu/memory/thread statistics collected for this tool
-   remote - is cpu/memory/thread statistics collected for remote Java side
-   http - is statistics collected for http requests
-   actions - is statistics collected for actions
-   plot - are plots generated when the tool finishes

Please note, to enable remote statistics you should add following JVM parameters
to remote Java process:

```
-Dcom.sun.management.jmxremote
-Dcom.sun.management.jmxremote.port=9999
-Dcom.sun.management.jmxremote.authenticate=false
-Dcom.sun.management.jmxremote.ssl=false
```

### Users

The tool runs N users, which are divided into groups. Each group has "users" and
"run" sections. "users" section contains either just a list of users, or a
pattern which describes a group of users. In the example below, "test_00"
pattern (where each zero correspond to number) is populated into test_01,
test_02, test_03. Each field (except of "pattern", "first" and "last") is used
in consequent actions as fields of "user" object. "run" section is a javascript
function, which calls actions from "mapping" definition (see above).

For example:

```javascript
groups = []

groups[0] = {
	users: [
		{ name: 'tes', password: 'fail', language: 'ru' },
		{ name: 'test', password: 'password', language: 'en' },
		{ pattern: true, name: 'test_00', password: 'pass_00', language: 'en', first: 1, last: 3 }
	],
	run: function() {
		setState123();
	},
}
```

Please note, you can call Java like in:

```javascript
run: function() {
	for (var i = 0; i < 100; i++) {
		java.lang.Thread.sleep(100);
		setState({ state: 'state' + i });
	}
}
```