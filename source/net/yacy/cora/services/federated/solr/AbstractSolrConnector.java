/**
 *  AbstractSolrConnector
 *  Copyright 2012 by Michael Peter Christen
 *  First released 21.06.2012 at http://yacy.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program in the file lgpl21.txt
 *  If not, see <http://www.gnu.org/licenses/>.
 */

package net.yacy.cora.services.federated.solr;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.yacy.kelondro.logging.Log;
import net.yacy.search.index.SolrField;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrInputDocument;

public class AbstractSolrConnector implements SolrConnector {

    protected SolrServer server;

    protected AbstractSolrConnector() {
        this.server = null;
    }

    protected void init(SolrServer server) {
        this.server = server;
    }

    public SolrServer getServer() {
        return this.server;
    }

    @Override
    public synchronized void close() {
        try {
            this.server.commit();
        } catch (SolrServerException e) {
            Log.logException(e);
        } catch (IOException e) {
            Log.logException(e);
        }
    }

    @Override
    public long getSize() {
        try {
            final SolrDocumentList list = get("*:*", 0, 1);
            return list.getNumFound();
        } catch (final Throwable e) {
            Log.logException(e);
            return 0;
        }
    }

    /**
     * delete everything in the solr index
     * @throws IOException
     */
    @Override
    public void clear() throws IOException {
        try {
            this.server.deleteByQuery("*:*");
            this.server.commit();
        } catch (final Throwable e) {
            throw new IOException(e);
        }
    }

    @Override
    public void delete(final String id) throws IOException {
        try {
            this.server.deleteById(id);
        } catch (final Throwable e) {
            throw new IOException(e);
        }
    }

    @Override
    public void delete(final List<String> ids) throws IOException {
        try {
            this.server.deleteById(ids);
        } catch (final Throwable e) {
            throw new IOException(e);
        }
    }

    @Override
    public boolean exists(final String id) throws IOException {
        try {
            final SolrDocumentList list = get(SolrField.id.getSolrFieldName() + ":" + id, 0, 1);
            return list.getNumFound() > 0;
        } catch (final Throwable e) {
            Log.logException(e);
            return false;
        }
    }

    public void add(final File file, final String solrId) throws IOException {
        final ContentStreamUpdateRequest up = new ContentStreamUpdateRequest("/update/extract");
        up.addFile(file);
        up.setParam("literal.id", solrId);
        up.setParam("uprefix", "attr_");
        up.setParam("fmap.content", "attr_content");
        //up.setAction(AbstractUpdateRequest.ACTION.COMMIT, true, true);
        try {
            this.server.request(up);
            this.server.commit();
        } catch (final Throwable e) {
            throw new IOException(e);
        }
    }

    @Override
    public void add(final SolrDoc solrdoc) throws IOException, SolrException {
        try {
            this.server.add(solrdoc,180000); // commitWithIn 180s
            //this.server.commit();
        } catch (SolrServerException e) {
            Log.logWarning("SolrConnector", e.getMessage() + " DOC=" + solrdoc.toString());
            throw new IOException(e);
        }
    }

    @Override
    public void add(final Collection<SolrDoc> solrdocs) throws IOException, SolrException {
        ArrayList<SolrInputDocument> l = new ArrayList<SolrInputDocument>();
        for (SolrDoc d: solrdocs) l.add(d);
        try {
            this.server.add(l,180000); // commitWithIn 120s
            //this.server.commit();
        } catch (SolrServerException e) {
            Log.logWarning("SolrConnector", e.getMessage() + " DOC=" + solrdocs.toString());
            throw new IOException(e);
        }
    }

    /**
     * get a query result from solr
     * to get all results set the query String to "*:*"
     * @param querystring
     * @throws IOException
     */
    @Override
    public SolrDocumentList get(final String querystring, final int offset, final int count) throws IOException {
        // construct query
        final SolrQuery query = new SolrQuery();
        query.setQuery(querystring);
        query.setRows(count);
        query.setStart(offset);
        //query.addSortField( "price", SolrQuery.ORDER.asc );

        // query the server
        //SearchResult result = new SearchResult(count);
        try {
            final QueryResponse rsp = this.server.query( query );
            final SolrDocumentList docs = rsp.getResults();
            return docs;
            // add the docs into the YaCy search result container
            /*
            for (SolrDocument doc: docs) {
                result.put(element)
            }
            */
        } catch (final Throwable e) {
            throw new IOException(e);
        }

        //return result;
    }

}