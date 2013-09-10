groups = []

groups[0] = { 
	users: [
		{ pattern: true, name: 'test_00', password: 'pass_00', language: 'en', first: 1, last: 500 }
	],
	run: function() { 
		for (var i = 0; i < 100; i++) {
			java.lang.Thread.sleep(100);
			setState({ state: 'state' + i });
			java.lang.Thread.sleep(100);
			logout();
			java.lang.Thread.sleep(100); 
			login(); 
		}
	},
}