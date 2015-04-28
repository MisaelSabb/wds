package org.fao.fenix.wds.web.rest.fenix;

import com.google.gson.Gson;
import com.mongodb.*;
import com.mongodb.util.JSON;
import org.fao.fenix.wds.core.bean.DatasourceBean;
import org.fao.fenix.wds.core.jdbc.JDBCIterable;
import org.fao.fenix.wds.core.jdbc.MongoDBConnectionManager;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:guido.barbaglia@fao.org">Guido Barbaglia</a>
 * @author <a href="mailto:guido.barbaglia@gmail.com">Guido Barbaglia</a>
 * */
public class WDSUtils {

    public static Gson g = new Gson();

    public static StreamingOutput sqlStreamingOutputObject(final DatasourceBean ds, final String query) {

        return new StreamingOutput() {

            @Override
            public void write(OutputStream os) throws IOException, WebApplicationException {

                /* Initiate the JDBC iterable. */
                JDBCIterable it = new JDBCIterable();
                List<String> headers = new ArrayList<String>();

                try {

                    /* Query DB. */
                    it.query(ds, query);

                    /* Get column names. */
                    headers = it.getColumnNames();

                } catch (Exception e) {
                    e.printStackTrace();
                }

                /* Write the result of the query. */
                Writer writer = new BufferedWriter(new OutputStreamWriter(os));
                writer.write("[");
                while (it.hasNext()) {
                    List<String> s = it.next();
                    try {
                        StringBuilder sb = new StringBuilder();
                        sb.append("{");
                        for (int i = 0 ; i < s.size() ; i++) {
                            sb.append("\"").append(headers.get(i)).append("\": ").append("\"").append(s.get(i)).append("\"");
                            if (i < s.size() - 1)
                                sb.append(",");
                        }
                        sb.append("}");
                        writer.write(sb.toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (it.hasNext())
                        writer.write(",");
                }
                writer.write("]");

                /* Convert and write the output on the stream. */
                writer.flush();

            }

        };
    }

    public static StreamingOutput sqlStreamingOutputArray(final DatasourceBean ds, final String query) {

        return new StreamingOutput() {

            @Override
            public void write(OutputStream os) throws IOException, WebApplicationException {

                /* Initiate the JDBC iterable. */
                JDBCIterable it = new JDBCIterable();
                List<String> headers = new ArrayList<String>();

                try {

                    /* Query DB. */
                    it.query(ds, query);

                    /* Get column names. */
                    headers = it.getColumnNames();

                } catch (Exception e) {
                    e.printStackTrace();
                }

                /* Compute result. */
                Writer writer = new BufferedWriter(new OutputStreamWriter(os));

                /* Write the result of the query. */
                writer.write("[");
                while(it.hasNext()) {
                    writer.write(g.toJson(it.next()));
                    if (it.hasNext())
                        writer.write(",");
                }
                writer.write("]");

                /* Convert and write the output on the stream. */
                writer.flush();

            }

        };

    }

    public static StreamingOutput mongoStreamingOutput(final DatasourceBean ds, final String query, final String collection) {

        return new StreamingOutput() {

            @Override
            public void write(OutputStream os) throws IOException, WebApplicationException {

                /* Query MongoDB. */
                MongoDBConnectionManager mgr = MongoDBConnectionManager.getInstance();
                Mongo mongo = mgr.getMongo();
                DB db = mongo.getDB(ds.getDbName());
                DBCollection dbCollection = db.getCollection(collection);
                DBObject dbobj;
                DBCursor cursor;
                try {
                    dbobj = (DBObject) JSON.parse(query);
                    cursor = dbCollection.find(dbobj);
                } catch (Exception e) {
                    cursor = dbCollection.find();
                }


                /* Compute result. */
                Writer writer = new BufferedWriter(new OutputStreamWriter(os));

                try {
                    writer.write("[");
                    int count = 0;
                    while(cursor.hasNext()) {
                        writer.write(cursor.next().toString());
                        if (count < cursor.size() - 1)
                            writer.write(",");
                        count++;
                    }
                } finally {
                    cursor.close();
                    writer.write("]");
                }

                /* Convert and write the output on the stream. */
                writer.flush();

            }

        };

    }

}
