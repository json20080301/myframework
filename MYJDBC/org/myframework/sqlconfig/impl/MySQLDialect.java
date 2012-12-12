package org.myframework.sqlconfig.impl;

import java.util.Map;

import org.myframework.sqlconfig.Dialect;

public class MySQLDialect extends Dialect {
	public String getPageSql(String sql, int offset, int limit,
			Map<String, Object> map) {
		map.put("offset", offset);
		map.put("limit", limit);
		return (new StringBuffer(sql.length() + 20)).append(sql)
				.append(" limit  :offset , :limit ").toString();
	}
}
