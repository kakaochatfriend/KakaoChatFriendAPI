#pragma once

#include <iostream>
#include <string>
#include <vector>
#include <set>
#include <unordered_map>

#include <boost/asio.hpp>
#include <boost/thread.hpp>
#include <boost/lexical_cast.hpp>
#include <boost/bind.hpp>
#include <boost/shared_ptr.hpp>

#include <mongo/bson/bson.h>

namespace Replice
{
	using namespace std;
	using boost::asio::ip::tcp;

	enum Type
	{
		RESULT = 0,
		ADD,
		BLOCK,
		LEAVE,
		REQUEST,
		UNKNOWN = 255
	};

	class Client 
	{
		boost::thread_group threads;
		boost::asio::io_service& io;
		tcp::socket	socket_;
		tcp::endpoint endpoint;
	
		string recv_buffer;
		string write_buffer;
		string login_id;
		string auth_key;
		boost::mutex write_lock;
		
		unordered_map< string, function<void (const Type type, const mongo::BSONObj& msg)> > handler;
		bool logged_in;
		bool is_writing;

		unordered_map< string, Type > type_map;

	public:
		Client(boost::asio::io_service& _io, string addr, string port, string _login_id, string _auth_key)
			: io(_io), socket_(_io), login_id(_login_id), auth_key(_auth_key)
		{
			tcp::resolver resolver(io);
			tcp::resolver::query query(addr, port);
			endpoint = *resolver.resolve(query);
			
			type_map["add"] = Type::ADD;		
			type_map["result"] = Type::RESULT;		
			type_map["block"] = Type::BLOCK;		
			type_map["leave"] = Type::LEAVE;		
			type_map["request"] = Type::REQUEST;		

			connect();
		}
		
		void reconnect(int delay)
		{
			if( socket_.is_open() )
				socket_.close();
			sleep(delay);
			connect();
		}

		void connect()
		{
			logged_in = false;	
			cout << "try to connect " << endpoint << "..." << endl;
			socket_.async_connect(endpoint,
				[=](const boost::system::error_code& ec)
				{
					if( ec )
					{
						cout << ec.message() << endl;
						reconnect(5);
						return;
					}
					cout << "connected." << endl;
					mongo::BSONObj msg = BSON( "type" << "login" << "id" << login_id << "pass" << auth_key );
					boost::asio::async_write(socket_,
						boost::asio::buffer(msg.objdata(), msg.objsize()),
						[this](const boost::system::error_code& ec, size_t bytes_transferred)
						{
							this->receive_bson();
						}
					);
				}
			);
		}

		void start(int nthreads = 1)
		{

			for(int i = 0; i < nthreads; i++)
				threads.create_thread([this](){this->io.run();});
		}

		void join()
		{
			threads.join_all();
		}

		void receive_bson()
		{
			recv_buffer.resize(4);
			boost::asio::async_read(
				socket_,
				boost::asio::buffer(&recv_buffer[0], recv_buffer.size()),
				[=](const boost::system::error_code& ec, size_t bytes_transferred)
				{
					if( ec )
					{
						cout << ec.message() << endl;
						reconnect(5);
						return;
					}
					int len = *reinterpret_cast<int*>(&recv_buffer[0]);
					if( len < 4 )
					{
						cout << "invalid message length" << endl;
						return;
					}
					receive_bson_body(len);
				}
			);
		}

		void receive_bson_body(int len)
		{
			recv_buffer.resize(len);
			boost::asio::async_read(
				socket_,
				boost::asio::buffer(&recv_buffer[4], len-4),
				[=](const boost::system::error_code& ec, size_t bytes_transferred)
				{
					if( ec )
					{
						cout << ec.message() << endl;
						reconnect(5);
						return;
					}
					io.post([this, recv_buffer](){this->handle_message(mongo::BSONObj(recv_buffer.data()).copy());});
					receive_bson();
				}
			);
		}

		void send_write_buffer()
		{
			boost::shared_ptr<std::string> v(new std::string());
			v->swap(write_buffer);
			boost::asio::async_write(socket_,
					boost::asio::buffer(*v),
					[this, v](const boost::system::error_code& ec, size_t bytes_transferred)
					{   
						if (!ec)
						{   
							boost::lock_guard<boost::mutex> lock(write_lock);
							if (write_buffer.empty())
							{   
								is_writing = false;
							}   
							else
							{   
								send_write_buffer();
							}   
						}   
						else
						{   
							cout << ec.message() << endl;
				
						}   
					}
				); 
		}

		void send(const mongo::BSONObj& msg)
		{   
			boost::shared_ptr<std::string> v(new std::string());
			v->resize(msg.objsize());
			memcpy(&((*v)[0]), msg.objdata(), msg.objsize());

			boost::lock_guard<boost::mutex> lock(write_lock);
			if (is_writing)
			{   
				write_buffer += *v; 
				return;
			}   

			is_writing = true;

			boost::asio::async_write(socket_,
					boost::asio::buffer(*v),
					[this, v](const boost::system::error_code& ec, std::size_t bytes_transferred)
					{   
						if (!ec)
						{
							boost::lock_guard<boost::mutex> lock(write_lock);
							if (write_buffer.empty())
							{
								is_writing = false;
							}
							else
							{
								send_write_buffer();
							}
						}
						else
						{
							std::cout << "ERR:\tSend error\t" << ec << '\t' << bytes_transferred << std::endl;
						}
					}
				);
			}

		void handle_message(const mongo::BSONObj& msg)
		{
			if(!logged_in)
			{
				handle_login_result(msg);
				return;
			}
			string str_type = msg["type"].String();
			if( str_type == "ping" )
			{
				bson::bo pong = BSON( "type" << "pong" << "time" << msg["time"] );
				send(pong);
				return;
			}
			Type type = Type::UNKNOWN;
			if( type_map.find(str_type) != type_map.end() )
				type = type_map[str_type];

			for(auto it = handler.begin(); it !=  handler.end(); ++it)
				(it->second)(type, msg);
		}

		void handle_login_result(const mongo::BSONObj& msg)
		{
			if (msg["type"].String() == "result" &&
				msg["code"].Int() == 200 )
			{
				logged_in = true;
				cout << "loggin success." << endl;
			}
			else
			{
				cout << "loggin failed." << endl;
				exit(1);
			}
		}

		void add_message_handler(string name, const function<void (const Type type, const mongo::BSONObj&)>& func)
		{
			handler[name] = func;
		}

		bool remove_message_handler(string name)
		{
			return 0 != handler.erase(name);
		}

		void makeResponse(const mongo::BSONObj& req, mongo::BSONObjBuilder& res)
		{
			res << "type" << "response" << "user_key" << req["user_key"] << "room_key" << req["room_key"] << "msg_id" << req["msg_id"];
		}
	};

	typedef boost::shared_ptr<Client> ClientPtr;
};
