/*
 * 创建日期 2010-5-12
 *
 * 联创科技(南京)股份有限公司
 * 电话：025-52209888-8139
 * 传真：025-52349113
 * 邮编：210006 
 * 版权所有
 */
package org.myframework.sqlconfig;

import java.util.Map;

/**
 * @rem: Sql构造器
 * @since 2010-5-12
 */
public interface SqlBuilder {
    
    /**
     * Sql构造器
     * @param map
     * @return
     */
    String getSql(Map<String,Object> map)  ;
}
