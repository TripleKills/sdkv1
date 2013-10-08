package com.sar.gp;

import java.util.Map;

public class Dlg {
	
	private static Dlg _inst;
	
	private Dlg(){}
	
	public static Dlg getIns() {
		if (null == _inst) _inst = new Dlg();
		return _inst;
	}
	
	public void dlg(Map<String, String> data) {}
	
}
