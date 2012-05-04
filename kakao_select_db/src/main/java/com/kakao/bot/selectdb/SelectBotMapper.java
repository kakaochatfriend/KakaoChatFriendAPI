package com.kakao.bot.selectdb;

import java.util.List;

import org.apache.ibatis.annotations.Select;

public interface SelectBotMapper {
	@Select("select data from databox where message like CONCAT('%',#{message},'%') LIMIT 3")
	List<String> getLike(String message);
	
	@Select("select data from databox where message=#{message}")
	String get(String message);

}
