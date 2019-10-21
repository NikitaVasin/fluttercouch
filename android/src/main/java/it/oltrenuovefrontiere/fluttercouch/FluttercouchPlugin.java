package it.oltrenuovefrontiere.fluttercouch;

import android.content.Context;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Query;

import org.json.JSONObject;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import android.content.res.AssetManager;
import android.content.res.AssetFileDescriptor;
import java.io.File;

/**
 * FluttercouchPlugin
 */
public class FluttercouchPlugin implements MethodCallHandler {

    HashMap<String, CBManager> managers = new HashMap<>();

    static Context context;

    Registrar registrar;

    /**
     * Plugin registration.
     */
    public static void registerWith(Registrar registrar) {
        context = registrar.context();
        final FluttercouchPlugin flutterCouchPlugin = new FluttercouchPlugin();
        flutterCouchPlugin.registrar = registrar;
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "it.oltrenuovefrontiere.fluttercouch");
        channel.setMethodCallHandler(flutterCouchPlugin);
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        String _name = null;
        if ("initDatabaseWithName".equals(call.method)) {
            _name = call.arguments();
        } else {
            _name = call.hasArgument("db") ? (String) call.argument("db") : null;
        }
        CBManager cbManager = getCBManager(_name);

        switch (call.method) {
            case ("initDatabaseWithName"):
                try {
                    final EventChannel eventChannel = new EventChannel(registrar.messenger(), "it.oltrenuovefrontiere.fluttercouch/replicationEventChannel/" + _name);
                    eventChannel.setStreamHandler(new ReplicationEventListener(cbManager));
                    final EventChannel docEventChannel = new EventChannel(registrar.messenger(), "it.oltrenuovefrontiere.fluttercouch/documentChangeEventListener/" + _name);
                    docEventChannel.setStreamHandler(new DocumentChangeEventListener(cbManager));
                    cbManager.initDatabaseWithName(_name);
                    result.success(_name);
                } catch (Exception e) {
                    e.printStackTrace();
                    result.error("errInit", "error initializing database", e.toString());
                }
                break;
            case ("closeDatabase"):
                try {
                    cbManager.close();
                    result.success(null);
                } catch (Exception e) {
                    e.printStackTrace();
                    result.error("errSave", "error saving the document", e.toString());
                }
                break;
            case ("deleteDatabase"):
                try {
                   cbManager.delete();
                    result.success(null);
                } catch (Exception e) {
                    e.printStackTrace();
                    result.error("errSave", "error delete the document", e.toString());
                }
                break;
            case ("prebuildDatabase"):
                String assetPath = call.argument("assetPath");
                try {
                    Context context = FluttercouchPlugin.context;
                    if (!Database.exists(_name, context.getFilesDir())) {
                        InputStream in = context.getAssets().open("flutter_assets/"+assetPath);
                        ZipUtils.unzip(in, context.getFilesDir());
                        result.success(true);
                    }else{
                        result.success(false);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    result.error("errInit", "error prebuild database", e.toString());
                }
                break;
            case ("saveDocument"):
                Map<String, Object> _document = call.argument("document");
                try {
                    String returnedId = cbManager.saveDocument(_document);
                    result.success(returnedId);
                } catch (CouchbaseLiteException e) {
                    e.printStackTrace();
                    result.error("errSave", "error saving the document", e.toString());
                }
                break;
            case ("saveDocumentWithId"):
                if (call.hasArgument("id") && call.hasArgument("map")) {
                    String _id = call.argument("id");
                    Map<String, Object> _map = call.argument("map");
                    try {
                        String returnedId = cbManager.saveDocumentWithId(_id, _map);
                        result.success(returnedId);
                    } catch (CouchbaseLiteException e) {
                        e.printStackTrace();
                        result.error("errSave", "error saving the document", e.toString());
                    }
                } else {
                    result.error("errArg", "invalid arguments", null);
                }
                break;
            case ("getDocumentWithId"):
                String _id = call.argument("id");
                try {
                    result.success(cbManager.getDocumentWithId(_id));
                } catch (CouchbaseLiteException e) {
                    e.printStackTrace();
                    result.error("errGet", "error getting the document with id: " + _id, e.toString());
                }
                break;

            case ("getDocumentsWithKey"):
                String key = call.argument("key");
                String value = call.argument("value");
                try {
                    result.success(cbManager.getDocumentsWith(key, value));
                } catch (CouchbaseLiteException e) {
                    e.printStackTrace();
                    result.error("errGet", "error getting the document with key: " + key, e.toString());
                }
                break;
            case ("getAllDocuments"):
                try {
                    result.success(cbManager.getAllDocuments());
                } catch (CouchbaseLiteException e) {
                    e.printStackTrace();
                }
                break;
            case ("addAttachment"):
                String documentId = call.argument("id");
                String contentType = call.argument("contentType");
                String filePath = call.argument("filePath");
                FileDescriptor fileDescriptor;
                InputStream inputStream;
                if (filePath.contains("asset://"))  {
                    filePath = filePath.replace("asset://", "");
                    AssetManager assetManager = registrar.context().getAssets();
                    String assetKey = registrar.lookupKeyForAsset(filePath);
                    try {
                        AssetFileDescriptor fd = assetManager.openFd(assetKey);
                        inputStream = fd.createInputStream();
                    } catch (Throwable e) {
                        result.error("errSave", "error add attachment " +filePath+" to document " + documentId, e.toString());
                        return;
                    }
                } else {
                    File file = new File(filePath);
                    try {
                        inputStream = new FileInputStream(file);
                    } catch (FileNotFoundException e) {
                        result.error("errSave", "error add attachment " +filePath+" to document " + documentId, e.toString());
                        return;
                    }
                }
                try{
                    result.success(cbManager.addAttachment(documentId, contentType, inputStream));
                } catch (CouchbaseLiteException e){
                    e.printStackTrace();
                    result.error("errSave", "error add attachment " +filePath+" to document " + documentId, e.toString());
                }
                break;
            case ("removeAttachment"):
                documentId = call.argument("id");
                key = call.argument("key");
                try {
                    cbManager.removeAttachment(documentId, key);
                    result.success(true);
                } catch (CouchbaseLiteException e) {
                    e.printStackTrace();
                    result.error("errRemove", "error remove attachment " + key + " from document " + documentId, e.toString());
                }
                break;
            case ("purgeDocument"):
                _id = call.argument("id");
                try {
                    cbManager.purgeDocument(_id);
                    result.success(true);
                } catch (CouchbaseLiteException e) {
                    e.printStackTrace();
                    result.error("errGet", "error purgeDocument with id: " + _id, e.toString());
                }
                break;
            case ("deleteDocument"):
                _id = call.argument("id");
                try {
                    cbManager.deleteDocument(_id);
                    result.success(true);
                } catch (CouchbaseLiteException e) {
                    e.printStackTrace();
                    result.error("errGet", "error deleteDocument with id: " + _id, e.toString());
                }
                break;
            case ("setReplicatorEndpoint"):
                String _endpoint = call.argument("endpoint");
                try {
                    String _result = cbManager.setReplicatorEndpoint(_endpoint);
                    result.success(_result);
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                    result.error("errURI", "error setting the replicator endpoint uri to " + _endpoint, e.toString());
                }
                break;
            case ("setReplicatorType"):
                String _type = call.argument("type");
                try {
                    result.success(cbManager.setReplicatorType(_type));
                } catch (CouchbaseLiteException e) {
                    e.printStackTrace();
                    result.error("errReplType", "error setting replication type to " + _type, e.toString());
                }
                break;
            case ("setReplicatorBasicAuthentication"):
                Map<String, String> _auth = call.argument("auth");
                try {
                    result.success(cbManager.setReplicatorBasicAuthentication(_auth));
                } catch (Exception e) {
                    e.printStackTrace();
                    result.error("errAuth", "error setting authentication for replicator", null);
                }
                break;
            case ("setReplicatorSessionAuthentication"):
                String _sessionID = call.argument("sessiodID");
                try {
                    result.success(cbManager.setReplicatorSessionAuthentication(_sessionID));

                } catch (Exception e) {
                    e.printStackTrace();
                    result.error("errAuth", "invalid session ID", null);
                }
                break;
            case ("setReplicatorContinuous"):
                Boolean _continuous = call.argument("continuous");
                try {
                    result.success(cbManager.setReplicatorContinuous(_continuous));
                } catch (Exception e) {
                    e.printStackTrace();
                    result.error("errContinuous", "unable to set replication to continuous", null);
                }
                break;
            case ("initReplicator"):
                cbManager.initReplicator();
                result.success("");
                break;
            case ("startReplicator"):
                cbManager.startReplicator();
                result.success("");
                break;
            case ("stopReplicator"):
                cbManager.stopReplicator();
                result.success("");
                break;
            default:
                result.notImplemented();
        }
    }

    private CBManager getCBManager(String name) {
        CBManager manager = managers.get(name);
        if (manager == null) {
            manager = new CBManager();
            managers.put(name, manager);
        }
        return manager;

    }
}
