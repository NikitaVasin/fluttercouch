import 'dart:async';

import 'package:flutter/services.dart';
import 'package:fluttercouch/document.dart';
import 'package:meta/meta.dart';

abstract class Fluttercouch {
  String dbName;
  MethodChannel _methodChannel =
  const MethodChannel('it.oltrenuovefrontiere.fluttercouch');

  EventChannel _replicationEventChannel;

  EventChannel _documentChangeEventListener;

  Future<String> initDatabaseWithName(String _name) async {
    try {
      this.dbName = _name;
      final String result = await _methodChannel.invokeMethod('initDatabaseWithName', _name);
      this._replicationEventChannel = EventChannel(
          "it.oltrenuovefrontiere.fluttercouch/replicationEventChannel/" + _name);
      this._documentChangeEventListener = EventChannel(
          "it.oltrenuovefrontiere.fluttercouch/documentChangeEventListener/" + _name);
      return result;
    } on PlatformException catch (e) {
      throw 'unable to init database $_name: ${e.message}';
    }
  }

  Future<bool> prebuildDatabase(String assetPath, String _name) async {
    this.dbName = _name;
    try {
      return await _methodChannel.invokeMethod(
          'prebuildDatabase', {"db": _name, "assetPath": assetPath});
    } on PlatformException catch (e) {
      throw 'unable to prebuild Database $_name: ${e.message}';
    }
  }

  Future<String> saveDocument(Document _doc) async {
    _assertInitialized();
    try {
      final String result =
      await _methodChannel.invokeMethod(
          'saveDocument', {"db": this.dbName, "document": _doc.toMap()});
      return result;
    } on PlatformException {
      throw 'unable to save the document';
    }
  }

  Future<String> saveDocumentWithId(String _id, Document _doc) async {
    _assertInitialized();
    try {
      final String result = await _methodChannel.invokeMethod(
          'saveDocumentWithId',
          <String, dynamic>{"db": this.dbName, 'id': _id, 'map': _doc.toMap()});
      return result;
    } on PlatformException {
      throw 'unable to save the document with set id $_id';
    }
  }

  Future<Document> getDocumentWithId(String _id) async {
    Map<dynamic, dynamic> _docResult;
    _docResult = await _getDocumentWithId(_id);
    return Document(_docResult["doc"], _docResult["id"]);
  }

  Future<List<Document>> getDocumentsWith({@required String key, @required String value}) async {
    _assertInitialized();
    try {
      final Map<dynamic, dynamic> result = await _methodChannel
          .invokeMethod('getDocumentsWithKey', {"db": this.dbName, "key": key, "value": value});
      List<Document> documents = result["docs"] != null
          ? result["docs"]
          .map<Document>((v) => Document(v['doc'], v['id']))
          .toList()
          : null;
      return documents;
    } on PlatformException {
      throw 'unable to get the document with key $key, value $value';
    }
  }

  Future<List<Document>> getAllDocuments() async {
    _assertInitialized();
    try {
      final Map<dynamic, dynamic> result = await _methodChannel
          .invokeMethod('getAllDocuments', {"db": this.dbName});
      List<Document> documents = result["docs"] != null
          ? result["docs"]
          .map<Document>((v) => Document(v['doc'], v['id']))
          .toList()
          : null;
      return documents;
    } on PlatformException {
      throw 'unable to get all documents';
    }
  }


  Future<String> addAttachment(String documentId, String contentType, String filePath) async {
    _assertInitialized();
    try {
      final String result = await _methodChannel.invokeMethod(
          'addAttachment',
          {"db": this.dbName,
            "id": documentId,
            "contentType": contentType,
            "filePath": filePath
          });
      return result;
    } on PlatformException {
      throw 'unable to add attachment $filePath';
    }
  }
  
  Future<bool> removeAttachment(String documentId, String key) async {
    _assertInitialized();
    try{
      return await _methodChannel.invokeMethod('removeAttachment', {"db": this.dbName,
        "id": documentId,
        "key": key,
      });
    } on PlatformException {
      throw 'unable to remove attachment $key';
    }
  }

  Future close() async {
    try {
      return await _methodChannel.invokeMethod(
          'closeDatabase', {"db": dbName,});
    } on PlatformException catch (e) {
      throw 'unable to delete Database $dbName: ${e.message}';
    }
  }

  Future delete() async {
    try {
      return await _methodChannel.invokeMethod(
          'deleteDatabase', {"db": dbName,});
    } on PlatformException catch (e) {
      throw 'unable to delete Database $dbName: ${e.message}';
    }
  }

  Future purgeDocumentById(String docId) async {
    _assertInitialized();
    try {
      final bool result = await _methodChannel.invokeMethod(
          'purgeDocument', {"db": this.dbName, "id": docId});
      return result;
    } on PlatformException {
      throw 'unable purge document';
    }
  }

  Future<bool> deleteDocumentById(String docId) async {
    _assertInitialized();
    try {
      final bool result = await _methodChannel.invokeMethod(
          'deleteDocument', {"db": this.dbName, "id": docId});
      return result;
    } on PlatformException {
      throw 'unable delete document';
    }
  }

  Future<String> setReplicatorEndpoint(String _endpoint) async {
    _assertInitialized();
    try {
      final String result =
      await _methodChannel.invokeMethod(
          'setReplicatorEndpoint', {"db": this.dbName, "endpoint": _endpoint});
      return result;
    } on PlatformException {
      throw 'unable to set target endpoint to $_endpoint';
    }
  }

  Future<String> setReplicatorType(String _type) async {
    _assertInitialized();
    try {
      final String result =
      await _methodChannel.invokeMethod('setReplicatorType', {"db": this.dbName, "type": _type});
      return result;
    } on PlatformException {
      throw 'unable to set replicator type to $_type';
    }
  }

  Future<bool> setReplicatorContinuous(bool _continuous) async {
    _assertInitialized();
    try {
      final bool result = await _methodChannel.invokeMethod(
          'setReplicatorContinuous', {"db": this.dbName, "continuous": _continuous});
      return result;
    } on PlatformException {
      throw 'unable to set replicator continuous setting to $_continuous';
    }
  }

  Future<String> setReplicatorBasicAuthentication(Map<String, String> _auth) async {
    _assertInitialized();
    try {
      final String result = await _methodChannel.invokeMethod(
          'setReplicatorBasicAuthentication', {"db": this.dbName, "auth": _auth});
      return result;
    } on PlatformException {
      throw 'unable to set replicator basic authentication';
    }
  }

  Future<String> setReplicatorSessionAuthentication(String _sessionID) async {
    _assertInitialized();
    try {
      final String result = await _methodChannel.invokeMethod(
          'setReplicatorSessionAuthentication', {"db": this.dbName, "sessiodID": _sessionID});
      return result;
    } on PlatformException {
      throw 'unable to set replicator basic authentication';
    }
  }

  Future<Null> initReplicator() async {
    _assertInitialized();
    try {
      await _methodChannel.invokeMethod("initReplicator", {"db": this.dbName});
    } on PlatformException {
      throw 'unable to init replicator';
    }
  }

  Future<Null> startReplicator() async {
    _assertInitialized();
    try {
      await _methodChannel.invokeMethod('startReplicator', {"db": this.dbName});
    } on PlatformException {
      throw 'unable to start replication';
    }
  }

  Future<Null> stopReplicator() async {
    _assertInitialized();
    try {
      await _methodChannel.invokeMethod('stopReplicator', {"db": this.dbName});
    } on PlatformException {
      throw 'unable to stop replication';
    }
  }

  Future<Map<dynamic, dynamic>> _getDocumentWithId(String _id) async {
    _assertInitialized();
    try {
      final Map<dynamic, dynamic> result =
      await _methodChannel.invokeMethod('getDocumentWithId', {"db": this.dbName, "id": _id});
      return result;
    } on PlatformException {
      throw 'unable to get the document with id $_id';
    }
  }

  void listenReplicationEvents(Function(dynamic) function) {
    _replicationEventChannel.receiveBroadcastStream().listen(function);
  }

  void listenDocumentChangeEvents(Function(dynamic) function) {
    _documentChangeEventListener.receiveBroadcastStream().listen(function);
  }

  void _assertInitialized() {
    if (this.dbName == null) throw Exception("Database havn't initialized yet");
  }
}
