package org.myframework.sqlconfig.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.Node;
import org.myframework.util.XmlUtil;

public class XmlSqlMapper extends PojoSqlMapper {

	public XmlSqlMapper(String namespace, boolean useCache, Node node) {
		this.sqlKey = namespace + "." + node.valueOf("@id");
		this.useCache = useCache;
		this.sqlCode = node.getText();
		this.flushCache = "TRUE".equalsIgnoreCase(node.valueOf("@flushCache"));
	}

	public XmlSqlMapper(Node node) {
		this.sqlKey = node.valueOf("@id");
		this.useCache = "TRUE".equalsIgnoreCase(node.valueOf("@useCache"));
		this.sqlCode = node.getText();
		this.flushCache = "TRUE".equalsIgnoreCase(node.valueOf("@flushCache"));
	}

	/**
	 * 测试SQL配置信息加载	s																			
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		String path = XmlSqlMapper.class.getResource("/sqlvelocity").getPath();
		System.out.println("path :" + path);
		File file = new File(path );
		File files[] = file.listFiles();
		for (int i = 0; files != null && i < files.length; i++) {
			String fileName = files[i].getName();
			System.out.println(fileName);
			System.out.println(files[i].getAbsolutePath());
			InputStream input = new FileInputStream(files[i]);
			Document docServiceCfg = XmlUtil.fromXML(input, null);
			List<Node> nodes = docServiceCfg.selectNodes("//mapper/sql");
			Node root = docServiceCfg.selectSingleNode("//mapper");
			String namespace = root.valueOf("@namespace");
			String useCache = root.valueOf("@useCache");
			for (Node node : nodes) {
				System.out.println(new XmlSqlMapper(namespace, "true"
						.equalsIgnoreCase(useCache), node));
			}
		}
	}

}
