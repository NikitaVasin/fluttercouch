import Flutter
import UIKit
    
public class SwiftFluttercouchPlugin: NSObject, FlutterPlugin {
    let mCbManager = CBManager.instance
    
  public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(name: "it.oltrenuovefrontiere.fluttercouch", binaryMessenger: registrar.messenger())
    let instance = SwiftFluttercouchPlugin()
    registrar.addMethodCallDelegate(instance, channel: channel)
    let eventChannel = FlutterEventChannel(name: "it.oltrenuovefrontiere.fluttercouch/replicationEventChannel", binaryMessenger: registrar.messenger())
    eventChannel.setStreamHandler(ReplicatorEventListener() as? FlutterStreamHandler & NSObjectProtocol)
    let docEventChannel = FlutterEventChannel(name: "it.oltrenuovefrontiere.fluttercouch/documentChangeEventListener", binaryMessenger: registrar.messenger())
    docEventChannel.setStreamHandler(DocumentChangeEventListener() as? FlutterStreamHandler & NSObjectProtocol)
  }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    switch (call.method) {
    case "initDatabaseWithName":
        let name : String = call.arguments! as! String
        self.mCbManager.initDatabaseWithName(name: name)
        result(String(name))
    case "saveDocument":
        let document = call.arguments! as! [String:Any]
        do {
            let returnedId = try self.mCbManager.saveDocument(map: document)
            result(returnedId!)
        } catch {
            result(FlutterError.init(code: "errSave", message: "Error saving document", details: ""))
        }
    case "saveDocumentWithId":
        let arguments = call.arguments! as! [String:Any]
        let id = arguments["id"] as! String?
        let map = arguments["map"] as! [String:Any]?
        if (id != nil && map != nil){
            do {
                let returnedId = try self.mCbManager.saveDocumentWithId(id: id!, map: map!)
                result(returnedId!)
            } catch {
                result(FlutterError.init(code: "errSave", message: "Error saving document with id \(id!)", details: ""))
            }
        } else {
            result(FlutterError.init(code: "errArgs", message: "Error saving document: Invalid Arguments", details: ""))
        }
    case "getDocumentWithId":
        let id = call.arguments! as! String
        if let returnMap = self.mCbManager.getDocumentWithId(id: id) {
            result(NSDictionary(dictionary: returnMap))
        }
    case "getDocumentsWithKey":
        if let arguments = call.arguments as? [String:Any],  let key = arguments["key"] as? String, let value = arguments["value"] as? String {
            if let returnMap = self.mCbManager.getDocumentsWith(key: key, value: value) {
                result(returnMap)
            }
        }
    case "getAllDocuments": 
        let returnMap = self.mCbManager.getAllDocuments() 
        result(returnMap)
    case "purgeDocument": 
        let id = call.arguments! as! String
        self.mCbManager.purgeDocument(docId: id) {
            result(true);
        } else {
            result(false);
        }
    case "setReplicatorEndpoint":
        let endpoint = call.arguments! as! String
        self.mCbManager.setReplicatorEndpoint(endpoint: endpoint)
        result(String(endpoint))
    case "setReplicatorType":
        let type = call.arguments! as! String
        result(String(self.mCbManager.setReplicatorType(type: type)))
    case "setReplicatorBasicAuthentication":
        let auth = call.arguments! as! [String:String]
        result(String(self.mCbManager.setReplicatorAuthentication(auth: auth)))
    case "setReplicatorSessionAuthentication":
        let sessionID = call.arguments! as! String
        self.mCbManager.setReplicatorSessionAuthentication(sessionID: sessionID)
        result(String(sessionID))
    case "setReplicatorContinuous":
        let isContinuous = call.arguments! as! Bool
        result(Bool(self.mCbManager.setReplicatorContinuous(isContinuous: isContinuous)))
    case "initReplicator":
        self.mCbManager.initReplicator()
        result(String(""))
    case "startReplicator":
        self.mCbManager.startReplication()
        result(String(""))
    case "stopReplicator":
        self.mCbManager.stopReplication()
        result(String(""))
    default:
        result(FlutterMethodNotImplemented)
    }
  }
}
