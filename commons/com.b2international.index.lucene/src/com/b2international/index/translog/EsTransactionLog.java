/*
 * Copyright 2011-2016 B2i Healthcare Pte Ltd, http://b2i.sg
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.b2international.index.translog;

import static com.google.common.collect.Lists.newArrayList;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.elasticsearch.cache.recycler.PageCacheRecycler;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.BigArrays;
import org.elasticsearch.index.engine.Engine;
import org.elasticsearch.index.mapper.ParseContext.Document;
import org.elasticsearch.index.mapper.ParsedDocument;
import org.elasticsearch.index.shard.ShardId;
import org.elasticsearch.index.translog.Translog;
import org.elasticsearch.index.translog.Translog.Snapshot;
import org.elasticsearch.index.translog.Translog.TranslogGeneration;
import org.elasticsearch.index.translog.TranslogConfig;
import org.slf4j.Logger;

import com.b2international.index.json.BulkUpdateOperation;
import com.b2international.index.json.Delete;
import com.b2international.index.json.Index;
import com.b2international.index.json.JsonDocumentMapping;
import com.b2international.index.json.JsonDocumentSearcher;
import com.b2international.index.json.Operation;
import com.b2international.index.mapping.Mappings;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Maps;

/**
 * @since 5.0
 */
public class EsTransactionLog implements TransactionLog {
	
	private final ObjectMapper mapper;
	private final Mappings mappings;
	private final Translog translog;
	private final Logger logger;

	public EsTransactionLog(final String indexName, final Path translogPath, final ObjectMapper mapper, final Mappings mappings,
			final Map<String, String> commitData, final Logger logger) throws IOException {
		this.mapper = mapper;
		this.mappings = mappings;
		this.logger = logger;
		
		final ShardId shardId = new ShardId(indexName, 0);
		
		final Settings indexSettings = Settings.builder()
			.put(PageCacheRecycler.TYPE, PageCacheRecycler.Type.NONE.name())
			.build();
		
		final TranslogConfig translogConfig = new TranslogConfig(shardId, translogPath, indexSettings, Translog.Durabilty.REQUEST,
				BigArrays.NON_RECYCLING_INSTANCE, null);
		
		if (commitData.containsKey(Translog.TRANSLOG_GENERATION_KEY)) {
			final long generation = Long.parseLong(commitData.get(Translog.TRANSLOG_GENERATION_KEY));
			translogConfig.setTranslogGeneration(new TranslogGeneration(commitData.get(Translog.TRANSLOG_UUID_KEY), generation));
		}
		
		this.translog = new Translog(translogConfig); 
	}

	@Override
	public void close() throws IOException {
		translog.close();
	}
	
	@Override
	public void commit(final IndexWriter writer) throws IOException {
		if (writer.hasUncommittedChanges()) {
			translog.prepareCommit();
			commitWriter(writer);
			translog.commit();
		}
	}

	@Override
	public void commitWriter(final IndexWriter writer) throws IOException {
		final Map<String, String> commitData = Maps.newHashMap();
		commitData.put(Translog.TRANSLOG_GENERATION_KEY, Long.toString(translog.getGeneration().translogFileGeneration));
		commitData.put(Translog.TRANSLOG_UUID_KEY, translog.getGeneration().translogUUID);
		
		writer.setCommitData(commitData);
		writer.commit();
	}
	
	@Override
	public void addOperation(final Operation op) throws IOException {
		final Collection<Translog.Operation> translogOps = toTranslogOperations(op);
		for (Translog.Operation translogOp : translogOps) {
			translog.add(translogOp);
		}
	}

	private Collection<Translog.Operation> toTranslogOperations(final Operation op) {
		if (op instanceof Index) {
			return Collections.<Translog.Operation>singleton(toTranslogIndexOperation((Index) op));
		} else if (op instanceof Delete) {
			return Collections.<Translog.Operation>singleton(toTranslogDeleteOperation((Delete) op));
		} else if (op instanceof BulkUpdateOperation<?>){
			return toTranslogIndexOperations((BulkUpdateOperation<?>) op);
		} else {
			throw new IllegalArgumentException(String.format("Unhandled operation type %s.", op));
		}
	}
	
	private Translog.Index toTranslogIndexOperation(final Index op) {
		final String opUid = op.uid();
		final StringField uid = new StringField("_uid", opUid, Store.NO);
		final LongField version = new LongField("_version", 0, Store.NO);
		final String type = op.mapping().type().getName();
		final BytesArray bytesArray = new BytesArray(op.source());
		
		final ParsedDocument document = new ParsedDocument(uid, version, op.key(), type, null, 
				0, 0, Collections.<Document> emptyList(), bytesArray, null);
		final Engine.Index index = new Engine.Index(new Term(opUid), document);

		return new Translog.Index(index);
	}
	
	private Translog.Delete toTranslogDeleteOperation(final Delete op) {
		final Term uid = JsonDocumentMapping._uid().toTerm(op.uid());
		final Engine.Delete delete = new Engine.Delete(null, null, uid);
		
		return new Translog.Delete(delete);
	}
	
	private Collection<Translog.Operation> toTranslogIndexOperations(final BulkUpdateOperation<?> op) {
		final Collection<Index> updates = op.updates();
		final Collection<Translog.Operation> ops = newArrayList();
		for (Index update : updates) {
			ops.add(toTranslogIndexOperation(update));
		}
		
		return ops;
	}

	@Override
	public void recoverFromTranslog(final IndexWriter writer, final JsonDocumentSearcher searcher) throws IOException {
		final Stopwatch w = Stopwatch.createStarted();
		logger.info("Starting recovery from translog.");
		
		final Snapshot snapshot = translog.newSnapshot();
		Translog.Operation op = null;
		
		while ((op = snapshot.next()) != null) {
			final Operation operation = toOperation(op);
			operation.execute(writer, searcher);
		}
		
		final int recoveredOps = snapshot.estimatedTotalOperations();
		
		if (recoveredOps == 0) {
			logger.info("No operations were found to recover.");
		} else {
			commit(writer);
			logger.info(String.format("Recovered %d operations from translog in %s.", recoveredOps, w));
		}
	}

	private Operation toOperation(final Translog.Operation op) {
		switch (op.opType()) {
		case SAVE:
			final Translog.Index index = (Translog.Index) op;
			return new Index(index.id(), index.source().toBytes(), mapper, mappings.getByType(index.type()));
		case DELETE:
			final Translog.Delete delete = (Translog.Delete) op;
			return new Delete(delete.uid().text());
		default:
			throw new IllegalArgumentException(String.format("Unhandled translog operation type %s.", op.opType().name()));
		}
	}
	
	@Override
	public void sync() throws IOException {
		final Stopwatch w = Stopwatch.createStarted();
		logger.trace("Starting translog sync.");
		translog.sync();
		logger.trace(String.format("Translog sync finished in %s.", w));
	}
	
}
