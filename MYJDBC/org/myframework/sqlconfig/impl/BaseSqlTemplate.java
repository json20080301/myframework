package org.myframework.sqlconfig.impl;

import org.myframework.sqlconfig.SqlConfig;
import org.myframework.sqlconfig.SqlMapper;
import org.myframework.sqlconfig.SqlTemplate;

public abstract class BaseSqlTemplate implements SqlTemplate{
	
	protected SqlConfig sqlConfig   ;
	
	public SqlConfig getSqlConfig() {
		return sqlConfig;
	}

	public void setSqlConfig(SqlConfig sqlConfig) {
		this.sqlConfig = sqlConfig;
	}

	public SqlMapper getSqlMapper(String sqlKey ){
		return sqlConfig.getSqlMapper(sqlKey) ;
	}
	
}
