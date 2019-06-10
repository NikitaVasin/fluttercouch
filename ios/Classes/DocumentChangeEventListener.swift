//
//  DocumentChangeEventListener.swift
//  fluttercouch
//
//  Created by IOS TECH on 03/06/2019.
//

import Foundation
import CouchbaseLiteSwift

class DocumentChangeEventListener: FlutterStreamHandler {
    
    var mCBManager:CBManager;
    var mListenerToken: ListenerToken?
    
    init(manager:CBManager) {
        self.mCBManager = manager;
    }


    func onListen(withArguments arguments: Any?, eventSink events: @escaping FlutterEventSink) -> FlutterError? {
        if let database = mCBManager.getDatabase() {
            mListenerToken = database.addChangeListener({(change) in
                for docId in change.documentIDs {
                    events(docId);
                }
            })
        }
        return nil
    }
    
    func onCancel(withArguments arguments: Any?) -> FlutterError? {
        if let database = mCBManager.getDatabase(), let token = mListenerToken {
            database.removeChangeListener(withToken: token)
        }
        return nil
    }
}
