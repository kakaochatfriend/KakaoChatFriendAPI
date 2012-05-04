package com.kakao.bot.selectdb;

public class App2 {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		SelectDBDAO dao = new SelectDBDAO();
		System.out.println(dao.getDataLike("된장"));
		System.out.println(dao.getDataLike("고추장"));
		System.out.println(dao.getDataLike("cheese"));
	}

}
