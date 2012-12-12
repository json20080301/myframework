package org.myframework.sqlconfig.impl;

import java.io.StringWriter;
import java.util.Map;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.myframework.sqlconfig.SqlMapper;
import org.myframework.util.StringUtil;

public class VelocitySqlTemplate extends BaseSqlTemplate {

	public String getSql(String sqlKey, Map<String, Object> map) {
		VelocityContext context = new VelocityContext(map);
		context.put("StringUtil", new StringUtil());
		/* 解析后数据的输出目标  */
		StringWriter w = new StringWriter();
		/* 进行解析 */
		SqlMapper sqlMapper = sqlConfig.getSqlMapper(sqlKey);
		if (sqlMapper!=null) {
			try {
				Velocity.evaluate(context, w, this.getClass().getName(), sqlMapper.getSqlCode());
			} catch ( Exception e) {
				throw new IllegalArgumentException("Velocity.evaluate {" + sqlKey + "}   error"+ e.getMessage());
			}
		}else{
			throw new IllegalArgumentException("Velocity  sqlConfig  {" + sqlKey + "} not exist");
		}
		return w.toString();
	}

}
