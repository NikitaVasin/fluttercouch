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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        MutableDocument mutableDoc = new MutableDocument(_id, _map);
        mDatabase.get(defaultDatabase).save(mutableDoc);
        return mutableDoc.getId();
    }

    public Map<String, Object> getDocumentWithId(String _id) throws CouchbaseLiteException {
        Database defaultDb = getDatabase();
        Map<String, Object> resultMap = new HashMap<>();
        if (defaultDatabase != null) {
            try {
                Document document = defaultDb.getDocument(_id);
                if (document != null) {
                    resultMap.put("doc", processDoc(document.toMap()));
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
        Query query = QueryBuilder.select(SelectResult.all(), SelectResult.expression(Meta.id))
                .from(DataSource.database(defaultDb))
                .where(Expression.property(key).equalTo(Expression.string(value)));
        try {
            ResultSet result = query.execute();
            ArrayList docs = new ArrayList();
            String dbName = defaultDb.getName();
            for (Result res : result.allResults()) {
                HashMap<String, Object> ret = new HashMap<String, Object>();
                Object doc = res.toMap().get(dbName);
                ret.put("doc", processDoc(( Map<String, Object>)doc));
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
        Query query = QueryBuilder.select(SelectResult.all(), SelectResult.expression(Meta.id))
                .from(DataSource.database(defaultDb));
        try {
            ResultSet result = query.execute();
            ArrayList docs = new ArrayList();
            String dbName = defaultDb.getName();
            for (Result res : result.allResults()) {
                HashMap<String, Object> ret = new HashMap<String, Object>();
                Object doc = res.toMap().get(dbName);
                ret.put("doc", processDoc(( Map<String, Object>)doc));
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

    public void purgeDocument(String _id) throws CouchbaseLiteException {
        Database defaultDb = getDatabase();
        Document document = defaultDb.getDocument(_id);
        if (document != null) {
            defaultDb.purge(document);
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
    private Map<String, Object> processDoc(Map<String, Object> document) {
        Map<String, Object> retrievedDocument = new HashMap<>(document);

        Map<String, Object> attachments = (Map<String, Object>) retrievedDocument.get("_attachments");
        if (attachments != null) {
            Map<String, Object> attachmentsPath = new HashMap<>();
            for (String key : attachments.keySet()) {
                Object b = attachments.get(key);
                if (b instanceof Blob) {
                    attachmentsPath.put(key, ((Blob) b).getFilePath());
                }
            }
            retrievedDocument.remove("_attachments");

            retrievedDocument = (Map<String, Object>) replaceFilesFromAttachments(attachmentsPath, retrievedDocument, null);
        }
        return retrievedDocument;
    }

    @SuppressWarnings("unchecked")
    private Object replaceFilesFromAttachments(final Map<String, Object> attachments, final Object object, String key) {
        if (object instanceof Map) {
            Map<String, Object> dict = ((Map<String, Object>) object);
            Map<String, Object> d = new HashMap<>(dict);
            for (Map.Entry<String, Object> entry : dict.entrySet()) {
                d.put(entry.getKey(), replaceFilesFromAttachments(attachments, entry.getValue(), entry.getKey()));
            }
            return d;
        } else if (object instanceof List) {
            List<Object> list = (List<Object>) object;
            List<Object> l = new ArrayList<>(list.size());
            for (Object it : list) {
                l.add(replaceFilesFromAttachments(attachments, it, null));
            }
            return l;
        } else if (object instanceof String) {
            if ("file".equals(key)) return attachments.get(object);
        }
        return object;
    }
}
