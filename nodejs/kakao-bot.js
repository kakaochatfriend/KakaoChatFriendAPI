var net = require("net"),
	sys = require("util"),
	events = require("events"),
	BSON = require("buffalo"),
	Binary = require("binary"),
	log4js = require('log4js');

var logger = log4js.getLogger("kakao-bot");

function Client(options) {
	events.EventEmitter.call(this);

	options = options || {};
	this.connectionTimeout = (options["connectionTimeout"] === undefined ? 3000 : options["connectionTimeout"]);
}

sys.inherits(Client, events.EventEmitter);

exports.Client = Client;

exports.createClient = function(options) {
	return new Client(options);
}

Client.prototype.connect = function(host, port, botId, password) {
	logger.info("[CONNECTING] Connecting to " + host + ":" + port);
	this.connection = net.createConnection(port, host);
	this.connection.setTimeout(this.connectionTimeout);

	this.host = host;
	this.port = port;
	this.botId = botId;
	this.password = password;

	// event handlers
	this.connection.on("error", errorHandler(this));
	this.connection.on("timeout", timeoutHandler(this));
	this.connection.on("connect", connectHandler(this));
	this.connection.on("close", closeHandler(this));

	// data handler
	var client = this;
	Binary.stream(this.connection)
	    .loop(function(end, vars) {
        	var self = this;
	        this.word32lu('len')
				.tap(function(vars_) {
					self.buffer('body', vars_.len - 4)
						.tap(function(vars__) {
							logger.debug("[DATA] "+ sys.inspect(vars__));

							var buffer = Binary.put()
									.word32le(vars__.len)
									.put(vars__.body)
									.buffer();

							var response = BSON.parse(buffer);
							client.processResponse(response);
                		});
            	})
    	})

}

var connectHandler = function(self) {
	return function() {
		logger.info("[CONNECTED] Connected to " + self.host + ":" + self.port);

		self.connection.setNoDelay();
		self.connection.setTimeout(0);

		self.login(self.botId, self.password);
	}
}

var closeHandler = function(self) {
	return function(hadError) {
		if (hadError) {
			logger.error("[ERROR] Connection failed");
		} else {
			logger.info("[DISCONNECTED]");
		}
	}
}

var timeoutHandler = function(self) {
	return function() {
		logger.error("[TIMEOUT] Connection timed out");
		self.connection.end();

		self.emit("error", "Connection timed out");
	}
}

var errorHandler = function(self) {
	return function(e) {
		logger.error("[ERROR] " + e);
		self.emit("error", e);
	}
}

Client.prototype.send = function(command) {
	var packet = BSON.serialize(command);
	this.connection.write(packet, "binary");
}

Client.prototype.login = function(botId, password) {
	var loginCommand = {
		"type": "login",
		"id": botId,
		"pass": password
	};

	this.send(loginCommand);
}

Client.prototype.pong = function(time) {
	var pongCommand = {
		"type": "pong",
		"time": time
	};

	this.send(pongCommand);
}

Client.prototype.sendMessage = function(user_key, room_key, msgId, message) {
	var messageCommand = {
		"type": "response",
		"user_key": user_key,
		"room_key": room_key,
		"msg_id": msgId,
		"message": message
	};

	this.send(messageCommand);
}

Client.prototype.processResponse = function(response) {
	logger.info("[RECV] response: " + sys.inspect(response));

	var type = response["type"];

	switch (type) {
	case "result":
		// login response
		var code = response["code"];
		var msg = response["msg"];

		if (code == 200) {
			logger.info("[AUTHENTICATED]");
		} else {
			this.emit("error", msg);
			this.connection.end();
		}
		break;
	case "add":
		var user_key = response["user_key"];
		var room_key = response["room_key"];
		var msgId = response["msg_id"];
		this.emit("add", user_key, room_key, msgId);
		break;
	case "request":
		var user_key = response["user_key"];
		var room_key = response["room_key"];
		var msgId = response["msg_id"];
		var countryIso = response["country_iso"];
		var message = response["message"];
		this.emit("message", user_key, room_key, msgId, countryIso, message);
		break;
	case "ping":
		var time = response["time"];
		this.pong(time);
		break;
	}
}
