<?php
include 'KakaoBot.class.php';

class EchoMessageHandler extends BotMessageHandler {
	function onAdd($bot, $req) {
		echo "Add message received " . print_r($req, true);
		$res = array("type" => "response", "user_key" => new MongoInt64($req["user_key"]), "room_key" => new MongoInt64($req["room_key"]), "msg_id" => (int)$req["msg_id"], "message" => "환영");

		$bot -> send($res);
	}

	function onRequestMessage($bot, $req) {
		echo "req message received " . print_r($req, true);
			$res = array("type" => "response", "user_key" => new MongoInt64($req["user_key"]), "room_key" => new MongoInt64($req["room_key"]), "msg_id" => (int)$req["msg_id"], "message" => $req["message"]);
		$bot -> send($res);
	}

	function onResult($bot, $res) {
		echo print_r($res, true);
	}

	function onLeave($bot, $req) {
		echo "leave message received " . print_r($req, true);
	}

	function onBlock($bot, $req) {
		echo "block message received " . print_r($req, true);
	}
}

if(count($argv) != 5) {
	echo "usage php weatherbot.php <host> <port> <id> <key>\n";
	exit(-1);
}
$addr = $argv[1];
$port = $argv[2];
$id = $argv[3];
$pw = $argv[4];
$login = false;
try {
	$bot = new KakaoBot($addr, $port, new EchoMessageHandler());
} catch(Exception $e) {
	echo "error trying connect to $addr:$port message: " . $e -> getMessage() . "\n";
	exit(-1);
}

try {
	$bot -> login($id, $pw);
	$login = true;
} catch(Exception $e) {
	$bot -> disconnect();
	exit(-1);
}

while (true) {
	try {
		if(!$login)
			$bot -> login($id, $pw);
			$login = true;
		$bot -> process();
	} catch(Exception $e) {
		echo "socket error msg: " . $e -> getMessage() . "\n";
		$bot -> disconnect();
		sleep(10); // reconnect after 10 sec
		$login = false;
		$bot -> connect();
	}
}
?>
