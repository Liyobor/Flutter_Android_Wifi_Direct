// ignore_for_file: camel_case_types

import 'dart:async';



import 'package:flutter/cupertino.dart';
import 'package:flutter/services.dart';
import 'dart:developer';

import 'package:flutter_easyloading/flutter_easyloading.dart';



class PortalManager{

  static final PortalManager _singleton = PortalManager._internal();

  factory PortalManager() {
    return _singleton;
  }

  PortalManager._internal();

  Stream<WifiList> get wifiList => _wifiListStreamController.stream;

  final StreamController<WifiList> _wifiListStreamController =
  StreamController.broadcast();

  static const methodChannel = MethodChannel("com.liyobor.android_wifi_direct.method");
  static const eventStream = EventChannel("com.liyobor.android_wifi_direct.event");
  late final StreamSubscription _eventSubscription;

  void init() {
    _eventSubscription = eventStream.receiveBroadcastStream().listen(onUpdate,
        onError: (dynamic error) {
          log('Received error: ${error.message}');
        });
  }

  void sendMessageToServer() {
    invokeMethod("sendMessageToServer", null);
  }

  void createServerSocketThread() {
    invokeMethod("createServerSocketThread", null);
  }

  void discoverPeers() {
    invokeMethod("discoverPeers", true);
  }

  void connectToDevice(int index){
    invokeMethod("connectToDevice", index);
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
