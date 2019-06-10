//
//  ReplicatorEventListener.swift
//  fluttercouch
//
//  Created by Luca Christille on 25/10/2018.
//

import Foundation
import CouchbaseLiteSwift

class ReplicatorEventListener: FlutterStreamHandler {
    
    var mCBManager:CBManager;
    var mListenerToken: ListenerToken?
    
    init(manager:CBManager) {
        self.mCBManager = manager;
    }
    
    func onListen(withArguments arguments: Any?, eventSink events: @escaping FlutterEventSink) -> FlutterError? {
        mListenerToken = mCBManager.getReplicator().addChangeListener { (change) in
            switch (change.status.activity) {
            case .busy:
                events("BUSY")
            case .idle:
                events("IDLE")
            case .offline:
                events("OFFLINE")
            case .stopped:
                events("STOPPED")
            case .connecting:
                events("CONNECTING")
            }
        }
        return nil
    }
    
    func onCancel(withArguments arguments: Any?) -> FlutterError? {
        mCBManager.getReplicator().removeChangeListener(withToken: mListenerToken!)
        return nil
    }
}
