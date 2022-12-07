// ignore_for_file: camel_case_types

import 'dart:async';





import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'dart:developer';

import 'package:flutter_easyloading/flutter_easyloading.dart';

import 'chat_room.dart';
import 'navigation_service.dart';



class PortalManager{

  static final PortalManager _singleton = PortalManager._internal();

  factory PortalManager() {
    return _singleton;
  }

  PortalManager._internal();

  Stream<WifiList> get wifiList => _wifiListStreamController.stream;

  final StreamController<WifiList> _wifiListStreamController =
  StreamController.broadcast();

  final StreamController<MessageList> _messageStreamController =
  StreamController.broadcast();

  Stream<MessageList> get messageList => _messageStreamController.stream;

  static const methodChannel = MethodChannel("com.liyobor.android_wifi_direct.method");
  static const eventStream = EventChannel("com.liyobor.android_wifi_direct.event");
  late final StreamSubscription _eventSubscription;

  final List<Widget> messageBox = [];

  void init() {
    _eventSubscription = eventStream.receiveBroadcastStream().listen(onUpdate,
        onError: (dynamic error) {
          log('Received error: ${error.message}');
        });
  }

  // void sendMessage(String message) {
  //   invokeMethod("sendMessage", message);
  // }

  void createServerSocketThread() {
    invokeMethod("createServerSocketThread", null);
  }

  void discoverPeers() {
    invokeMethod("discoverPeers", true);
  }

  void connectToDevice(int index){
    invokeMethod("connectToDevice", index);
  }

  void connectToServerTCP(String ip,String port){

    int portInt = int.parse(port);
    var map = {
      'port':portInt,
      'ip':ip,
    };
    invokeMethod("connectToServerTCP", map);
  }

  void closeSocket(){
    invokeMethod("closeSocket", null);
  }

  void upload(){
    invokeMethod("upload", null);
  }


  void sendMessage(String text){
    invokeMethod("sendMessage", text);
    messageBox.insert(
        0,
        Container(
          alignment: Alignment.centerRight,
          child: Text(
            text,
            style: const TextStyle(fontSize: 50),
          ),
        )
    );
    _messageStreamController.sink.add(MessageList(box: messageBox));
  }




  Future<dynamic> invokeMethod(String item, dynamic data) async {
    try {
      return await methodChannel.invokeMethod(item, data);
    } on PlatformException catch (e) {
      log("error result is $e");
    }
    return false;
  }

  void onUpdate(dynamic data) {
    if(data["wifiList"]!=null){
      List wifiList = data["wifiList"];
      _wifiListStreamController.sink.add(WifiList(wifiList: wifiList));
      if(wifiList.isNotEmpty){
        EasyLoading.dismiss();
      }
    }
    if(data["receivedMessage"]!=null){
      String receivedMessage = data["receivedMessage"];
      messageBox.insert(
          0,
          Container(
            alignment: Alignment.centerLeft,
            child: Text(
              receivedMessage,
              style: const TextStyle(fontSize: 50),
            ),
          ));
      _messageStreamController.sink.add(MessageList(box:messageBox));
    }

    if(data["enterChat"]!=null){

      NavigationService.navigatorKey.currentState?.pushNamed("/chat");
    }
  }


  void dispose() {

    _eventSubscription.cancel();
    _wifiListStreamController.close();
  }
}


@immutable
class PortalState {
  const PortalState(
      this.flashOn,
      this.vibrateOn, {
        required this.isRecording,
      });
  final bool flashOn;
  final bool vibrateOn;
  final bool isRecording;
}



@immutable
class WifiList{
  const WifiList(
      {required this.wifiList});
  final List wifiList;
}


@immutable
class MessageList{
  const MessageList(
      {required this.box});
  final List<Widget> box;
}
