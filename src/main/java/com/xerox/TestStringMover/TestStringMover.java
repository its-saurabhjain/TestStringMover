package com.xerox.TestStringMover;

import java.util.Timer;

public class TestStringMover {

	public static void main(String[] args) {				
		
		TestStringMover testStringMover = new TestStringMover();
		// set up scheduler
		Timer primaryTimer = new Timer();
		primaryTimer.scheduleAtFixedRate(new TestScanForWork(), 100L, 1000L * Configurator.getInstance().getPOLLING_PERIOD());
	}

}
