package org.myframework.sqlconfig.impl;

import java.util.Map;

import org.myframework.sqlconfig.Dialect;

public class OracleDialect extends Dialect {
	public String getPageSql(String sql, int offset, int limit ,Map<String, Object> map) {
		StringBuffer pagingSelect = new StringBuffer(sql.length() + 100);
		pagingSelect
				.append("select * from ( select row_.*, rownum rownum_ from ( \n");

		pagingSelect.append(sql);

		pagingSelect
				.append("\n ) row_ where rownum <=  :rownumEnd  ) where rownum_ > :rownumStart  ");
		
		map.put("rownumStart", offset);
		map.put("rownumEnd", offset+limit);
		
		return pagingSelect.toString();

	}
}
