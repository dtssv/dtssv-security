package com.dtssv.security.mybatis.config;

import lombok.Data;

/**
 * SecurityColumnConfig
 * @author dtssv
 * @date 2020/11/23 15:12
 * @return
 **/
@Data
public class SecurityColumnConfig {
    /**
     * table
     */
    private String table;
    /**
     * sourceColumn
     */
    private String sourceColumn;
    /**
     * targetColumn
     */
    private String targetColumn;
    /**
     * indexColumn
     */
    private String indexColumn;

}
