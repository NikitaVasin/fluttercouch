import Flutter
import UIKit
import CouchbaseLiteSwift
import SSZipArchive
    
public class SwiftFluttercouchPlugin: NSObject, FlutterPlugin {
    // let mCbManager = CBManager()
    var mCbManagers:[String:CBManager] = [:]
    var registar:FlutterPluginRegistrar?
    
  public static func register(with registrar: FlutterPluginRegistrar) {
    
    let channel = FlutterMethodChannel(name: "it.oltrenuovefrontiere.fluttercouch", binaryMessenger: registrar.messenger())
    let instance = SwiftFluttercouchPlugin()
    instance.registar = registrar;
    registrar.addMethodCallDelegate(instance, channel: channel)
    // let eventChannel = FlutterEventChannel(name: "it.oltrenuovefrontiere.fluttercouch/replicationEventChannel", binaryMessenger: registrar.messenger())
    // eventChannel.setStreamHandler(ReplicatorEventListener() as? FlutterStreamHandler & NSObjectProtocol)
    // let docEventChannel = FlutterEventChannel(name: "it.oltrenuovefrontiere.fluttercouch/documentChangeEventListener", binaryMessenger: registrar.messenger())
    // docEventChannel.setStreamHandler(DocumentChangeEventListener() as? FlutterStreamHandler & NSObjectProtocol)
  }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    switch (call.method) {
    case "initDatabaseWithName":
        let name : String = call.arguments! as! String
        var mCbManager = self.mCbManagers[name]
        if (mCbManager == nil) {
            mCbManager = CBManager()
            self.mCbManagers[name] = mCbManager;
        }
        if let registar = self.registar {
            let eventChannel = FlutterEventChannel(name: "it.oltrenuovefrontiere.fluttercouch/replicationEventChannel/"+name, binaryMessenger: registar.messenger())
            eventChannel.setStreamHandler(ReplicatorEventListener(manager: mCbManager!) as? FlutterStreamHandler & NSObjectProtocol)
            let docEventChannel = FlutterEventChannel(name: "it.oltrenuovefrontiere.fluttercouch/documentChangeEventListener/"+name, binaryMessenger: registar.messenger())
            docEventChannel.setStreamHandler(DocumentChangeEventListener(manager: mCbManager!) as? FlutterStreamHandler & NSObjectProtocol)
        }
        mCbManager!.initDatabaseWithName(name: name)
        result(String(name))
    case "prebuildDatabase":
        let arguments = call.arguments! as! [String:Any]
        let dbName = arguments["db"] as! String
        let assetPath = arguments["assetPath"] as! String
        if let resourceName = assetPath.components(separatedBy: "/").last?.replacingOccurrences(of: ".zip", with: ""){
            if let zipPath = Bundle.main.path(forResource: resourceName, ofType: "zip") {
                let unzipPath = DatabaseConfiguration().directory
                if !Database.exists(withName: dbName) {
                    SSZipArchive.unzipFile(atPath:zipPath,toDestination:unzipPath)
                    result(true)
                }else{
                    result(false)
                }
            } else {
                result(false)
            }
        }
    case "saveDocument":
        let arguments = call.arguments! as! [String:Any]
        let dbName = arguments["db"] as! String
        if let mCbManager = self.mCbManagers[dbName] {
            let document = arguments["document"] as! [String:Any]
            do {
                let returnedId = try mCbManager.saveDocument(map: document)
                result(returnedId!)
            } catch {
                result(FlutterError.init(code: "errSave", message: "Error saving document", details: ""))
            }    
        }
    case "saveDocumentWithId":
        let arguments = call.arguments! as! [String:Any]
        let dbName = arguments["db"] as! String
        let id = arguments["id"] as! String?
        let map = arguments["map"] as! [String:Any]?
        if let mCbManager = self.mCbManagers[dbName] {
            if (id != nil && map != nil){
                do {
                    let returnedId = try mCbManager.saveDocumentWithId(id: id!, map: map!)
                    result(returnedId!)
                } catch {
                    result(FlutterError.init(code: "errSave", message: "Error saving document with id \(id!)", details: ""))
                }
            } else {
                result(FlutterError.init(code: "errArgs", message: "Error saving document: Invalid Arguments", details: ""))
            }
        }
    case "getDocumentWithId":
        let arguments = call.arguments! as! [String:Any]
        let dbName = arguments["db"] as! String
        let id = arguments["id"] as! String
        if let mCbManager = self.mCbManagers[dbName] {
            if let returnMap = mCbManager.getDocumentWithId(id: id) {
                result(NSDictionary(dictionary: returnMap))
            }
        }
        
    case "getDocumentsWithKey":
        if let arguments = call.arguments as? [String:Any], let dbName = arguments["db"] as? String, let key = arguments["key"] as? String, let value = arguments["value"] as? String {
            if let mCbManager = self.mCbManagers[dbName] {
                if let returnMap = mCbManager.getDocumentsWith(key: key, value: value) {
                    result(returnMap)
                }
            }
        }
    case "getAllDocuments": 
        let arguments = call.arguments! as! [String:Any]
        let dbName = arguments["db"] as! String
        if let mCbManager = self.mCbManagers[dbName] {
            let returnMap = mCbManager.getAllDocuments() 
            result(returnMap)
        }
    case "addAttachment":
        let arguments = call.arguments! as! [String:Any]
        let dbName = arguments["db"] as! String
        let id = arguments["id"] as! String
        let contentType = arguments["contentType"] as! String
        var filePath = arguments["filePath"] as! String
        if filePath.contains("asset://") {
            let key = registar?.lookupKey(forAsset: filePath.replacingOccurrences(of: "asset://", with: ""))
            filePath = Bundle.main.path(forResource: key, ofType: nil) ?? "";
        }
        
        if let mCbManager = self.mCbManagers[dbName] {
            do{
                let key = try mCbManager.addAttachment(docId: id, contentType: contentType, filePath: filePath)
                result(key)
            }catch{
                result(FlutterError.init(code: "errAdd", message: "error add attachment", details: ""))
            }
        }
    case "removeAttachment":
        let arguments = call.arguments! as! [String:Any]
        let dbName = arguments["db"] as! String
        let id = arguments["id"] as! String
        let key = arguments["key"] as! String
        if let mCbManager = self.mCbManagers[dbName] {
            do{
                try mCbManager.removeAttachment(docId: id, key: key);
                result(true)
            }catch{
                result(FlutterError.init(code: "errRemove", message: "error remove attachment", details: ""))
            }
        }
    case "purgeDocument": 
        let arguments = call.arguments! as! [String:Any]
        let dbName = arguments["db"] as! String
        let id = arguments["id"] as! String
        if let mCbManager = self.mCbManagers[dbName] {
            if mCbManager.purgeDocument(docId: id) {
                result(true);
            } else {
                result(false);
            }
        }
    case "deleteDocument":
        let arguments = call.arguments! as! [String:Any]
        let dbName = arguments["db"] as! String
        let id = arguments["id"] as! String
        if let mCbManager = self.mCbManagers[dbName] {
            if mCbManager.deleteDocument(docId: id) {
                result(true);
            } else {
                result(false);
            }
        }
    case "setReplicatorEndpoint":
        let arguments = call.arguments! as! [String:Any]
        let dbName = arguments["db"] as! String
        let endpoint = arguments["endpoint"] as! String
        if let mCbManager = self.mCbManagers[dbName] {
            mCbManager.setReplicatorEndpoint(endpoint: endpoint)
            result(String(endpoint))
        }
    case "setReplicatorType":
        let arguments = call.arguments! as! [String:Any]
        let dbName = arguments["db"] as! String
        let type = arguments["type"] as! String
        if let mCbManager = self.mCbManagers[dbName] {
            result(String(mCbManager.setReplicatorType(type: type)))
        }
    case "setReplicatorBasicAuthentication":
        let arguments = call.arguments! as! [String:Any]
        let dbName = arguments["db"] as! String
        let auth = arguments["auth"] as! [String:String]
        if let mCbManager = self.mCbManagers[dbName] {
            result(String(mCbManager.setReplicatorAuthentication(auth: auth)))
        }
    case "setReplicatorSessionAuthentication":
        let arguments = call.arguments! as! [String:Any]
        let dbName = arguments["db"] as! String
        let sessionID = arguments["sessionID"] as! String
        if let mCbManager = self.mCbManagers[dbName] {
            mCbManager.setReplicatorSessionAuthentication(sessionID: sessionID)
            result(String(sessionID))
        }
    case "setReplicatorContinuous":
        let arguments = call.arguments! as! [String:Any]
        let dbName = arguments["db"] as! String
        let isContinuous = arguments["continuous"] as! Bool
        if let mCbManager = self.mCbManagers[dbName] {
            result(Bool(mCbManager.setReplicatorContinuous(isContinuous: isContinuous)))
        }
    case "initReplicator":
        let arguments = call.arguments! as! [String:Any]
        let dbName = arguments["db"] as! String
        if let mCbManager = self.mCbManagers[dbName] {
            mCbManager.initReplicator()
            result(String(""))
        }
    case "startReplicator":
        let arguments = call.arguments! as! [String:Any]
        let dbName = arguments["db"] as! String
        if let mCbManager = self.mCbManagers[dbName] {
            mCbManager.startReplication()
            result(String(""))
        }
    case "stopReplicator":
        let arguments = call.arguments! as! [String:Any]
        let dbName = arguments["db"] as! String
        if let mCbManager = self.mCbManagers[dbName] {
            mCbManager.stopReplication()
            result(String(""))
        }
    default:
        result(FlutterMethodNotImplemented)
    }
  }
}
