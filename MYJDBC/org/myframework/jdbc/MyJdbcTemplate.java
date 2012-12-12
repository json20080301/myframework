package org.myframework.jdbc;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.apache.commons.dbcp.DelegatingConnection;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.myframework.cache.Cache;
import org.myframework.cache.impl.OSCache;
import org.myframework.sqlconfig.Dialect;
import org.myframework.sqlconfig.SqlMapper;
import org.myframework.sqlconfig.SqlTemplate;
import org.myframework.sqlconfig.impl.JavaSqlTemplate;
import org.myframework.sqlconfig.impl.OracleDialect;
import org.myframework.util.DateUtil;
import org.myframework.util.ResultMap;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;


/**
 * 
 * @author Administrator
 * 
 */
public class MyJdbcTemplate extends JdbcTemplate {
	private static Log log = LogFactory.getLog(MyJdbcTemplate.class);

	private static int DEFAULT_PAGE_SIZE = 50;

	protected Cache dataCache = new OSCache(getClass().getName()); // EhcacheCache

	private SqlTemplate sqlTemplate = new JavaSqlTemplate(); // VelocitySqlTemplate

	private Dialect dialect = new OracleDialect();// MySQLDialect


	@Resource(name = "dataSource")
	public void setDataSource(DataSource dataSource) {
		super.setDataSource(dataSource);
	}

	public void setDialect(Dialect dialect) {
		this.dialect = dialect;
	}

	/**
	 * 
	 * @param mapperId
	 * @param map
	 * @return
	 * @throws Exception
	 */
	public List<Map<String, Object>> selectAllList(String sqlKey,
			Map<String, Object> map) {
		SqlMapper sqlMapper = sqlTemplate.getSqlMapper(sqlKey);
		String cacheKey = getCacheKey(sqlKey, map);
		if (sqlMapper == null) {
			throw new RuntimeException("load sqlconfig error :" + sqlKey);
		}
		if (sqlMapper.isFlushCache()) {
			dataCache.removeObject(cacheKey);
		}
		if (sqlMapper.isUseCache()) {
			if (dataCache.getObject(cacheKey) != null) {
				log.debug("load from cacheKey : " + cacheKey);
				return (List<Map<String, Object>>) dataCache
						.getObject(cacheKey);
			} else {
				log.debug("reload   cacheKey : " + cacheKey);
				List<Map<String, Object>> rs = this.queryForList(
						sqlTemplate.getSql(sqlKey, map), map);
				dataCache.putObject(cacheKey, rs);
				return rs;
			}
		} else {
			List<Map<String, Object>> rs = this.queryForList(
					sqlTemplate.getSql(sqlKey, map), map);
			return rs;
		}
	}

	/**
	 * @param sqlKey
	 * @param map
	 * @param offset
	 * @param limit
	 * @return  结果集是 offset+1~offset+limit这些记录
	 */
	public List<Map<String, Object>> selectPageList(String sqlKey,
			Map<String, Object> map, int offset, int limit) {
		log.debug("offset " + offset + " limit " + limit);
		long startTime = System.currentTimeMillis();
		String sql = sqlTemplate.getSql(sqlKey, map);
		if (dialect!=null){
			sql = dialect.getPageSql(sql, offset, limit, map);
			long endTime = System.currentTimeMillis();
			log.debug("getSql costTime ms :" + (endTime - startTime));
			log.debug(sql);
			log.debug(map);
			return this.queryForList(sql, map);
		} else {
			return selectLogicPageList(  sqlKey, map,   offset,   limit);
		}
		
	}

	/**
	 * @param sqlKey
	 * @param map
	 * @param offset 
	 * @param limit
	 * @return 结果集是 offset+1~offset+limit这些记录
	 * @throws SQLException
	 */
	public List<Map<String, Object>> selectLogicPageList(String sqlKey,
			Map<String, Object> map, int offset, int limit)   {
		log.debug("offset " + offset + " limit " + limit);
		long startTime = System.currentTimeMillis();
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		List<Map<String, Object>> rsList= new ArrayList<Map<String, Object>>(limit);
		String sql = sqlTemplate.getSql(sqlKey, map);
		long endTime = System.currentTimeMillis();
		log.debug("getSql costTime ms :" + (endTime - startTime));
		List<Object> params = new ArrayList<Object>();
		sql = this.sqlConvert(sql, map, params);
		log.debug(sql);
		log.debug(map);
		DataSource dataSource = this.getDataSource();
		try {
			conn = dataSource.getConnection();
			stmt = conn.prepareStatement(sql);
			stmt.setQueryTimeout(this.getQueryTimeout());
			for (int i = 0; i < params.size(); i++) {
				if (params.get(i) instanceof Integer)
					stmt.setInt(i + 1, ((Integer) params.get(i)).intValue());
				else if (params.get(i) instanceof Long)
					stmt.setLong(i + 1, ((Long) params.get(i)).longValue());
				else if (params.get(i) instanceof String)
					stmt.setString(i + 1, params.get(i).toString());
				else if (params.get(i) instanceof java.sql.Date)
					stmt.setDate(i + 1, (java.sql.Date) params.get(i));
				else if (params.get(i) instanceof java.sql.Timestamp)
					stmt.setTimestamp(i + 1, (java.sql.Timestamp) params.get(i));
				else if (params.get(i) instanceof java.util.Date)
					stmt.setDate(
							i + 1,
							new java.sql.Date(((java.util.Date) params.get(i))
									.getTime()));
				else if (params.get(i) instanceof Double)
					stmt.setDouble(i + 1, ((Double) params.get(i)).doubleValue());
				else if (params.get(i) instanceof Float)
					stmt.setFloat(i + 1, ((Float) params.get(i)).floatValue());
				else
					stmt.setObject(i + 1, params.get(i));
			}
			rs = stmt.executeQuery();
			// 如果支持scrollable result，使用ResultSet的absolute方法直接移到查询起点
			List<String> keysList = getMetaData(rs);
			if (isScrollable(rs)) {
				// we can go straight to the first required row
				rs.absolute(offset);
				int rows =0 ;
				while(rs.next()){
					rows++;
					if( rows >  limit ){
						break;
					}else{
						 Map<String, Object> rsMap = new HashMap<String, Object>();
						 for(String key :keysList)
							 rsMap.put(key, rs.getObject(key));
						 rsList.add(rsMap);	 
					}
				}
			} else {
				int i =0;
				while(rs.next()){
					i++ ;
					log.debug(i);
					if(i> offset&&  i <= offset+limit ){
						//插入结果集
						 Map<String, Object> rsMap = new HashMap<String, Object>();
						 for(String key :keysList)
							 rsMap.put(key, rs.getObject(key));
						 rsList.add(rsMap);	
					}
					if(i>offset+limit) break ;
				}
			}
		} catch (SQLException ex) {
			throw new RuntimeException(ex.getMessage());
		} finally {
			DataSourceUtils.releaseConnection(conn, dataSource);
		}
		return rsList;
	}

	private List<String> getMetaData (ResultSet resultSet) throws SQLException  {
		List<String> list = new ArrayList<String>();
		ResultSetMetaData metadata = resultSet.getMetaData();
		int n = metadata.getColumnCount();
		for (int i = 1; i <= n; i++) {
			String colName = metadata.getColumnName(i);
			list.add(colName);
		}
		return list;
	}
	
	/**
	 * 是否支持 scrollable result
	 * @param resultSet
	 * @return
	 * @throws SQLException
	 */
	private boolean isScrollable(ResultSet resultSet) throws SQLException{
		int type = resultSet.getType();
		if(type==ResultSet.TYPE_SCROLL_INSENSITIVE||type==ResultSet.TYPE_SCROLL_SENSITIVE)
			return true;
		else 
			return false;
	}

	/**
	 * 
	 * @param mapperId
	 * @param map
	 * @return
	 * @throws Exception
	 */
	public List<Map<String, Object>> selectPageList(String mapperId,
			Map<String, Object> map) {
		ResultMap<String, Object> rm = new ResultMap<String, Object>(map);
		int offset = rm.getInt(Dialect.OFFSET);
		int limit = rm.hasKey(Dialect.LIMIT) ? rm.getInt(Dialect.LIMIT)
				: DEFAULT_PAGE_SIZE;
		return this.selectPageList(mapperId, map, offset, limit);
	}

	/**
	 * 
	 * @param mapperId
	 * @param map
	 * @return
	 * @throws Exception
	 */
	public Long selectCount(String sqlKey, Map<String, Object> map) {
		String sql = dialect.getCountString(sqlTemplate.getSql(sqlKey, map));
		List<Object> lstParam = new ArrayList<Object>();
		String querySql = this.sqlConvert(sql, map, lstParam);
		return this.queryForLong(querySql, lstParam.toArray());
	}

	/**
	 * sql example: update table set colname = '' where user_id = :userId
	 * 
	 * @param sqlKey
	 * @param map
	 */
	public int updateBySqlKey(String sqlKey, Map<String, Object> map) {
		String sql = sqlTemplate.getSql(sqlKey, map);
		List<Object> paramList = new ArrayList<Object>();
		String updateSql = this.sqlConvert(sql, map, paramList);
		return this.update(updateSql, paramList.toArray());
	}

	/**
	 * @param sql
	 *            example:select * from table where user_id = :userId
	 * @param map
	 */
	public List<Map<String, Object>> queryForList(String sql,
			Map<String, Object> map) {
		List<Object> lstParam = new ArrayList<Object>();
		log.debug("-------------sql before convert--------------------");
		log.debug("\n\n" + sql + "\n");
		log.debug(map);
		log.debug("-------------sql before convert--------------------");
		String querySql = this.sqlConvert(sql, map, lstParam);
		log.debug("-------------sql after convert--------------------");
		log.debug("\n\n" + this.getSql(querySql, lstParam) + "\n");
		log.debug("-------------sql after convert--------------------");
		return this.queryForList(querySql, lstParam.toArray());
	}

	/**
	 * select * from table where user_id = :userId
	 * 
	 * @param sql
	 * @param map
	 */
	public Map<String, Object> queryForMap(String sql, Map<String, Object> map) {
		List<Object> paramList = new ArrayList<Object>();
		String querySql = this.sqlConvert(sql, map, paramList);
		log.debug(this.getSql(querySql, paramList));
		return this.queryForMap(querySql, paramList.toArray());
	}

	/**
	 * @param sql
	 *            example: update table set colname = '' where user_id = :userId
	 * @param map
	 */
	public int update(String sql, Map<String, Object> map) {
		List<Object> paramList = new ArrayList<Object>();
		String updateSql = this.sqlConvert(sql, map, paramList);
		return this.update(updateSql, paramList.toArray());
	}

	/**
	 * sql ( select * from table where user_id = :userId ) Convert to standard
	 * jdbc sql ( select * from table where user_id = ?) ;
	 * 
	 * @param sql
	 * @param map
	 * @param list
	 * @return
	 */
	@SuppressWarnings("all")
	private String sqlConvert(String sql, Map qryMap, List<Object> list) {
		if (qryMap != null && !qryMap.isEmpty()) {
			List<String> params = getParameterList(sql);
			for (String param : params) {
				list.add(qryMap.get(param));
				sql = StringUtils.replaceOnce(sql, ":" + param, "?");
			}
		}
		return sql;
	}

	/**
	 * sql ( select * from table where user_id = :userId ) :userId will be added
	 * into list as param
	 * 
	 * @param sql
	 * @return
	 */
	private List<String> getParameterList(String sql) {
		String regex = "\\ :(\\w+)";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(sql);
		List<String> params = new ArrayList<String>();
		while (matcher.find())
			params.add(matcher.group(1));
		return params;
	}

	private String getSql(String sql, List<Object> lstParam) {
		for (int i = 0; i < lstParam.size(); i++) {
			Object value = lstParam.get(i);
			if (value instanceof Integer || value instanceof Double
					|| value instanceof Float || value instanceof Long) {
				sql = StringUtils.replaceOnce(sql, "?", value.toString());
			} else if (value instanceof String) {
				sql = StringUtils.replaceOnce(sql, "?", "'" + value.toString()
						+ "'");
			} else if (value instanceof java.util.Date) {
				String dateString = DateUtil.format((java.util.Date) value,
						"yyyy-MM-dd HH:mm:ss");
				sql = StringUtils.replaceOnce(sql, "?", "to_date('"
						+ dateString + "','YYYY-MM-DD HH24:MI:SS'");
			} else {
				sql = StringUtils.replaceOnce(sql, "?",
						"'" + String.valueOf(value) + "'");
			}
		}
		return sql;
	}

	public String getSql(String sqlKey, Map<String, Object> map) {
			String sql = sqlTemplate.getSql(sqlKey, map);
			return "[" + sqlKey + "]" + getQrySql(sql,map) ;
	}
	
	public String getQrySql(String sql, Map<String, Object> map) {
			List<Object> lstParam = new ArrayList<Object>();
			String querySql = this.sqlConvert(sql, map, lstParam);
			return   this.getSql(querySql, lstParam);
	}

	/**
	 * just for Tomcat ORACLE native NativeConnection
	 * 
	 * @param con
	 * @return
	 * @throws SQLException
	 */
	private Connection getNativeConnection(Connection con)  {
		if (con instanceof DelegatingConnection) {
			Connection nativeCon = ((DelegatingConnection) con)
					.getInnermostDelegate();
			log.info("just for Tomcat ORACLE datasource : getNativeConnection from tomcat pool !! ");
			try {
				return (nativeCon != null ? nativeCon : con.getMetaData()
						.getConnection());
			} catch (SQLException e) {
				throw new RuntimeException(e.getMessage());
			}
		}
		return con;
	}

	/**
	 * executeProcedure example: Object[] inValues = new Object[] {
	 * prodIds.split(","), newGridId , managerInfo.getManagerID()}; Object[]
	 * outTypes = new Integer[] {};
	 * qryCenter.executeProcedure("call Grid_Opt.p_Serv_To_Grid(?,?,?)",
	 * Arrays.asList(inValues), Arrays.asList(outTypes));
	 * 
	 * @param procedureName
	 * @param params
	 * @param outLstType
	 * @return
	 * @throws SQLException
	 */
	public List<Object> executeProcedure(String procedureName,
			List<Object> params, List<Object> outLstType)  {
		DataSource dataSource = this.getDataSource();
		Connection conn = getNativeConnection(DataSourceUtils
				.getConnection(dataSource));
		CallableStatement stmt = null;
		int count = params.size();
		try {
			stmt = conn.prepareCall("{" + procedureName + "}");
			for (int i = 0; i < count; i++) {
				if (params.get(i) instanceof Integer)
					stmt.setInt(i + 1, ((Integer) params.get(i)).intValue());
				else if (params.get(i) instanceof Long)
					stmt.setLong(i + 1, ((Long) params.get(i)).longValue());
				else if (params.get(i) instanceof String)
					stmt.setString(i + 1, params.get(i).toString());
				else if (params.get(i) instanceof java.sql.Date)
					stmt.setDate(i + 1, (java.sql.Date) params.get(i));
				else if (params.get(i) instanceof java.sql.Timestamp)
					stmt.setTimestamp(i + 1, (java.sql.Timestamp) params.get(i));
				else if (params.get(i) instanceof java.util.Date)
					stmt.setDate(i + 1, new java.sql.Date(
							((java.util.Date) params.get(i)).getTime()));
				else if (params.get(i) instanceof Double)
					stmt.setDouble(i + 1,
							((Double) params.get(i)).doubleValue());
				else if (params.get(i) instanceof Float)
					stmt.setFloat(i + 1, ((Float) params.get(i)).floatValue());
				else if (params.get(i) instanceof Integer[]) {
					oracle.sql.ArrayDescriptor desc = oracle.sql.ArrayDescriptor
							.createDescriptor("NUM_ARRAY", conn);
					oracle.sql.ARRAY array = new oracle.sql.ARRAY(desc, conn,
							params.get(i));
					stmt.setArray(i + 1, array);
				} else if (params.get(i) instanceof Long[]) {
					oracle.sql.ArrayDescriptor desc = oracle.sql.ArrayDescriptor
							.createDescriptor("NUM_ARRAY", conn);
					oracle.sql.ARRAY array = new oracle.sql.ARRAY(desc, conn,
							params.get(i));
					stmt.setArray(i + 1, array);
				} else if (params.get(i) instanceof String[]) {
					oracle.sql.ArrayDescriptor desc = oracle.sql.ArrayDescriptor
							.createDescriptor("STR_ARRAY", conn);
					oracle.sql.ARRAY array = new oracle.sql.ARRAY(desc, conn,
							params.get(i));
					stmt.setArray(i + 1, array);
				} else
					stmt.setObject(i + 1, params.get(i));
			}
			for (int j = 0; j < outLstType.size(); j++) {
				stmt.registerOutParameter(count + j + 1,
						((Integer) outLstType.get(j)).intValue());
			}
			stmt.execute();
			List<Object> resultList = new ArrayList<Object>();
			for (int k = 0; k < outLstType.size(); k++) {
				resultList.add(stmt.getObject(count + k + 1));
			}
			return resultList;
		} catch (SQLException ex) {
			throw new RuntimeException(ex.getMessage());
		} finally {
			DataSourceUtils.releaseConnection(conn, dataSource);
		}
	}

	private String getCacheKey(String sqlKey, Map<String, Object> map) {
		List<Object> paramList = new ArrayList<Object>();
		StringBuffer cacheKey = new StringBuffer(sqlKey);
		cacheKey.append("_");
		for (Object value : paramList) {
			cacheKey.append(value.toString()).append("_");
		}
		return cacheKey.toString();
	}

	public SqlTemplate getSqlTemplate() {
		return sqlTemplate;
	}

	public void setSqlTemplate(SqlTemplate sqlTemplate) {
		this.sqlTemplate = sqlTemplate;
	}

}
