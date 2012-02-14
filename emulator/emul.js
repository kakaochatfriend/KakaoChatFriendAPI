var net = require('net');
var bson = require('mongodb').BSONPure;
var conf = require('./emul.cfg');

var msg_id = 0;

var stdin = process.openStdin();
var stdout = process.stdout;

var params = { 'user_key' : bson.Long.fromInt(0), 'room_key' : bson.Long.fromInt(0), 'country_iso' : 'KR' };

var results = {
	200 : "정상",
	403 : "로그인이 필요합니다",
	404 : "로그인 실패",
	443 : "이미 로그인되어 있습니다",
	444 : "정상적이지 않은 행위가 감지되어 접속을 종료합니다",
	501 : "잘못된 세션키입니다",
	502 : "해당 세션키의 발송제한을 초과하였습니다",
	503 : "잘못된 메세지 형식입니다",
	604 : "해당 USER가 없습니다",
	605 : "연결되어 있는 REU Server가 없습니다",
	606 : "접속된 TAYO Server가 없거나 라우팅 정보가 없습니다",
	607 : "메세지 응답 시간 초과입니다. 메세지가 전송되지 않습니다",
	701 : "서버 오류로 메세지 전송에 실패 하였습니다"
};

stdin.pause();

var conn = null;

var showPrompt = function() { stdout.write('> '); stdin.resume(); };

var server = net.createServer(function(c) {
	console.log('connected');

	var sendResult = function(code) {
		var msg = { type : 'result', code : code, msg : results[code] };
		c.write(bson.BSON.serialize(msg));
	};
	
	var packet_handler = {
		'login' : function(packet) {
			if( conf.id != packet.id || conf.pass != packet.pass )
			{
				sendResult(404);
				console.log('... login failed');
			}
			else
			{
				sendResult(200);
				console.log('... login success');
			}
		},
		'response' : function(packet) {
			if( 'message' in packet )
				console.log('... message : ' + packet.message);
			if( 'messages' in packet )
				packet.messages.forEach(function(e, i, a) {
					console.log('... messages ' + i + ' : ' + e);
				});
			sendResult(200);
		}
	};	

	c.on('data', function(data) {
		var packet = {};
		try {
			packet = bson.BSON.deserialize(data);
		}
		catch(err) {
			console.log('... invalid bson format : ' + err);
			showPrompt();
			return;
		}
		console.log('... received : ' + JSON.stringify(packet));
		if( !('type' in packet) )
		{
			console.log('... invalid packet format : ' + JOSN.stringify(packet));
			showPrompt();
			return;
		}
		if( packet.type in packet_handler )
			packet_handler[packet.type](packet);
		else
			console.log('... unhandled packet type : ' + packet.type);
		showPrompt();
	});
	
	conn = c;
});

server.listen(conf.port, function() {
	console.log('listen with port ' + conf.port + '...');
});

var command_handler = {
	'set' : function(args) {
		if( args[0] in params && args.length == 2 )
		{
			if( isNaN(args[1]) )
				params[args[0]] = args[1];
			else
				params[args[0]] = bson.Long.fromString(args[1]);
			
			console.log('OK');
		}
		else
			console.log('invalid set command');
		showPrompt();
	},
	'show' : function(args) {
		if( args[0] in params )
			console.log(args[0] + ' : ' + params[args[0]]); 
		else if( args[0] == 'all' )
			console.log(params);
		else
			console.log('invalid get command');
		showPrompt();
	},
	'event' : function(args) {
		if( conn == null )
		{
			console.log('... not connected');
			return;
		}
		var msg = {
			type : args[0],
			user_key : params.user_key,
			room_key : params.room_key,
			msg_id : msg_id,
			country_iso : params.country_iso
		};
		console.log(msg);
		conn.write(bson.BSON.serialize(msg));
	},
	'exit' : function(args) {
		process.exit(0);
	}
};


stdin.on('data', function(data) {
	stdin.pause();
	
	var cmd = String(data).replace('\n', '').replace('\r','');
	if( cmd[0] == '/' )
	{
		args = cmd.substring(1).split(' ');
		stdout.write('... ');
		if( args[0] in command_handler )
			command_handler[args[0]](args.slice(1));
		else
		{
			console.log('unknown command : ' + args[0]);
			showPrompt();
		}
	}
	else
	{
		if( conn == null )
		{
			console.log('... not connected');
			return;
		}
		var msg = { 
			type : 'request', 
			user_key : params.user_key, 
			room_key : params.room_key, 
			msg_id : msg_id, 
			message : cmd, 
			country_iso : params.country_iso 
		};
		console.log('... send : ' + JSON.stringify(msg));
		conn.write(bson.BSON.serialize(msg));
		msg_id++;
	}
});

