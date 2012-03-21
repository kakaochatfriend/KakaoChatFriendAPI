<?php
define('RESULT_OK', 200);

class KakaoBot {

	var $socket;
	var $address;
	var $port;
	var $handler;
	var $rbuf;

	function KakaoBot($address, $port, $handler) {
		$this -> address = $address;
		$this -> port = $port;
		$this -> handler = $handler;

		if (!$this -> connect()) {
			throw new Exception("connection error");
		}
	}

	function connect() {
		$this -> socket = socket_create(AF_INET, SOCK_STREAM, SOL_TCP);
		return socket_connect($this -> socket, $this -> address, $this -> port);
	}

	function disconnect() {
		socket_close($this -> socket);
		$this -> socket = null;
	}

	function login($bot_id, $auth_key) {
		$msg = array("type" => "login", "id" => $bot_id, "pass" => $auth_key);
		$this -> send($msg);

		$ret = $this -> receive();

		if ($ret["code"] != RESULT_OK) {
			throw new Exception("login error code : " . $ret["code"] . ", msg : " . $ret["msg"]);
		}
	}

	function send($msg) {
		$serialized_message = bson_encode($msg);
		$len = strlen($serialized_message);
		$off = 0;

		while ($off < $len) {
			$len_write = socket_write($this -> socket, $off == 0 ? $serialized_message : $substr($serialized_message, $off), $len - $off);

			if (!$len_write)
				throw new Exception("write error");

			$off += $len_write;
		}
	}

	function receive() {
		$this -> rbuf = '';
		$ret = socket_recv($this -> socket, $this -> rbuf, 4, MSG_PEEK | MSG_WAITALL);

		if (!$ret)
			throw new Exception("socket read error");

		$msg_len = unpack('N', strrev($this -> rbuf));

		$this -> rbuf = '';

		echo "receiving message size = $msg_len[1]";

		$ret = socket_recv($this -> socket, $this -> rbuf, $msg_len[1], MSG_WAITALL);
		if (!$ret)
			throw new Exception("socket read error");

		return bson_decode($this -> rbuf);
	}

	function process() {
		$msg = $this -> receive();

		switch($msg["type"]) {
			case "add" :
				$this -> handler -> onAdd($this, $msg);
				break;
			case "request" :
				$this -> handler -> onRequestMessage($this, $msg);
				break;
			case "ping" :
				$this -> handler -> onPing($this, $msg);
				break;
			case "result" :
				$this -> handler -> onResult($this, $msg);
				break;
			case "leave" :
				$this -> handler -> onLeave($this, $msg);
				break;
			case "block" :
				$this -> handler -> onBlock($this, $msg);
				break;
			default :
				echo "unknown message : " . $msg["type"];
		}
	}

}

class BotMessageHandler {
	function onAdd($bot, $req) {
	}

	function onRequestMessage($bot, $req) {
	}

	function onPing($bot, $req) {
		echo "ping arrived";
		$res = array("type" => "pong", "time" => $req["time"]);

		$bot -> send(res);
	}

	function onResult($bot, $req) {
	}
	
	function onLeave($bot, $req) {
	}

	function onBlock($bot, $req) {
	}
}
?>
