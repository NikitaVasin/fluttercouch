package it.oltrenuovefrontiere.fluttercouch;

import android.util.Log;

import com.couchbase.lite.BasicAuthenticator;
import com.couchbase.lite.Blob;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.DataSource;
import com.couchbase.lite.Database;
import com.couchbase.lite.DatabaseConfiguration;
import com.couchbase.lite.Document;
import com.couchbase.lite.Endpoint;
import com.couchbase.lite.Expression;
import com.couchbase.lite.Meta;
import com.couchbase.lite.MutableDocument;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryBuilder;
import com.couchbase.lite.Replicator;
import com.couchbase.lite.ReplicatorConfiguration;
import com.couchbase.lite.Result;
import com.couchbase.lite.ResultSet;
import com.couchbase.lite.SelectResult;
import com.couchbase.lite.SessionAuthenticator;
import com.couchbase.lite.URLEndpoint;
import java.io.InputStream;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class CBManager {

    private HashMap<String, Database> mDatabase = new HashMap<>();
    private ReplicatorConfiguration mReplConfig;
    private Replicator mReplicator;
    private String defaultDatabase = "defaultDatabase";

    public CBManager() {
    }

    public Database getDatabase() {
        return mDatabase.get(defaultDatabase);
    }

    public Database getDatabase(String name) {
        if (mDatabase.containsKey(name)) {
            return mDatabase.get(name);
        }
        return null;
    }

    public String saveDocument(Map<String, Object> _map) throws CouchbaseLiteException {
        MutableDocument mutableDoc = new MutableDocument(_map);
        mDatabase.get(defaultDatabase).save(mutableDoc);
        return mutableDoc.getId();
    }

    public String saveDocumentWithId(String _id, Map<String, Object> _map) throws CouchbaseLiteException {
        Database defaultDb = mDatabase.get(defaultDatabase);
        Document document = defaultDb.getDocument(_id);
        for (String key : document.getKeys()) {
            if (key.equals("_attachments")) {
                _map.put(key, document.getValue(key));
            }
            Blob b = document.getBlob(key);
            if (b != null) _map.put(key, b);
        }
        removeAttachmentsPath(_map);
        MutableDocument mutableDoc = new MutableDocument(_id, _map);
        defaultDb.save(mutableDoc);
        return mutableDoc.getId();
    }

    public Map<String, Object> getDocumentWithId(String _id) throws CouchbaseLiteException {
        Database defaultDb = getDatabase();
        Map<String, Object> resultMap = new HashMap<>();
        if (defaultDatabase != null) {
            try {
                Document document = defaultDb.getDocument(_id);
                if (document != null) {
                    resultMap.put("doc", processDoc(document));
                    resultMap.put("id", document.getId());
                } else {
                    resultMap.put("doc", null);
                    resultMap.put("id", _id);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return resultMap;
    }

    public Map<String, Object> getDocumentsWith(String key, String value) throws CouchbaseLiteException {
        Database defaultDb = getDatabase();
        HashMap<String, Object> resultMap = new HashMap<String, Object>();
        Query query = QueryBuilder.select(SelectResult.expression(Meta.id))
                .from(DataSource.database(defaultDb))
                .where(Expression.property(key).equalTo(Expression.string(value)));
        try {
            ResultSet result = query.execute();
            ArrayList docs = new ArrayList();
            String dbName = defaultDb.getName();
            for (Result res : result.allResults()) {
                HashMap<String, Object> ret = new HashMap<String, Object>();
                ret.put("doc", processDoc(defaultDb.getDocument(res.getString("id"))));
                ret.put("id", res.getString("id"));
                docs.add(ret);
            }
            resultMap.put("docs", docs);
        } catch (Exception e) {
            e.printStackTrace();
            resultMap.put("docs", null);
        }
        return resultMap;
    }

    public Map<String, Object> getAllDocuments() throws CouchbaseLiteException {
        Database defaultDb = getDatabase();
        HashMap<String, Object> resultMap = new HashMap<String, Object>();
        Query query = QueryBuilder.select(SelectResult.expression(Meta.id))
                .from(DataSource.database(defaultDb));
        try {
            ResultSet result = query.execute();
            ArrayList docs = new ArrayList();
            String dbName = defaultDb.getName();
            for (Result res : result.allResults()) {
                HashMap<String, Object> ret = new HashMap<String, Object>();
                ret.put("doc", processDoc(defaultDb.getDocument(res.getString("id"))));
                ret.put("id", res.getString("id"));
                docs.add(ret);
            }
            resultMap.put("docs", docs);
        } catch (Exception e) {
            e.printStackTrace();
            resultMap.put("docs", null);
        }
        return resultMap;
    }

    public String addAttachment(String _id, String contentType, InputStream inputStream) throws CouchbaseLiteException {
        Database defaultDb = getDatabase();
        MutableDocument document = defaultDb.getDocument(_id).toMutable();
        String key = new RandomString(5, new Random()).nextString();
        Blob b = new Blob(contentType, inputStream/*new File(filePath).toURI().toURL()*/);
        document.setBlob(key, b);
        defaultDb.save(document);
        return key;
    }

    public void removeAttachment(String _id, String key) throws CouchbaseLiteException {
        Database defaultDb = getDatabase();
        MutableDocument document = defaultDb.getDocument(_id).toMutable();
        document.setBlob(key, null);
        defaultDb.save(document);
    }

    public void purgeDocument(String _id) throws CouchbaseLiteException {
        Database defaultDb = getDatabase();
        Document document = defaultDb.getDocument(_id);
        if (document != null) {
            defaultDb.purge(document);
        }
    }

    public void deleteDocument(String _id) throws CouchbaseLiteException {
        Database defaultDb = getDatabase();
        Document document = defaultDb.getDocument(_id);
        if (document != null) {
            defaultDb.delete(document);
        }
    }

    public void initDatabaseWithName(String _name) throws CouchbaseLiteException {
        DatabaseConfiguration config = new DatabaseConfiguration(FluttercouchPlugin.context);
        if (!mDatabase.containsKey(_name)) {
            defaultDatabase = _name;
            // Database.setLogLevel(LogDomain.REPLICATOR, LogLevel.VERBOSE);
            mDatabase.put(_name, new Database(_name, config));
        }
    }

    public void delete() throws CouchbaseLiteException {
        Database defaultDb = getDatabase();
        defaultDb.delete();
        mDatabase.clear();
    }

    public String setReplicatorEndpoint(String _endpoint) throws URISyntaxException {
        Endpoint targetEndpoint = new URLEndpoint(new URI(_endpoint));
        mReplConfig = new ReplicatorConfiguration(mDatabase.get(defaultDatabase), targetEndpoint);
        return mReplConfig.getTarget().toString();
    }

    public String setReplicatorType(String _type) throws CouchbaseLiteException {
        ReplicatorConfiguration.ReplicatorType settedType = ReplicatorConfiguration.ReplicatorType.PULL;
        if (_type.equals("PUSH")) {
            settedType = ReplicatorConfiguration.ReplicatorType.PUSH;
        } else if (_type.equals("PULL")) {
            settedType = ReplicatorConfiguration.ReplicatorType.PULL;
        } else if (_type.equals("PUSH_AND_PULL")) {
            settedType = ReplicatorConfiguration.ReplicatorType.PUSH_AND_PULL;
        }
        mReplConfig.setReplicatorType(settedType);
        return settedType.toString();
    }

    public String setReplicatorBasicAuthentication(Map<String, String> _auth) throws Exception {
        if (_auth.containsKey("username") && _auth.containsKey("password")) {
            mReplConfig.setAuthenticator(new BasicAuthenticator(_auth.get("username"), _auth.get("password")));
        } else {
            throw new Exception();
        }
        return mReplConfig.getAuthenticator().toString();
    }

    public String setReplicatorSessionAuthentication(String sessionID) throws Exception {
        if (sessionID != null) {
            mReplConfig.setAuthenticator(new SessionAuthenticator(sessionID));
        } else {
            throw new Exception();
        }
        return mReplConfig.getAuthenticator().toString();
    }

    public boolean setReplicatorContinuous(boolean _continuous) {
        mReplConfig.setContinuous(_continuous);
        return mReplConfig.isContinuous();
    }

    public void initReplicator() {
        mReplicator = new Replicator(mReplConfig);
    }

    public void startReplicator() {
        mReplicator.start();
    }

    public void stopReplicator() {
        mReplicator.stop();
        mReplicator = null;
    }

    public Replicator getReplicator() {
        return mReplicator;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> processDoc(Document document) {
        Map<String, Object> retrievedDocument = new HashMap<>(document.toMap());
        Map<String, Object> attachmentsPath = new HashMap<>();
        for (String key : retrievedDocument.keySet()) {
            Blob b = document.getBlob(key);
            if (b != null) {
                attachmentsPath.put(key, b.getFilePath());
            }
        }
        for (String key : attachmentsPath.keySet()) {
            retrievedDocument.remove(key);
        }
        Map<String, Object> attachments = (Map<String, Object>) retrievedDocument.get("_attachments");
        if (attachments != null) {
            for (String key : attachments.keySet()) {
                Object b = attachments.get(key);

                if (b instanceof Blob) {
                    attachmentsPath.put(key, ((Blob) b).getFilePath());
                }
            }
            retrievedDocument.remove("_attachments");
        }

        if (attachmentsPath.size() > 0) {
            retrievedDocument = (Map<String, Object>) addAttachmentsPath(attachmentsPath, retrievedDocument, null);
        }
        return retrievedDocument;
    }


    private void removeAttachmentsPath(final Object object){
        if(object instanceof  Map){
            Map<String, Object> map = ((Map<String, Object>) object);
            if(map.containsKey("contentType")){
                map.remove("path");
            }
            for(String key:map.keySet()){
                removeAttachmentsPath(map.get(key));
            }
        }else if(object instanceof List){
            List<Object> list = (List<Object>) object;
            for(Object o:list){
                removeAttachmentsPath(o);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Object addAttachmentsPath(final Map<String, Object> attachments, final Object object, String key) {
        if (object instanceof Map) {
            Map<String, Object> dict = ((Map<String, Object>) object);
            Map<String, Object> d = new HashMap<>(dict);
            Object attachPath = attachments.get((String) d.get("file"));
            if (attachPath != null) d.put("path", attachPath);
            for (Map.Entry<String, Object> entry : dict.entrySet()) {
                d.put(entry.getKey(), addAttachmentsPath(attachments, entry.getValue(), entry.getKey()));
            }

            return d;
        } else if (object instanceof List) {
            List<Object> list = (List<Object>) object;
            List<Object> l = new ArrayList<>(list.size());
            for (Object it : list) {
                l.add(addAttachmentsPath(attachments, it, null));
            }
            return l;
        }
        return object;
    }
}
