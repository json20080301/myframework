package org.myframework.sqlconfig;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public abstract class Dialect {
	
	public static String ROWS_COUNT = "ROWS_COUNT" ;
	
	public static String OFFSET = "OFFSET" ;
	
	public static String LIMIT = "LIMIT" ;
	
	
	protected Log log = LogFactory.getLog(getClass());

	public abstract String getPageSql(String sql, int offset, int limit ,Map<String, Object> map);
    
    public String getCountString(String sql )
    {
    	StringBuffer countSelect = new StringBuffer(sql.length() + 100);
		countSelect.append("select count(1) "+ROWS_COUNT+" from ( ");
		countSelect.append(sql);
		countSelect.append(" ) ");
		return countSelect.toString();
    }

}