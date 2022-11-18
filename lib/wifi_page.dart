
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_easyloading/flutter_easyloading.dart';
import 'package:provider/provider.dart';

import 'portal_manager.dart';

class WifiPage extends StatefulWidget {
  const WifiPage({Key? key}) : super(key: key);
  @override
  State<WifiPage> createState() => _WifiPageState();
}

class _WifiPageState extends State<WifiPage> {
  @override
  Widget build(BuildContext context) => Scaffold(
    extendBodyBehindAppBar: true,
    appBar: AppBar(

      automaticallyImplyLeading: true,
      elevation: 0,
      centerTitle: true,
      title: const Text("Wifi P2P device list"),
    ),
    body: Consumer2<PortalManager,WifiList>(
      builder: (_, pMgr,wList, __) => SizedBox(
        height: MediaQuery.of(context).size.height,
        child: Column(
          mainAxisAlignment: MainAxisAlignment.spaceEvenly,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
              children: [
                Padding(
                  padding: const EdgeInsets.fromLTRB(0, 120, 0, 20),
                  child: TextButton(onPressed:(){
                    EasyLoading.show(status: 'Searching...');
                    pMgr.discoverPeers();
                  }, child: const Text('Scan')),
                ),
                Padding(
                  padding: const EdgeInsets.fromLTRB(0, 120, 0, 20),
                  child: TextButton(onPressed:(){
                    pMgr.sendMessage();
                  }, child: const Text('Send')),
                ),
                Padding(
                  padding: const EdgeInsets.fromLTRB(0, 120, 0, 20),
                  child: TextButton(onPressed:(){
                    pMgr.loadWav();
                  }, child: const Text('Load')),
                ),
              ],
            ),
            SizedBox(
              height: MediaQuery.of(context).size.height*0.65,
              child: ListView.builder(
                  itemCount: wList.wifiList.length,
                  shrinkWrap: true,
                  padding: const EdgeInsets.all(20.0),
                  itemBuilder: (BuildContext context, int index) {
                    return ListTile(title: Text(wList.wifiList[index]),
                      onTap: (){
                        if (kDebugMode) {
                          _showReturnMessageDialog("Device name:",wList.wifiList,index);
                          // pMgr.connectToDevice(index);
                          // print("index =　$index");
                        }},
                    );
                  }),
            ),
          ],
        ),
      ),
    ),
  );


  Future<void> _showReturnMessageDialog(String title,List wifiList,int index) async {
    return showDialog<void>(
      context: context,
      barrierDismissible: false, // user must tap button!
      builder: (BuildContext context) {
        return AlertDialog(
          title: Text(title),
          content: SingleChildScrollView(
            child: Text(wifiList[index]),
          ),
          actions: <Widget>[
            TextButton(
              child: const Text('Cancel'),
              onPressed: () {
                setState(() {
                  Navigator.pop(context);
                });
              },
            ),
            TextButton(
              child: const Text('Connect'),
              onPressed: () {
                final pMgr = PortalManager();
                pMgr.connectToDevice(index);
                Navigator.pop(context);
              },
            ),
          ],
        );
      },
    );
  }
}
