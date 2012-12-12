package org.myframework.sqlconfig;

import java.util.Map;

/**
 * sql生成模板 ：生成SQL ，获取SQL其他关联信息
 * @author Administrator
 *
 */
public interface SqlTemplate {

	public String getSql(String sqlKey, Map<String, Object> context);

	public SqlMapper getSqlMapper(String sqlKey);

}
