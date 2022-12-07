

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'portal_manager.dart';

class TCPPage extends StatefulWidget {
  const TCPPage({Key? key}) : super(key: key);
  @override
  State<TCPPage> createState() => _TCPPageState();
}

class _TCPPageState extends State<TCPPage> {

  final TextEditingController _ipTextController = TextEditingController(text: "192.168.4.1");
  final TextEditingController _portTextController = TextEditingController(text: "9090");

  @override
    Widget build(BuildContext context) => Scaffold(
      resizeToAvoidBottomInset: false,
      appBar: AppBar(
        centerTitle: true,
        title: const Text("TCP"),
      ),
      body: Consumer2<PortalManager,WifiList>(
        builder: (_, pMgr,wList, __) => SizedBox(
          child:
          Column(
            children: <Widget>[
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 24.0, vertical: 16.0),
                child: TextFormField(
                  controller: _ipTextController,
                  keyboardType: TextInputType.number,
                  decoration: const InputDecoration(
                    labelText: "IP Address",
                    // hintText: "輸入提示(可改)",
                  ),
                ),
              ),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 24.0, vertical: 16.0),
                child: TextFormField(
                  controller: _portTextController,
                  keyboardType: TextInputType.number,
                  decoration: const InputDecoration(
                    labelText: "Port",
                    // hintText: "輸入提示(可改)",
                  ),
                ),
              ),
              Row(
                children: [
                  SizedBox(
                    height: 48.0,
                    child: TextButton(
                      child: const Text("start"),
                      onPressed: () async {
                        if (kDebugMode) {
                          print("ip = ${_ipTextController.text}");
                          print("port = ${_portTextController.text}");
                        }
                        pMgr.connectToServerTCP(_ipTextController.text, _portTextController.text);
                      },
                    ),
                  ),
                  SizedBox(
                    height: 48.0,
                    child: TextButton(
                      child: const Text("stop"),
                      onPressed: () async {
                        pMgr.closeSocket();
                      },
                    ),
                  ),
                  SizedBox(
                    height: 48.0,
                    child: TextButton(
                      child: const Text("upload"),
                      onPressed: () async {
                        pMgr.upload();
                      },
                    ),
                  ),
                ],
              ),
            ],
          )
        ),
      ),
    );

}


