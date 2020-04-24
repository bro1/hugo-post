package com.bro1.hugopost;

public class Category implements Cloneable{
	String name;
	int count;

	public Category() {
		super();
	}

	
	public Category(String name) {
		super();
		this.name = name; 
	}
	
	public String getName() {
		return name;
	}


	@Override
	public String toString() {
		if (count == 0) return name;
		return name + " (" + count + ")";
	}


	public Category clone()  {
		
		Category a = new Category();
		a.name = name;
		
		return a;
	}	
	
}
