package com.github.perf;

import java.util.List;
import java.util.Map;

public class UserGroup {

	private List<Map<String, Object>> users;
	private String func;
	
	public List<Map<String, Object>> getUsers() {
		return users;
	}
	public void setUsers(List<Map<String, Object>> users) {
		this.users = users;
	}
	public String getFunc() {
		return func;
	}
	public void setFunc(String func) {
		this.func = func;
	}
	
}
