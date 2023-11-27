package com.myngshj3.testrunner;

public class TestRunnerException extends Exception {
	public TestRunnerException(String msg, Exception cause) {
		super(msg, cause);
	}
	public TestRunnerException(String msg) {
		super(msg);
	}
}