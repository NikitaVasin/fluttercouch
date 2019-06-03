package it.oltrenuovefrontiere.fluttercouch;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.ListenerToken;
import com.couchbase.lite.DatabaseChange;
import com.couchbase.lite.DatabaseChangeListener;

import io.flutter.plugin.common.EventChannel;

public class DocumentChangeEventListener implements EventChannel.StreamHandler, DatabaseChangeListener {

    private CBManager mCBmanager;
    private ListenerToken mListenerToken;
    private EventChannel.EventSink mEventSink;

    DocumentChangeEventListener(CBManager _cbManager) {
        this.mCBmanager = _cbManager;
    }
    @Override
    public void onListen(Object o, final EventChannel.EventSink eventSink) {
        mEventSink = eventSink;
        mListenerToken = mCBmanager.getDatabase().addChangeListener(this);
    }

    @Override
    public void onCancel(Object o) {
        mCBmanager.getDatabase().removeChangeListener(mListenerToken);
        mEventSink = null;
    }

    @Override
    public void changed(DatabaseChange change) {
            for (String docId: change.getDocumentIDs()) {
                mEventSink.success(docId);
            }
    }
}
