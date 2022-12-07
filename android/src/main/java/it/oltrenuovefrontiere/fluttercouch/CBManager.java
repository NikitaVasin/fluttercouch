package it.oltrenuovefrontiere.fluttercouch;

import android.util.Log;

import com.couchbase.lite.BasicAuthenticator;
import com.couchbase.lite.Blob;
import com.couchbase.lite.CouchbaseLite;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.DataSource;
import com.couchbase.lite.Database;
import com.couchbase.lite.DatabaseChangeListener;
import com.couchbase.lite.DatabaseConfiguration;
import com.couchbase.lite.Document;
import com.couchbase.lite.Endpoint;
import com.couchbase.lite.Expression;
import com.couchbase.lite.ListenerToken;
import com.couchbase.lite.Meta;
import com.couchbase.lite.MutableDocument;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryBuilder;
import com.couchbase.lite.Replicator;
import com.couchbase.lite.ReplicatorChangeListener;
import com.couchbase.lite.ReplicatorConfiguration;
import com.couchbase.lite.Result;
import com.couchbase.lite.ResultSet;
import com.couchbase.lite.SelectResult;
import com.couchbase.lite.SessionAuthenticator;
import com.couchbase.lite.URLEndpoint;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class CBManager {

    private Database mDatabase;
    private ReplicatorConfiguration mReplicatorConfig;
    private Replicator mReplicator;
    private String attachPath;

    public void initDatabaseWithName(String _name) throws CouchbaseLiteException {
        CouchbaseLite.init(FluttercouchPlugin.context);
        DatabaseConfiguration config = new DatabaseConfiguration();
        if (mDatabase == null) {
            mDatabase = new Database(_name, config);
        }
        attachPath = mDatabase.getPath();
    }

    public void close() throws CouchbaseLiteException {
        ensureInitialized();
        mDatabase.close();
        mDatabase = null;
    }

    public void delete() throws CouchbaseLiteException {
        ensureInitialized();
        mDatabase.delete();
        mDatabase = null;
    }

    public String saveDocument(Map<String, Object> _map) throws CouchbaseLiteException {
        ensureInitialized();
        MutableDocument mutableDoc = new MutableDocument(_map);
        mDatabase.save(mutableDoc);
        return mutableDoc.getId();
    }

    public ListenerToken addDatabaseChangeListener(DatabaseChangeListener listener) {
        ensureInitialized();
        return mDatabase.addChangeListener(listener);
    }

    public void removeDatabaseChangeListener(ListenerToken token) {
        ensureInitialized();
        mDatabase.removeChangeListener(token);
    }


    public ListenerToken addReplicationChangeListener(ReplicatorChangeListener listener) {
        ensureInitialized();
        return mReplicator.addChangeListener(listener);
    }

    public void removeReplicationChangeListener(ListenerToken token) {
        ensureInitialized();
        mReplicator.removeChangeListener(token);
    }

    public String saveDocumentWithId(String _id, Map<String, Object> _map) throws CouchbaseLiteException {
        ensureInitialized();
        Document document = mDatabase.getDocument(_id);
        for (String key : document.getKeys()) {
            if (key.equals("_attachments")) {
                _map.put(key, document.getValue(key));
            }
            Blob b = document.getBlob(key);
            if (b != null) _map.put(key, b);
        }
        removeAttachmentsPath(_map);
        MutableDocument mutableDoc = new MutableDocument(_id, _map);
        mDatabase.save(mutableDoc);
        return mutableDoc.getId();
    }

    public Map<String, Object> getDocumentWithId(String _id) throws CouchbaseLiteException {
        ensureInitialized();
        Map<String, Object> resultMap = new HashMap<>();
        try {
            Document document = mDatabase.getDocument(_id);
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
        return resultMap;
    }

    public Map<String, Object> getDocumentsWith(String key, String value) throws CouchbaseLiteException {
        ensureInitialized();
        HashMap<String, Object> resultMap = new HashMap<String, Object>();
        Query query = QueryBuilder.select(SelectResult.expression(Meta.id))
                .from(DataSource.database(mDatabase))
                .where(Expression.property(key).equalTo(Expression.string(value)));
        try {
            ResultSet result = query.execute();
            ArrayList docs = new ArrayList();
            for (Result res : result.allResults()) {
                HashMap<String, Object> ret = new HashMap<String, Object>();
                ret.put("doc", processDoc(mDatabase.getDocument(res.getString("id"))));
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
        ensureInitialized();
        HashMap<String, Object> resultMap = new HashMap<String, Object>();
        Query query = QueryBuilder.select(SelectResult.expression(Meta.id))
                .from(DataSource.database(mDatabase));
        try {
            ResultSet result = query.execute();
            ArrayList docs = new ArrayList();
            for (Result res : result.allResults()) {
                HashMap<String, Object> ret = new HashMap<String, Object>();
                ret.put("doc", processDoc(mDatabase.getDocument(res.getString("id"))));
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

    public String addAttachment(String _id, String contentType, InputStream is) throws CouchbaseLiteException, IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            ensureInitialized();
            MutableDocument document = mDatabase.getDocument(_id).toMutable();
            String key = new RandomString(5, new Random()).nextString();
            IOUtils.inputStreamToOutputStream(is, os);
            Blob b = new Blob(contentType, os.toByteArray());
            document.setBlob(key, b);
            mDatabase.save(document);
            return key;
        } finally {
            IOUtils.closeSafe(is);
            IOUtils.closeSafe(os);
        }

    }

    public void removeAttachment(String _id, String key) throws CouchbaseLiteException {
        ensureInitialized();
        MutableDocument document = mDatabase.getDocument(_id).toMutable();
        document.setBlob(key, null);
        mDatabase.save(document);
    }

    public void purgeDocument(String _id) throws CouchbaseLiteException {
        ensureInitialized();
        Document document = mDatabase.getDocument(_id);
        if (document != null) {
            mDatabase.purge(document);
        }
    }

    public void deleteDocument(String _id) throws CouchbaseLiteException {
        ensureInitialized();
        Document document = mDatabase.getDocument(_id);
        if (document != null) {
            mDatabase.delete(document);
        }
    }


    public String setReplicatorEndpoint(String _endpoint) throws URISyntaxException {
        ensureInitialized();
        Endpoint targetEndpoint = new URLEndpoint(new URI(_endpoint));
        mReplicatorConfig = new ReplicatorConfiguration(mDatabase, targetEndpoint);
        return mReplicatorConfig.getTarget().toString();
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
        mReplicatorConfig.setReplicatorType(settedType);
        return settedType.toString();
    }

    public String setReplicatorBasicAuthentication(Map<String, String> _auth) throws Exception {
        if (_auth.containsKey("username") && _auth.containsKey("password")) {
            mReplicatorConfig.setAuthenticator(new BasicAuthenticator(_auth.get("username"), _auth.get("password").toCharArray()));
        } else {
            throw new Exception();
        }
        return mReplicatorConfig.getAuthenticator().toString();
    }

    public String setReplicatorSessionAuthentication(String sessionID) throws Exception {
        if (sessionID != null) {
            mReplicatorConfig.setAuthenticator(new SessionAuthenticator(sessionID));
        } else {
            throw new Exception();
        }
        return mReplicatorConfig.getAuthenticator().toString();
    }

    public boolean setReplicatorContinuous(boolean _continuous) {
        mReplicatorConfig.setContinuous(_continuous);
        return mReplicatorConfig.isContinuous();
    }

    public void initReplicator() {
        mReplicator = new Replicator(mReplicatorConfig);
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

    private String getBlobPath(Blob blob) {
        return  attachPath +"Attachments/" + blob.digest().substring(5).replace("/", "_") + ".blob";
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> processDoc(Document document) {
        Map<String, Object> retrievedDocument = new HashMap<>(document.toMap());
        Map<String, Object> attachmentsPath = new HashMap<>();

        for (String key : retrievedDocument.keySet()) {
            Blob b = document.getBlob(key);
            if (b != null) {
                attachmentsPath.put(key, getBlobPath(b));
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
                    attachmentsPath.put(key, getBlobPath(((Blob) b)));
                }
            }
            retrievedDocument.remove("_attachments");
        }

        if (attachmentsPath.size() > 0) {
            retrievedDocument = (Map<String, Object>) addAttachmentsPath(attachmentsPath, retrievedDocument, null);
        }
        return retrievedDocument;
    }


    private void removeAttachmentsPath(final Object object) {
        if (object instanceof Map) {
            Map<String, Object> map = ((Map<String, Object>) object);
            if (map.containsKey("contentType")) {
                map.remove("path");
            }
            for (String key : map.keySet()) {
                removeAttachmentsPath(map.get(key));
            }
        } else if (object instanceof List) {
            List<Object> list = (List<Object>) object;
            for (Object o : list) {
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

    private void ensureInitialized() {
        if (mDatabase == null) throw new IllegalStateException("Database not initialized yet");
    }
}
