package com.kakao.bot.selectdb;

import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

public class SelectDBDAO {
	SqlSessionFactory sqlSession = SqlSessionManager.getSqlSession();
	public List<String> getDataLike(String message) {
		SqlSession session = sqlSession.openSession();
		try {
			SelectBotMapper mapper = session.getMapper(SelectBotMapper.class);
			return mapper.getLike(message);
		} finally {
			session.close();
		}
	}
	
	public String getData(String message) {
		SqlSession session = sqlSession.openSession();
		try {
			SelectBotMapper mapper = session.getMapper(SelectBotMapper.class);
			return mapper.get(message);
		} finally {
			session.close();
		}
	}	
}
