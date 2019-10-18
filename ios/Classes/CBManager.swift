//
//  CBManager.swift
//  Runner
//
//  Created by Luca Christille on 14/08/18.
//  Copyright © 2018 The Chromium Authors. All rights reserved.
//

import Foundation
import CouchbaseLiteSwift

class CBManager {
    // static let instance = CBManager();
    private var mDatabase : Dictionary<String, Database> = Dictionary();
    private var mReplConfig : ReplicatorConfiguration! = nil;
    private var mReplicator : Replicator! = nil;
    private var defaultDatabase = "defaultDatabase";
    
    init() {}
    
    func getDatabase() -> Database? {
        if let result = mDatabase[defaultDatabase] {
            return result;
        } else {
            return nil;
        }
    }
    
    func getDatabase(name : String) -> Database? {
        if let result = mDatabase[name] {
            return result;
        } else {
            return nil;
        }
    }
    
    func saveDocument(map: Dictionary<String, Any>) throws -> String? {
        let mutableDocument: MutableDocument = MutableDocument(data: map);
        try mDatabase[defaultDatabase]?.saveDocument(mutableDocument)
        return mutableDocument.id;
    }
    
    func saveDocumentWithId(id : String, map: Dictionary<String, Any>) throws -> String? {
        if let defaultDb: Database = getDatabase() {
            if let doc: Document = defaultDb.document(withID: id) {
                let mutableMap = (map as NSDictionary).mutableCopy() as! NSMutableDictionary;
                for key in doc.keys{
                    if(key == "_attachments"){
                        mutableMap[key] = doc.value(forKey: key)
                    }
                    if let blob = doc.blob(forKey: key){
                        mutableMap[key] = blob
                    }
                }
                let resultMap = removeAttachmentsPath(object: mutableMap as NSDictionary)
                let mutableDocument: MutableDocument = MutableDocument(id: id, data: mutableMap as? Dictionary<String, Any>)
                try defaultDb.saveDocument(mutableDocument)
                return mutableDocument.id
            }
        }
        return id
    }
    
    func removeAttachmentsPath(object: AnyObject) -> AnyObject {
        if let dict = object as? NSDictionary{
            let d:NSMutableDictionary = dict.mutableCopy() as! NSMutableDictionary
            if(d.value(forKey: "contentType") != nil){
                d["path"] = nil
            }
            for (key, value) in d {
                let val =  removeAttachmentsPath(object:value as AnyObject)
                d[key] = val;
            }
        }else if let array = object as? NSArray{
            let a =  array.map{(value)->AnyObject in
                return removeAttachmentsPath(object:value as AnyObject)
                } as AnyObject
            return a
        }
        return object;
        
    }
    
    func addAttachmentsPath(attarchments:NSDictionary, object:AnyObject,  key:String?) -> AnyObject {
        if let dict = object as? NSDictionary {
            let d:NSMutableDictionary = dict.mutableCopy() as! NSMutableDictionary
            if let attachKey = d["file"] as! String?{
                if let attachPath = attarchments[attachKey] {
                    d["path"] = attachPath
                }
            }
           
            for (key, value) in dict {
                let val = addAttachmentsPath(attarchments: attarchments, object: value as AnyObject, key: key as? String)
                d[key] = val
            }
            return d as AnyObject
        }
        else if let array = object as? NSArray {
            let a =  array.map{(value)->AnyObject in
                return addAttachmentsPath(attarchments: attarchments, object: value as AnyObject, key: nil)
                } as AnyObject
            return a
        }
        return object;
    }
    
    
    
    func processDoc (document: Document) -> NSDictionary? {
        let dictionary: NSDictionary = document.toDictionary() as NSDictionary
        var retrievedDocument: NSMutableDictionary = dictionary.mutableCopy() as! NSMutableDictionary
        let attachmentsPath = NSMutableDictionary()
        for (key, _) in retrievedDocument {
            if let blob = document.blob(forKey: (key as! String)){
                 attachmentsPath[key] = blob.filePath
            }
        }
        for (key, _) in attachmentsPath {
            retrievedDocument.removeObject(forKey: key as! String)
        }
        if let attcahments = retrievedDocument.value(forKey: "_attachments") as? NSDictionary {
            
            for (key, value) in attcahments {
                if let blob = value as? Blob {
                    attachmentsPath[key] = blob.filePath
                }
            }

            retrievedDocument.removeObject(forKey: "_attachments")

        }

        if let d = addAttachmentsPath(attarchments: attachmentsPath, object: retrievedDocument, key: nil) as? NSDictionary {
            retrievedDocument = d.mutableCopy() as! NSMutableDictionary
        }
        
        return retrievedDocument
    }
    
    func getDocumentWithId(id : String) -> NSDictionary? {
        let resultMap: NSMutableDictionary = NSMutableDictionary()
        resultMap["id"] = id
        resultMap["doc"] = NSDictionary()
        if let defaultDb: Database = getDatabase() {
            if let document: Document = defaultDb.document(withID: id) {
                if let d = processDoc(document: document) {
                    resultMap["id"] = document.id
                    resultMap["doc"] = d
                }
            } else {
                resultMap["id"] = id
                resultMap["doc"] = NSDictionary.init()
            }
        }
        return NSDictionary.init(dictionary: resultMap)
    }

    func getDocumentsWith(key : String, value: String) -> NSDictionary? {
        let resultMap: NSMutableDictionary = NSMutableDictionary();
        if let defaultDb: Database = getDatabase() {
            let query = QueryBuilder
                .select(SelectResult.expression(Meta.id))
                .from(DataSource.database(defaultDb))
                .where(Expression.property(key).equalTo(Expression.string(value)))
            do {
                let result = try query.execute()
                let docs = result.allResults().map{(result)->NSDictionary in
                    let ret = NSMutableDictionary();
                    if let id = result.string(forKey: "id") {
                        if let doc:Document = defaultDb.document(withID: id) {
                        ret["doc"] = processDoc(document: doc)
                        ret["id"] = id
                        }
                    } 
                    return ret;
                    };
                resultMap["docs"] = docs;
            } catch {
                resultMap["docs"] = nil;
            }
        }
        return resultMap;
    }


    func getAllDocuments() -> NSDictionary {
        let resultMap: NSMutableDictionary = NSMutableDictionary();
        if let defaultDb: Database = getDatabase() {
            let query = QueryBuilder
                .select(SelectResult.expression(Meta.id))
                .from(DataSource.database(defaultDb))
            do {
                let result = try query.execute()
                let docs = result.allResults().map{(result)->NSDictionary in
                    let ret = NSMutableDictionary();
                    if let id = result.string(forKey: "id") {
                        if let doc:Document = defaultDb.document(withID: id) {
                            ret["doc"] = processDoc(document: doc)
                            ret["id"] = id
                        }
                    }
                   
                    return ret;
                    };
                resultMap["docs"] = docs;
            } catch {
                resultMap["docs"] = nil;
            }
        }
        return resultMap;
    }
    
    func addAttachment(docId:String, contentType:String, filePath:String) throws -> String? {
        if let defaultDb: Database = getDatabase() {
            if let doc: MutableDocument = defaultDb.document(withID: docId)?.toMutable() {
                let key = String((0..<5).map{ _ in "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".randomElement()! })//String(Int.random(in: 0 ..< 99999)) //TODO random String
                let blob = try Blob(contentType: contentType, fileURL: URL.init(fileURLWithPath: filePath))
                doc.setBlob(blob, forKey: key)
                try defaultDb.saveDocument(doc)
                return key;
            }
        }
        return nil;
    }
    
    func removeAttachment(docId:String, key:String) throws {
        if let defaultDb: Database = getDatabase() {
            if let doc: MutableDocument = defaultDb.document(withID: docId)?.toMutable() {
                doc.setBlob(nil, forKey: key)
                try defaultDb.saveDocument(doc)
            }
        }
    }
    
    func purgeDocument(docId: String) -> Bool {
        if let defaultDb: Database = getDatabase() {
            if let doc: Document = defaultDb.document(withID: docId) {
                try! defaultDb.purgeDocument(doc);
                return true;
            }
        }
        return false;
    }

    func deleteDocument(docId: String) -> Bool {
        if let defaultDb: Database = getDatabase() {
            if let doc: Document = defaultDb.document(withID: docId) {
                try! defaultDb.deleteDocument(doc);
                return true;
            }
        }
        return false;
    }

    func initDatabaseWithName(name: String){
        if mDatabase.keys.contains(name) {
            defaultDatabase = name
        } else {
            do {
                let newDatabase = try Database(name: name)
                // Database.setLogLevel(level: LogLevel.verbose, domain: LogDomain.replicator)
                mDatabase[name] = newDatabase
                defaultDatabase = name
            } catch {
                print("Error initializing new database")
            }
        }
    }

     func delete(){
        if let defaultDb: Database = getDatabase() {
             try! defaultDb.delete();
             mDatabase.removeAll();
        }
     }
    
    func setReplicatorEndpoint(endpoint: String) {
        let targetEndpoint = URLEndpoint(url: URL(string: endpoint)!)
        mReplConfig = ReplicatorConfiguration(database: getDatabase()!, target: targetEndpoint)
    }
    
    func setReplicatorType(type: String) -> String {
        var settedType: ReplicatorType = ReplicatorType.pull
        if (type == "PUSH") {
            settedType = .push
        } else if (type == "PULL") {
            settedType = .pull
        } else if (type == "PUSH_AND_PULL") {
            settedType = .pushAndPull
        }
        mReplConfig?.replicatorType = settedType
        switch(mReplConfig?.replicatorType.rawValue) {
        case (0):
            return "PUSH_AND_PULL"
        case (1):
            return "PUSH"
        case(2):
            return "PULL"
        default:
            return ""
        }
    }
    
    func setReplicatorAuthentication(auth: [String:String]) -> String {
        if let username = auth["username"], let password = auth["password"] {
            mReplConfig?.authenticator = BasicAuthenticator(username: username, password: password)
        }
        return mReplConfig.authenticator.debugDescription
    }
    
    func setReplicatorSessionAuthentication(sessionID: String?) {
        if ((sessionID) != nil) {
            if ((mReplConfig) != nil) {
            mReplConfig.authenticator = SessionAuthenticator(sessionID: sessionID!)
            }
        }
    }
    
    func setReplicatorContinuous(isContinuous: Bool) -> Bool {
        if ((mReplConfig) != nil) {
            mReplConfig?.continuous = isContinuous
            return mReplConfig!.continuous
        }
        return false
    }
    
    func initReplicator() {
        mReplicator = Replicator(config: mReplConfig)
    }
    
    func startReplication() {
        if ((mReplicator) != nil) {
            mReplicator.start()
        }
    }
    
    func stopReplication() {
        if ((mReplicator) != nil) {
            mReplicator.stop()
            mReplicator = nil
        }
    }
    
    func getReplicator() -> Replicator {
        return mReplicator
    }
}
