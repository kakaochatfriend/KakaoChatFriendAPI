CC=g++
MONGO_PATH=.
CXXFLAGS=-g -I$(MONGO_PATH) --std=c++0x
LIBS=-lboost_thread-mt -lboost_system-mt

TARGET=echo_server

all: $(TARGET)

$(TARGET): echo.cc RepliceClient.hpp
	$(CC) -o $@ echo.cc $(CXXFLAGS) $(LIBS)

clean:
	rm -f echo_server

tar:
	tar cvzf RepliceCpp.tar.gz Makefile echo.cc RepliceClient.hpp README.md
