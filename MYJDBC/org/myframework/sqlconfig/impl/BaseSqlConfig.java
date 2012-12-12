package org.myframework.sqlconfig.impl;

import java.util.HashMap;
import java.util.Map;

import org.myframework.sqlconfig.SqlConfig;
import org.myframework.sqlconfig.SqlMapper;

public abstract class BaseSqlConfig implements SqlConfig{

	protected static Map<String, SqlMapper> sqlConfigCache = new HashMap<String, SqlMapper>();

	public BaseSqlConfig() {
		super();
	}

	public SqlMapper getSqlMapper(String sqlKey) {
		return sqlConfigCache.get(sqlKey);
	}

	public void addSqlMapper(String sqlKey, SqlMapper sqlMapper){
		  sqlConfigCache.put(sqlKey, sqlMapper);
	}
	
}