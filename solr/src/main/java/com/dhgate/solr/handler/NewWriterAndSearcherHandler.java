package com.dhgate.solr.handler;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.lucene.index.IndexCommit;
import org.apache.solr.common.SolrException;
import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.RequestHandlerBase;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.util.RefCounted;
import org.apache.solr.util.plugin.SolrCoreAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NewWriterAndSearcherHandler extends RequestHandlerBase implements SolrCoreAware {
	private static final Logger LOG = LoggerFactory.getLogger(NewWriterAndSearcherHandler.class.getName());
	private SolrCore core;
	@Override
	public void inform(SolrCore core) {
		this.core = core;
	}

	@Override
	public void handleRequestBody(SolrQueryRequest req, SolrQueryResponse rsp) throws Exception {
		boolean isFullCopyNeeded = req.getParams().getBool("fullcopy", false);
		SolrCore solrCore = req.getCore();
		solrCore.getUpdateHandler().newIndexWriter(isFullCopyNeeded);
	    RefCounted<SolrIndexSearcher> searcher = null;
	    IndexCommit commitPoint;
	    try {
	      Future[] waitSearcher = new Future[1];
	      searcher = solrCore.getSearcher(true, true, waitSearcher, true);
	      if (waitSearcher[0] != null) {
	        try {
	          waitSearcher[0].get();
	        } catch (InterruptedException e) {
	          SolrException.log(LOG, e);
	        } catch (ExecutionException e) {
	          SolrException.log(LOG, e);
	        }
	      }
	      commitPoint = searcher.get().getIndexReader().getIndexCommit();
	    } finally {
	      req.close();
	      if (searcher != null) {
	        searcher.decref();
	      }
	    }
	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSource() {
		// TODO Auto-generated method stub
		return null;
	}

}
