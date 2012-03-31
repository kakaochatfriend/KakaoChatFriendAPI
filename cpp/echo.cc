#include <iostream>
#include "RepliceClient.hpp"

int main()
{
	boost::asio::io_service io;
	Replice::ClientPtr r(new Replice::Client(io, "localhost", "11111", "test", "test"));

	r->add_message_handler("default", [=](const Replice::Type type, const mongo::BSONObj& msg) {
		std::cout << "default handler recv : " << msg << std::endl;
		switch(type)
		{
			case Replice::Type::ADD:
				{
					mongo::BSONObjBuilder res_builder;
					r->makeResponse(msg, res_builder);
					res_builder << "message" << "hihi";
					bson::bo res = res_builder.obj();
					r->send(res);
					std::cout << "default handler sent : " << res << std::endl;
				}
				break;
			case Replice::Type::REQUEST:
				{
					mongo::BSONObjBuilder res_builder;
					r->makeResponse(msg, res_builder);
					res_builder << "message" << msg["message"];
					bson::bo res = res_builder.obj();
					r->send(res);
					std::cout << "default handler sent : " << res << std::endl;
				}
				break;
			case Replice::Type::RESULT:
				return;
		};
	});
	r->start(4);
	r->join();
}
