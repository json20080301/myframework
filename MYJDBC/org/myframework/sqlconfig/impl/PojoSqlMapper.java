package org.myframework.sqlconfig.impl;

import org.myframework.sqlconfig.SqlMapper;

public class PojoSqlMapper implements SqlMapper {
	
	boolean useCache ;
	
	String sqlKey ;
	
	String sqlCode ;
	
	boolean flushCache;


	public boolean isUseCache() {
		return useCache;
	}

	public void setUseCache(boolean useCache) {
		this.useCache = useCache;
	}

	public String getSqlKey() {
		return sqlKey;
	}

	public void setSqlKey(String sqlKey) {
		this.sqlKey = sqlKey;
	}

	public String getSqlCode() {
		return sqlCode;
	}

	public void setSqlCode(String sqlCode) {
		this.sqlCode = sqlCode;
	}
	
	public void setFlushCache(boolean flushCache) {
		this.flushCache = flushCache;
	}

	public boolean isFlushCache() {
		return flushCache;
	}

	@Override
	public String toString() {
		return "SqlMapperConfig [useCache=" + useCache + ", sqlKey=" + sqlKey
				+ ", sqlCode=" + sqlCode + ", flushCache=" + flushCache + "]";
	}

}
