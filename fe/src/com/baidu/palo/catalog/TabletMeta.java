// Copyright (c) 2017, Baidu.com, Inc. All Rights Reserved

// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package com.baidu.palo.catalog;

import com.google.common.base.Preconditions;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TabletMeta {
    private static final Logger LOG = LogManager.getLogger(TabletMeta.class);

    private final long dbId;
    private final long tableId;
    private final long partitionId;
    private final long indexId;

    private int oldSchemaHash;
    private int newSchemaHash;

    private ReentrantReadWriteLock lock;

    public TabletMeta(long dbId, long tableId, long partitionId, long indexId, int schemaHash) {
        this.dbId = dbId;
        this.tableId = tableId;
        this.partitionId = partitionId;
        this.indexId = indexId;

        this.oldSchemaHash = schemaHash;
        this.newSchemaHash = -1;

        lock = new ReentrantReadWriteLock();
    }

    public long getDbId() {
        return dbId;
    }

    public long getTableId() {
        return tableId;
    }

    public long getPartitionId() {
        return partitionId;
    }

    public long getIndexId() {
        return indexId;
    }

    public void setNewSchemaHash(int newSchemaHash) {
        lock.writeLock().lock();
        try {
            Preconditions.checkState(this.newSchemaHash == -1);
            this.newSchemaHash = newSchemaHash;
            LOG.debug("setNewSchemaHash: {}", toString());
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void updateToNewSchemaHash() {
        lock.writeLock().lock();
        try {
            Preconditions.checkState(this.newSchemaHash != -1);
            int tmp = this.oldSchemaHash;
            this.oldSchemaHash = this.newSchemaHash;
            this.newSchemaHash = tmp;
            LOG.debug("updateToNewSchemaHash: " + toString());
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void deleteNewSchemaHash() {
        lock.writeLock().lock();
        try {
            LOG.debug("deleteNewSchemaHash: " + toString());
            this.newSchemaHash = -1;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int getOldSchemaHash() {
        lock.readLock().lock();
        try {
            return this.oldSchemaHash;
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean containsSchemaHash(int schemaHash) {
        lock.readLock().lock();
        try {
            return this.oldSchemaHash == schemaHash || this.newSchemaHash == schemaHash;
        } finally {
            lock.readLock().unlock();
        }
    }

    // XXX
    public void forceSetSchema(int schemaHash) {
        lock.writeLock().lock();
        try {
            this.oldSchemaHash = schemaHash;
            this.newSchemaHash = -1;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public String toString() {
        lock.readLock().lock();
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("dbId=" + dbId);
            sb.append(" tableId=" + tableId);
            sb.append(" partitionId=" + partitionId);
            sb.append(" indexId=" + indexId);
            sb.append(" oldSchemaHash=" + oldSchemaHash);
            sb.append(" newSchemaHash=" + newSchemaHash);

            return sb.toString();
        } finally {
            lock.readLock().unlock();
        }
    }
}
