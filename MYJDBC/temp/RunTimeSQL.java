package temp;

import java.util.Map;

import org.myframework.sqlconfig.SqlBuilder;
import org.myframework.util.ResultMap;

public class RunTimeSQL implements SqlBuilder {

	public String getSql(Map<String, Object> map)   {
		ResultMap rm = new ResultMap(map);
		StringBuffer sql = new StringBuffer(512);
		
		//---------------------------------以下内容写入sqlbuilder目录中的XML文件-------------
		sql.append(" select owner , tablespace_name from all_all_tables where 1=1 \n");
		if (rm.isValid("owner"))
			sql.append(" and owner = :owner  \n");
		//---------------------------------
		  
		
		
		return sql.toString();
	}
}