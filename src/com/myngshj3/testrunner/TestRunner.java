package com.myngshj3.testrunner;

import java.util.ArrayList;
import java.util.List;

import com.myngshj3.testrunner.annotation.DataModelCheck;

@DataModelCheck
public class TestRunner {
	
	protected TestConfig testConfig;
	
	@AroundInvoke
	public Object intercept(InvocationContext ic) throws Exception {
		String className = ic.getMethod().getDeclaringClass().getName();
		String methodName = ic.getMethod().getName();
		
		TestConfig.Test test = testConfig.getTest(className, methodName);
		TestConfig.CheckRule precheck = test.getPrecheck();
		if (precheck != null) {
			precheck.test(null);
		}
		Object result = ic.proceed();
		
		TestConfig.CheckRule postcheck = test.getPrecheck();
		if (postcheck != null) {
			postcheck.test(null);
		}
		return result;
		
	}
	public TestConfig getTestConfig() { return testConfig; }
	
	protected String[] parseArgs(String[] args) throws Exception {
		List<String> argList = new ArrayList<>();
		for (String arg: args) {
			if (arg.startsWith("--testconfig=")) {
				String configPath = arg.substring("--testconfig=".length());
				testConfig = new TestConfig(configPath);
			} else {
				argList.add(arg);
			}
		}
		return (String[])argList.toArray();
	}
	
	public void run(String[] args) {
		
	}
	
	public static void main(String[] args) {
		try {
			TestRunner testRunner = new TestRunner();
			args = testRunner.parseArgs(args);
			TestConfig testConfig = testRunner.getTestConfig();
			testConfig.getDatabase().backupDatabase(null);
			testConfig.getDatabase().setupDatabase();
			
			testRunner.run(args);
			
			testConfig.getDatabase().restoreDatabase(null);
			
		} catch (Exception e) {
			
		}
	}
}