groups = []

groups[0] = { 
	users: [
		{ name: 'tes', password: 'fail', language: 'ru' },
		{ name: 'test', password: 'password', language: 'en' },
		{ pattern: true, name: 'test_00', password: 'pass_00', language: 'en', first: 1, last: 3 }
	],
	run: function() { 
		//setState({ state: 'state1' });
		setState123();
	},
}