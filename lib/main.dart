import 'package:android_wifi_direct/portal_manager.dart';
import 'package:flutter/material.dart';
import 'package:flutter_easyloading/flutter_easyloading.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:provider/provider.dart';

import 'wifi_page.dart';


import 'dart:io';

final portal = PortalManager()..init();

Future<void> _checkPermissions() async {
  if (Platform.isAndroid) {
    Map<Permission, PermissionStatus> statuses = await [
      Permission.location,
      Permission.locationAlways,
    ].request();
    statuses.forEach((key, value) async {
      if (value.isDenied) {
        if (await key.request().isGranted) {
          debugPrint("$key is granted");
        }
      }
    });
  }
}

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await _checkPermissions();

  runApp(
      MultiProvider(providers: [
        Provider.value(value: portal),
        StreamProvider<WifiList>(
          create: (_) => portal.wifiList,
          initialData: const WifiList(wifiList: []),
        ),
      ], child: const MyApp())
  );
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});


  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: 'Flutter Demo',
      builder: EasyLoading.init(),
      theme: ThemeData(
        primarySwatch: Colors.blue,
      ),
      home: const WifiPage(),
    );
  }
}


