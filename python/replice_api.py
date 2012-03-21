import gevent
from gevent import monkey, queue, socket, pool

import bson
import logging

monkey.patch_all()

class RepliceClient(object):

  def __init__(self, host, port, login_id, auth_key):
    self.host, self.port = host, port
    self.login_id, self.auth_key = login_id, auth_key
    self._socket = None
    self._recv_queue = queue.Queue()
    self._send_queue = queue.Queue()
    self._group = pool.Group()

    self.handlers = {}

    self._send = lambda _: self._send_queue.put(_)

    self.login = False

  def convert_unicode(self, s):
    if not isinstance(s, unicode):
      return s.decode('utf-8')
    return s

  def send_message(self, user_key, room_key, msg_id, message = None, messages = None):
    packet = {
      u'type' : u'response',
      u'user_key' : long(user_key),
      u'room_key' : long(room_key),
      u'msg_id' : msg_id
    }
    
    if message:
      packet[u'message'] = self.convert_unicode(message)
    if messages:
      packet[u'messages'] = [self.convert_unicode(s) for s in messages]
    
    self._send(packet)
 
  def start(self):
    addr = None
    try: 
      addr = (socket.gethostbyname(self.host), self.port)
    except socket.gaierror:
      logging.error('hostname not found')
      raise

    logging.info(u'try to connect ... %r'%(addr,))
    self._socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    self._socket.connect(addr)
    self._group.spawn(self._send_loop)
    self._group.spawn(self._process_loop)
    self._group.spawn(self._recv_loop)

    gevent.sleep(0)

    self._send({ u'type' : u'login', u'id' : unicode(self.login_id), u'pass' : unicode(self.auth_key) })

  def _recv_loop(self):
    while True:
      self._recv_queue.put(self._socket.recv(8192))

  def _process_loop(self):
    buf = ''
    while True:
      buf += self._recv_queue.get()
      while len(buf) > 4:
        n = bson.network._bintoint(buf[:4])
        if len(buf) < n:
          break
        packet, buf = bson.loads(buf[:n]), buf[n:]

        gevent.sleep(0)
        logging.debug(u'received ' + repr(packet))
        if not self.login:
          if packet[u'type'] == u'result' and packet[u'code'] == 200:
            self.login = True
            logging.info(u'login success.')
          else:
            logging.error(u'login error : code(%(code)d) msg(%(msg)s)'%packet)

        else: 
          event_type = packet.pop(u'type')
          if u'message' in packet:
            packet[u'message'] = packet[u'message'].encode('utf-8')
          if event_type in self.handlers:
            self.handlers[event_type](self, **packet)
          else:
            logging.warning(u'unhandled event : %s'%event_type)

  def _send_loop(self):
    while True:
      packet = self._send_queue.get()
      self._socket.sendall(bson.dumps(packet))
      logging.debug(u'sent ' + repr(packet))

  def on(self, event_type, func):
    self.handlers[event_type] = func

  def stop(self):
    self._group.kill()
    if self._socket is not None:
      self._socket.close()
      self._socket = None

  def join(self):
    self._group.join()

