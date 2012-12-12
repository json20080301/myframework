package org.myframework.sqlconfig.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.Node;
import org.myframework.sqlconfig.SqlMapper;
import org.myframework.util.XmlUtil;

public class XmlSqlConfig extends BaseSqlConfig  {
	
	protected final Log log = LogFactory.getLog(getClass());
	
	String mapperDir = "sqlbuilder";

	public XmlSqlConfig(String mapperDir) {
		this.mapperDir = mapperDir;
		loadConfig();
	}

	

	private void loadConfig() {
		File file = new File(getMapperDir());
		File files[] = file.listFiles();
		for (int i = 0; files!=null && i < files.length; i++) {
			reload(files[i]);
		}
	}
	

	private void reload(File dist) {
		log.debug("load sqlconfig :" + dist.getAbsolutePath());
		try {
			InputStream input = new FileInputStream(dist);
			Document docServiceCfg = XmlUtil.fromXML(input, null);
			List<Node> nodes = docServiceCfg.selectNodes("//mapper/sql");
			Node root = docServiceCfg.selectSingleNode("//mapper");
			String namespace = root.valueOf("@namespace");
			String useCache = root.valueOf("@useCache");
			for (Node node : nodes) {
				SqlMapper sqlMapper = new XmlSqlMapper(namespace,
						"true".equalsIgnoreCase(useCache), node);
				sqlConfigCache.put(sqlMapper.getSqlKey(), sqlMapper);
			}
		} catch (Exception e) {
			throw new RuntimeException("load sqlconfig error :" + dist.getAbsolutePath());
		}
	}

	public void reload(String dist) {
		String filePath = getMapperDir() + File.separator + dist;
		log.debug("load sqlconfig :" + filePath);
		try {
			InputStream input = new FileInputStream(filePath);
			Document docServiceCfg = XmlUtil.fromXML(input, null);
			List<Node> nodes = docServiceCfg.selectNodes("//mapper/sql");
			Node root = docServiceCfg.selectSingleNode("//mapper");
			String namespace = root.valueOf("@namespace");
			String useCache = root.valueOf("@useCache");
			System.out.println(namespace + useCache);
			for (Node node : nodes) {
				SqlMapper sqlMapper = new XmlSqlMapper(namespace,
						"true".equalsIgnoreCase(useCache), node);
				sqlConfigCache.put(sqlMapper.getSqlKey(), sqlMapper);
			}
		} catch (Exception e) {
			log.error("load sqlconfig error :" + filePath);
			log.error(e.getMessage());
		}
	}

	public void setMapperDir(String mapperDir) {
		this.mapperDir = mapperDir;
	}
	
	private String getMapperDir() {
		String path = this.getClass().getResource("/").getPath();
		return path +"/"+ mapperDir;
	}


}
