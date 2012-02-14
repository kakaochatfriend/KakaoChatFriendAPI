# -*- coding: utf-8 -*-
import asyncore, socket
import bson
from StringIO import StringIO
from Queue import Queue
from struct import unpack
import logging

class RepliceClient(asyncore.dispatcher):

  def __init__(self, host, port, login_id, auth_key):
    asyncore.dispatcher.__init__(self)
    self.create_socket(socket.AF_INET, socket.SOCK_STREAM)
    self.login_id, self.auth_key = login_id, auth_key
    self.send_queue = Queue()
    self._send = lambda _: self.send_queue.put(_)
     
    # patch socket
    self.sendobj = lambda _1: bson.network._sendobj(self, _1)
    self.recvobj = lambda : bson.network._recvobj(self)
    self.recvbytes = lambda _1, _2=None: bson.network._recvbytes(self, _1, _2)

    self.handler = {}
   
    logging.info(u'try to connect ... %s:%d'%(host, port))
    self.connect((host, port))
    self._send({ u'type' : u'login', u'id' : unicode(self.login_id), u'pass' : unicode(self.auth_key) })
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
  
  def handle_connect(self):
    pass

  def handle_close(self):
    self.close()
  
  def writable(self):
    return not self.send_queue.empty()

  def handle_write(self):
    while not self.send_queue.empty():
      data = self.send_queue.get()
      logging.debug(u'send ' + repr(data))
      self.sendobj(data)

  def handle_read(self):
    packet = self.recvobj()
    if packet is None:
      return

    logging.debug(u'received ' + repr(packet))
    if not self.login:
      if packet[u'type'] == u'result' and packet[u'code'] == 200:
        self.login = True
        logging.info(u'login success.')
      else:
        logging.error(u'login error : code(%{code}d) msg(%{msg}s)'%packet)
      return

    event_type = packet.pop(u'type')
    if u'message' in packet:
      packet[u'message'] = packet[u'message'].encode('utf-8')
    if event_type in self.handler:
      self.handler[event_type](self, **packet)
    else:
      logging.warning(u'unhandled event : %s'%event_type)

  def on(self, event_type, func):
    self.handler[self.convert_unicode(event_type)] = func
