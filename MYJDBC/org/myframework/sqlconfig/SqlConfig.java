package org.myframework.sqlconfig;



/**
 * SQL-Config 
 * SQL配置信息重新加载，获取，添加，
 * @author Administrator
 *
 */
public interface SqlConfig {

	 void reload(String dist) ;
	
	public SqlMapper getSqlMapper(String sqlKey );
	
	public void addSqlMapper(String sqlKey, SqlMapper sqlMapper);
	
}
