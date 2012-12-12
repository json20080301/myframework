package org.myframework.sqlconfig;

/**
 * 每个sql的具体信息，是否使用缓存，是否刷新，SQL对应的唯一键
 * @author Administrator
 *
 */
public interface SqlMapper {
	public boolean isUseCache();
	
	public boolean isFlushCache();

	public String getSqlKey();

	public String getSqlCode();
}
