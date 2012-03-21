# 날씨봇

날씨봇은 kakaotalk bot api에 대한 php 구현체 샘플입니다.

## weatherbot 구동법

sudo pecl install mongo

php.ini에 extension=mongo.so 추가

php echobot.php apihost port id auth_key

## Usage

Custom BotMessageHandler 작성

	class MyBotMessageHandler extends BotMessageHandler {
		function onAdd($bot, $req) {
		}
	
		function onRequestMessage($bot, $req) {
		}
	
		function onResult($bot, $req) {
		}
	}
 
bot api server connection

	try {
		$bot = new KakaoBot($addr, $port, new MyBotMessageHandler());
	} catch(Exception $e) {
		echo "error trying connect to $addr:$port message: " . $e -> getMessage() . "\n";
		exit(-1);
	}

login

	try {
		$bot -> login($id, $pw);
	} catch(Exception $e) {
		$bot -> disconnect();
		exit(-1);
	}

message processing

	while (true) {
		try {
			$bot -> process();
		} catch(Exception $e) {
			echo "socket error msg: " . $e -> getMessage() . "\n";
			$bot -> disconnect();
			$bot -> connect();
		}
	}
