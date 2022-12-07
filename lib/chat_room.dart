import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'portal_manager.dart';

class ChatPage extends StatefulWidget {
  const ChatPage({super.key});

  @override
  State createState() => ChatPageState();
}

class ChatPageState extends State<ChatPage> {

  void _submitText(String text) {
    final pMgr = PortalManager();
    _chatController.clear();
    // pMgr.messageBox.insert(
    //     0,
    //     Container(
    //       alignment: Alignment.centerRight,
    //       child: Text(
    //         text,
    //         style: const TextStyle(fontSize: 50),
    //       ),
    //     ));
    pMgr.sendMessage(text);
  }

  final TextEditingController _chatController = TextEditingController();
  // final List<Widget> _message = [];
  // final pMgr = PortalManager();

  @override
  Widget build(BuildContext context) {
    return Scaffold(
        // resizeToAvoidBottomInset: false,
        appBar: AppBar(
          title: const Text('Chat'),
          leading: BackButton(
            onPressed:(){
              final pMgr = PortalManager();
              Navigator.of(context).pop();
              pMgr.closeSocket();
            },
          ),
        ),
        body: Column(
          children: <Widget>[
            Expanded(
              child: Consumer<MessageList>(
                builder: (_, mL, __) =>
                    ListView.builder(
                      reverse: true,
                      padding: const EdgeInsets.all(8.0),
                      itemBuilder: (context, index) => mL.box[index],
                      itemCount: mL.box.length,
                ),
              ),
            ),
            Container(
                margin: const EdgeInsets.fromLTRB(8,0,0,16),
                child: Row(
                  children: <Widget>[
                    Flexible(
                      child: TextField(
                        decoration: const InputDecoration(
                            contentPadding: EdgeInsets.all(16.0),
                            border: OutlineInputBorder(),
                            hintText: 'Type something...'),
                        controller: _chatController,
                        onSubmitted: _submitText,
                      ),
                    ),
                    IconButton(
                      icon: const Icon(Icons.send),
                      onPressed: () => _submitText(_chatController.text),
                    ),
                  ],
                ))
          ],
        ));
  }
}
