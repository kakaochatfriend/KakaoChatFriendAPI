package main

import (
	"github.com/garyburd/go-mongo/mongo"
	"net"
	"os"
	"fmt"
	"encoding/binary"
	"time"
)

const (
	HOST="127.0.0.1"
	PORT=8080
	BUFMAX=10000
	ID="test"
	PASS="test"
)


func main() {
	conn, err := net.Dial("tcp", fmt.Sprintf("%s:%d",HOST,PORT))
	checkError(err)

	defer conn.Close()

	var loginPkt []byte
	loginPkt, err2 := mongo.Encode(loginPkt,map[string]interface{}{
		"type":"login",
		"id":ID,
		"pass":PASS,
	})

	checkError(err2)

	conn.Write(loginPkt)

	doClient(conn)

}

func doClient(conn net.Conn) {
	var buf [BUFMAX]byte
	for {
		var n int
		n, err := conn.Read(buf[:4])
		if err != nil {
			fmt.Println("1 Fatal error", err.Error())
			return
		} else if n != 4 {
			fmt.Println("Size under 4", n)
			return
		}

		pktLen := binary.LittleEndian.Uint32(buf[:4])

		if pktLen < 0 || pktLen > BUFMAX {
			fmt.Println("invalid pkt size ", pktLen)
			return
		}

		n, err = conn.Read(buf[4:pktLen])
		if err != nil {
			fmt.Println("Err", err)
			return
		}

		go handleRead(conn, buf[:pktLen])
	}
}

func writeBson(conn net.Conn, m map[string]interface{}) {
	var pkt []byte
	pkt, err := mongo.Encode(pkt, m)
	if err != nil {
		fmt.Println("writeBson Error", err)
		return
	}
	conn.Write(pkt)
}


func handleRead(conn net.Conn, bt []byte) {

	m := map[string]interface{}{}

	err := mongo.Decode(bt, m)

	if err != nil {
		fmt.Println("Err,", err)
		return
	}

	fmt.Println("Recv Packet ,", m)

	switch m["type"] {
		// user_key, room_key, msg_id, message, country_iso, login_id, attachment
	case "request":
		writeBson(conn,map[string]interface{}{
			"type":"response",
			"user_key":m["user_key"],
			"room_key":m["room_key"],
			"msg_id":m["msg_id"],
			"message":m["message"],
		})
	case "add":
		writeBson(conn, map[string]interface{}{
			"type":"response",
			"user_key":m["user_key"],
			"room_key":m["room_key"],
			"msg_id":m["msg_id"],
			"message":"Welcome to Echobot...",
		})
	case "ping":
		writeBson(conn, map[string]interface{}{
			"type":"pong",
			"time":time.Now().Unix(),
		})
	case "result":
	case "block":
	}
}

func checkError(err error) {
	if err != nil {
		fmt.Println("Fatal error ", err.Error())
		os.Exit(1)
	}
}
