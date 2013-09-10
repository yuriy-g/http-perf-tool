connection = {
	url: 'http://localhost:8888/test',
	mandatoryHttpParams: { sessionid: 'user.sessionid' },
	retrieveLinks: true,
	contentCached: true,
}

events = {
		onStart: 'login',
		onStop: 'logout'
}

mapping = {
		login: { uri: 'login', method: 'post', params: { username: 'user.name' , password: 'user.password', dummy: 'empty' },
			handlers: { 
				after: function() { 
					user.authenticated = (content == 'Session created'); 
					user.sessionid='1234'; 
					println(user.name + ' user ' + (user.authenticated ? '' : 'not ') + 'authenticated'); 
				} 
			},
			additionalLinks: [ 'a.js', 'b.js', 'c.js', 'd.js', 'e.js' ],
		},
		logout: { uri: 'logout',
			handlers: { 
				after: function() { 
					user.authenticated = false; 
				} 
			} 
		},
		setState: { uri: 'state', params: { state: 'arg.state' }, 
			handlers: { 
				after: function() { 
					println("name('inp1').val()= " + $.name('inp1').val());
					println("id('state').text()= " + $.id('state').text()); 
				} 
			} 
		},
		setState123: { handlers: { before: function() {
			setState({ state: 'state1' });
			setState({ state: 'state2' });
			setState({ state: 'state3' });
		}}},
}

stat = {
	enabled: false,
	local: true,
	remote: { name: 'server', host: 'localhost', port: 9999 },
	http: true,
	actions: true,
	plot: true
}