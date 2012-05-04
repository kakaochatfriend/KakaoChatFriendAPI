package com.kakao.bot.selectdb;

import javax.sql.DataSource;

import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

public class SqlSessionManager {
	public static SqlSessionFactory sqlSession;
	static {
		DataSource dataSource = new PooledDataSource("com.mysql.jdbc.Driver",
				"jdbc:mysql://localhost:3306/test?useTimezone=true&amp;serverTimezone=UTC&amp;characterEncoding=utf8&amp;debugSQL=true",
				"root",
				"");

		TransactionFactory transactionFactory = new JdbcTransactionFactory();
		Environment environment = new Environment( "dev", transactionFactory, dataSource );
		Configuration configuration = new Configuration( environment );
		configuration.addMapper( SelectBotMapper.class );
		sqlSession = new SqlSessionFactoryBuilder().build( configuration );
	}
	
	public static SqlSessionFactory getSqlSession() {
		return sqlSession;
	}
}
