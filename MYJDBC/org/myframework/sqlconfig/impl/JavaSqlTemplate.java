package org.myframework.sqlconfig.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.myframework.sqlconfig.SqlBuilder;
import org.myframework.sqlconfig.SqlMapper;
import org.myframework.util.Debug;

public class JavaSqlTemplate extends BaseSqlTemplate {
	private static final Log log = LogFactory.getLog(JavaSqlTemplate.class);

	private Map<String, SqlBuilder> cache = new HashMap<String, SqlBuilder>();

	private String packageName = "temp";

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	public   String getSql(String sqlKey, Map<String, Object> context)  {
		SqlBuilder sqlBuilder  = getSqlBuilder(sqlKey);
		return sqlBuilder.getSql(context);
	}

	private  SqlBuilder getSqlBuilder(String sqlKey)  {
		SqlBuilder sqlBuilder = cache.get(sqlKey);
		SqlMapper sqlMapper = sqlConfig.getSqlMapper(sqlKey);
		//SQL配置缓存中存在数据
		if (sqlMapper!=null&&sqlBuilder != null) {
			log.debug("load {" + sqlKey + "} sqlBuilder from cache !");
			return sqlBuilder;
		} else  {
			if(sqlMapper==null||sqlMapper.getSqlCode()==null){
				throw new IllegalArgumentException("{" + sqlKey + "} sqlConfig not exist");
			}
			try {
				sqlBuilder = compile(sqlMapper.getSqlCode());
			} catch (Exception e) {
				throw new IllegalArgumentException("compile {" + sqlKey + "} to sqlBuilder error");
			} finally {
				log.debug("add {" + sqlKey + "} sqlBuilder to cache !");
				cache.put(sqlKey, sqlBuilder);
			}
			return sqlBuilder;
		}  
	}

	/**
	 * 编译返回SqlBuilder
	 * 
	 * @param sqlCode
	 * @return
	 * @throws Exception
	 */
	private SqlBuilder compile(String sqlCode) throws Exception {
		log.info("################ Class Dir[" + getClassDir()
				+ "] #################");

		File file = null;
		try {
			Debug debug = new Debug("Dyna Complie");
			debug.start("ComplieFile");
			file = createFile(sqlCode);
			String result = compileFile(file);
			String classname = file.getName().substring(0,
					file.getName().lastIndexOf('.'));
			// 编译结果0标识成功,编译错误返回NULL
			if (!"0".equals(result)) {
				// 保存错误信息
				throw new Exception("【" + getJavaCode(classname, sqlCode) + "】"
						+ result);
			}
			debug.end("ComplieFile");
			log.info(debug);
			return createInstance(file);
		} catch (Exception e) {
			log.error(e);
			throw new Exception(e);
		} finally {
			removeTempFile(file);
		}
	}

	/**
	 * 创建Java文件
	 * 
	 * @param sqlCode
	 * @return
	 * @throws Exception
	 */
	private File createFile(String sqlCode) throws Exception {
		File dir = new File(getClassDir());
		if (!dir.exists())
			dir.mkdir(); // 如果目录不存在就创建目录
		File file = File.createTempFile("RunTime", ".java", dir);
		String name = file.getName();
		String classname = name.substring(0, name.lastIndexOf('.'));
		PrintWriter out = null;
		try {
			out = new PrintWriter(new FileOutputStream(file));
			String javaCode = getJavaCode(classname, sqlCode);
			out.println(javaCode);
			log.info(javaCode);
			out.flush();
		} finally {
			out.close();
		}
		return file;
	}

	/**
	 * 编译文件
	 * 
	 * @param file
	 * @throws Exception
	 */
	private String compileFile(File file) throws Exception {
		String classPath = getClassPath();
		String[] arg = { "-classpath", classPath, "-d", classPath,
				file.getAbsolutePath() };
		StringWriter writer = new StringWriter(1024);
		int code = com.sun.tools.javac.Main.compile(arg,
				new PrintWriter(writer));
		return code == 0 ? "0" : writer.toString();
	}

	/**
	 * 创建实例
	 * 
	 * @param file
	 * @return
	 * @throws Exception
	 */
	private SqlBuilder createInstance(File file) throws Exception {
		String filename = file.getName();
		String classname = filename.substring(0, filename.lastIndexOf('.'));
		return (SqlBuilder) Class.forName(packageName + "." + classname)
				.newInstance();
	}

	/**
	 * 删除临时文件
	 * 
	 * @param file
	 * @throws Exception
	 */
	private void removeTempFile(File file) throws Exception {
		String classDir = getClassDir();
		int last = file.getName().lastIndexOf('.');
		String name = file.getName().substring(0, last);
		String classFile = classDir + File.pathSeparator + name + ".java";
		String javaFile = classDir + File.pathSeparator + name + ".class";
		new File(javaFile).delete();
		new File(classFile).delete();
	}

	/**
	 * 取得Class路径
	 * 
	 * @return
	 */
	private String getClassPath() {
		URL url = this.getClass().getResource("/");
		// URL url = Thread.currentThread().getContextClassLoader()
		// .getResource("/");
		File file = new File(url.getPath());
		return file.getAbsolutePath();
	}

	/**
	 * 生成 JAVA代码片段
	 * 
	 * @param classname
	 * @param sqlCode
	 * @return
	 */
	private String getJavaCode(String classname, String sqlCode) {
		StringBuffer code = new StringBuffer(1024);
		code.append("\npackage " + packageName + "; \n");
		code.append("import java.util.Map;\n");
		code.append("import org.myframework.sqlconfig.SqlBuilder;\n");
		code.append("import org.myframework.util.ResultMap;\n");
		code.append("public class " + classname + " implements SqlBuilder {\n");
		code.append("   public String getSql(Map<String,Object> map) { \n");
		code.append(" 		ResultMap rm = new ResultMap(map) ;\n");
		code.append(" 		StringBuffer sql = new StringBuffer(512);\n");
		code.append("       " + sqlCode + " \n");
		code.append("       return sql.toString();\n");
		code.append("   }\n");
		code.append("}\n");
		return code.toString();
	}

	/**
	 * 取得Java路径
	 * 
	 * @return
	 */
	private String getClassDir() {
		return getClassPath() + "/" + packageName;
	}

	/**
	 * 验证 JAVA语法
	 * 
	 * @param sqlCode
	 * @return
	 * @throws Exception
	 */
	public String validateJavaCode(String sqlCode) throws Exception {
		log.info("################ Class Dir[" + getClassDir()
				+ "] #################");
		File file = createFile(sqlCode);
		String classname = file.getName().substring(0,
				file.getName().lastIndexOf('.'));
		try {
			Debug debug = new Debug("Dyna Complie");
			debug.start("ComplieFile");
			String result = compileFile(file);
			if (!"0".equals(result)) {
				// 保存错误信息
				return "语法验证不通过：\n【\n" + getJavaCode(classname, sqlCode)
						+ "\n】\n" + "验证信息如下：\n【\n" + result + "\n】\n";
			}
			debug.end("ComplieFile");
			log.info(debug);
			return "语法验证通过：\n【\n" + getJavaCode(classname, sqlCode) + "\n】\n";
		} catch (Exception e) {
			log.error(e);
			return ("\n【\n" + getJavaCode(classname, sqlCode) + "\n】\n");
		} finally {
			removeTempFile(file);
		}
	}

}
