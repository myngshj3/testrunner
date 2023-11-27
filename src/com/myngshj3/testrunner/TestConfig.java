package com.myngshj3.testrunner;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class TestConfig {
	public static final String TAG_TESTCONFIG = "testconfig";
	public static final String TAG_CLASSPATH = "classpath";
	public static final String TAG_TESTS = "tests";
	public static final String TAG_TEST = "test";
	public static final String TAG_DATABASE = "database";
	
	public static class Database {
		private String directory;
		private String[] tables;
		public Database(Element databaseElement) throws TestRunnerException {
			directory = databaseElement.getAttribute("directory");
			tables = databaseElement.getAttribute("tables").split(";");
		}
		public void setupDatabase() throws TestRunnerException {
			for (String table: tables) {
				String filePath = directory + File.separator + table + ".csv";
			}
		}
		public void backupDatabase(String directory) throws TestRunnerException {
			
		}
		public void restoreDatabase(String directory) throws TestRunnerException {
			
		}
	}
	
	public static class Classpath {
		public static final String TAG_URL = "url";
		public static final String ATTR_VALUE = "value";
		private URLClassLoader classLoader;
		public Classpath(Element classpathElement) throws TestRunnerException {
			loadClasspath(classpathElement);
		}
		public URLClassLoader getClassLoader() { return classLoader; }
		protected void loadClasspath(Element classpathElement) throws TestRunnerException {
			NodeList nodeList = classpathElement.getChildNodes();
			List<URL> urls = new ArrayList<>();
			for (int i = 0; i < nodeList.getLength(); i++) {
				if (nodeList.item(i) instanceof Element) {
					Element element = (Element)nodeList.item(i);
					if (!element.getTagName().equals(TAG_URL)) {
						throw new TestRunnerException("Only tag url can be contained as child element in tests");
					}
					try {
						URL url = new URL(element.getAttribute(ATTR_VALUE));
						urls.add(url);
					} catch (Exception e) {
						throw new TestRunnerException("Something happened during creating url.", e);
					}
				}
			}
			classLoader = URLClassLoader.newInstance((URL[])urls.toArray());
		}
	}
	
	public static class Tests {
		protected List<Test> tests;
		public List<Test> getTests() {
			return tests;
		}
		public Tests(Element testsElement) throws TestRunnerException {
			loadTests(testsElement);
		}
		protected void loadTests(Element testsNode) throws TestRunnerException {
			NodeList nodeList = testsNode.getChildNodes();
			for (int i = 0; i < nodeList.getLength(); i++) {
				if (nodeList.item(i) instanceof Element) {
					Element element = (Element)nodeList.item(i);
					if (!element.getTagName().equals(TAG_TEST)) {
						throw new TestRunnerException("Only tag test can be contained as child element in tests");
					}
					Test testConfig = new Test(element);
					tests.add(testConfig);
				}
			}
		}
	}
	
	public static class Test {
		public static final String ATTR_CLASSNAME = "classname";
		public static final String ATTR_METHODNAME = "methodname";
		public static final String TAG_PRECHECK = "precheck";
		public static final String TAG_POSTCHECK = "postcheck";
		private String methodName;
		private String className;
		private CheckRule precheck;
		private CheckRule postcheck;
		
		public String getMethodName() {
			return methodName;
		}
		public String getClassName() {
			return className;
		}
		public Test(Element testElement) throws TestRunnerException {
			className = testElement.getAttribute(ATTR_CLASSNAME);
			if (className == null) {
				throw new TestRunnerException("Classname not specified.");
			}
			methodName = testElement.getAttribute(ATTR_METHODNAME);
			if (methodName == null) {
				throw new TestRunnerException("Methodname not specified.");
			}
			NodeList nodeList = testElement.getChildNodes();
			for (int i = 0; i < nodeList.getLength(); i++) {
				if (nodeList.item(i) instanceof Element) {
					Element element = (Element)nodeList.item(i);
					if (element.getTagName().equals(TAG_PRECHECK)) {
						if (precheck != null) {
							throw new TestRunnerException("Only one tag precheck can be contained.");
						}
						precheck = new CheckRule(element);
					} else if (element.getTagName().equals(TAG_POSTCHECK)) {
						if (postcheck != null) {
							throw new TestRunnerException("Only one tag postcheck can be contained.");
						}
						postcheck = new CheckRule(element);
					}
				}
			}
		}
		public CheckRule getPrecheck() {
			return precheck;
		}
		public CheckRule getPostcheck() {
			return postcheck;
		}
	}
	
	public static class CheckRule {
		public static final String TAG_QUERY = "query";
		public static final String TAG_SCRIPT = "script";
		public static final String ATTR_NAME = "name";
		public static final String ATTR_ARGS = "args";
		
		public static class Query {
			private String query;
			private String name;
			private String[] args;
			
			public String getQuery() { return query; }
			public String getName() { return name; }
			public String[] getArgs() { return args; }
			
			public Query(Element queryElement) {
				name = queryElement.getAttribute(ATTR_NAME);
				args = queryElement.getAttribute(ATTR_ARGS).split(";");
				query = queryElement.getTextContent();
			}
		}
		
		private List<Query> queries = new ArrayList<>();
		private String script;
		
		public CheckRule(Element checkRuleElement) throws TestRunnerException {
			NodeList nodeList = checkRuleElement.getChildNodes();
			for (int i = 0; i < nodeList.getLength(); i++) {
				if (nodeList.item(i) instanceof Element) {
					Element element = (Element)nodeList.item(i);
					if (element.getTagName().equals(TAG_QUERY)) {
						Query query = new Query(element);
						queries.add(query);
					} else if (element.getTagName().equals(TAG_SCRIPT)) {
						if (script != null) {
							throw new TestRunnerException("Tag script can be contained only onece.");
						}
					}
				}
			}
			if (script == null) {
				throw new TestRunnerException("No script tag found.");
			}
		}
		
		public void test(Connection connection) throws TestRunnerException {
			ScriptEngineManager manager = new ScriptEngineManager();
			ScriptEngine engine = manager.getEngineByExtension("js");
			ScriptContext context = engine.getContext();
			PreparedStatement stmt = null;
			ResultSet rs = null;
			try {
				for (Query query: queries) {
					stmt = connection.prepareStatement(query.getQuery());
					rs = stmt.executeQuery();
					List<Map<String, Object>> records = new ArrayList<>();
					while (rs.next()) {
						Map<String, Object> record = new HashMap<>();
						for (int i = 0; i < rs.getMetaData().getColumnCount(); i++) {
							record.put(rs.getMetaData().getColumnName(i), rs.getObject(i));
						}
						records.add(record);
						context.setAttribute(query.getName(), records, ScriptContext.ENGINE_SCOPE);
					}
				}
			} catch (Exception ex) {
				throw new TestRunnerException("Exception occured during querying.", ex);
			} finally {
				if (rs != null) {
					try {
						rs.close();
					} catch (SQLException e) {
						throw new TestRunnerException("Something happned during closing ResultSet.", e);
					}
				}
				if (stmt != null) {
					try {
						stmt.close();
					} catch (SQLException e) {
						throw new TestRunnerException("Something happned during closing PreparedStatement.", e);
					}
				}
			}
			try {
				engine.eval(script);
			} catch (ScriptException e) {
				throw new TestRunnerException("Something happned during evaluating script.", e);
			}
		}
	}

	protected Classpath classpathConfig;
	protected Database databaseConfig;
	protected Tests testsConfig;
	
	public TestConfig(String configPath) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document document = builder.parse(Paths.get(configPath).toFile());
		Element topElement = document.getDocumentElement();
		if (!topElement.getTagName().equals(TAG_TESTCONFIG)) {
			throw new TestRunnerException("Invalid top element:" + topElement.getTagName());
		}
		loadTopElement(topElement);
		
	}
	
	protected void loadTopElement(Element topElement) throws TestRunnerException {
		NodeList nodeList = topElement.getElementsByTagName(TAG_CLASSPATH);
		if (nodeList.getLength() != 1) {
			throw new TestRunnerException("Tag classpath should be contained only once");
		}
		classpathConfig = new Classpath((Element)nodeList.item(0));
		nodeList = topElement.getElementsByTagName(TAG_TESTS);
		if (nodeList.getLength() != 1) {
			throw new TestRunnerException("Tag tests should be contained only once");
		}
		testsConfig = new Tests((Element)nodeList.item(0));
		nodeList = topElement.getElementsByTagName(TAG_DATABASE);
		if (nodeList.getLength() > 1) {
			throw new TestRunnerException("Tag database should be contained only once");
		}
		if (nodeList.getLength() == 1) {
			databaseConfig = new Database((Element)nodeList.item(0));
		}
	}
	
	public Classpath getClasspath() { return classpathConfig; }
	public Database getDatabase() { return databaseConfig; }
	public Test getTest(String className, String methodName) {
		for (Test test: testsConfig.getTests()) {
			if (test.getClassName().equals(className) && test.getMethodName().equals(methodName)) {
				return test;
			}
		}
		return null;
	}
}