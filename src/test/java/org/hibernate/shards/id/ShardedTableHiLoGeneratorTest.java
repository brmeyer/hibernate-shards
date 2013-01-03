/**
 * Copyright (C) 2007 Google Inc.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.

 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.

 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */

package org.hibernate.shards.id;

import java.io.Serializable;
import java.sql.Connection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.Query;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.cache.spi.CacheKey;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.jdbc.spi.JdbcConnectionAccess;
import org.hibernate.engine.query.spi.sql.NativeSQLQuerySpecification;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.NonFlushedChanges;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.transaction.spi.TransactionCoordinator;
import org.hibernate.internal.CriteriaImpl;
import org.hibernate.loader.custom.CustomQuery;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.shards.defaultmock.SessionDefaultMock;
import org.hibernate.shards.defaultmock.SessionImplementorDefaultMock;
import org.hibernate.shards.session.ControlSessionProvider;
import org.hibernate.type.Type;

/**
 * @author maxr@google.com (Max Ross)
 */
public class ShardedTableHiLoGeneratorTest extends TestCase {

    public void testGenerate() {
        final SessionImplementor controlSessionToReturn = new MySession();
        ControlSessionProvider provider = new ControlSessionProvider() {
            public SessionImplementor openControlSession() {
                return controlSessionToReturn;
            }
        };
        final SessionImplementor session = new SessionImplementorDefaultMock();
        ShardedTableHiLoGenerator gen = new ShardedTableHiLoGenerator() {
            @Override
            Serializable superGenerate(SessionImplementor controlSession, Object obj) {
                assertSame(controlSessionToReturn, controlSession);
                return 33;
            }
        };
        gen.setControlSessionProvider(provider);
        assertEquals(33, gen.generate(session, null));
    }

    private static final class MySession extends SessionDefaultMock implements SessionImplementor {

        @Override
        public Connection close() throws HibernateException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Interceptor getInterceptor() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setAutoClear(boolean enabled) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isTransactionInProgress() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void initializeCollection(PersistentCollection collection,
                                         boolean writing) throws HibernateException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object internalLoad(String entityName, Serializable id, boolean eager,
                                   boolean nullable) throws HibernateException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object immediateLoad(String entityName, Serializable id) throws HibernateException {
            throw new UnsupportedOperationException();
        }

        @Override
        public long getTimestamp() {
            throw new UnsupportedOperationException();
        }

        @Override
        public SessionFactoryImplementor getFactory() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ScrollableResults scroll(CriteriaImpl criteria, ScrollMode scrollMode) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List list(CriteriaImpl criteria) {
            throw new UnsupportedOperationException();
        }

        @Override
        public EntityPersister getEntityPersister(String entityName, Object object) throws HibernateException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object getEntityUsingInterceptor(EntityKey key) throws HibernateException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Serializable getContextEntityIdentifier(Object object) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String bestGuessEntityName(Object object) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String guessEntityName(Object entity) throws HibernateException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object instantiate(String entityName, Serializable id) throws HibernateException {
            throw new UnsupportedOperationException();
        }


        @Deprecated
        @Override
        public Object getFilterParameterValue(String filterParameterName) {
            throw new UnsupportedOperationException();
        }

        @Deprecated
        @Override
        public Type getFilterParameterType(String filterParameterName) {
            throw new UnsupportedOperationException();
        }

        @Deprecated
        @Override
        public Map getEnabledFilters() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getDontFlushFromFind() {
            throw new UnsupportedOperationException();
        }

        @Override
        public PersistenceContext getPersistenceContext() {
            throw new UnsupportedOperationException();
        }


        @Override
        public NonFlushedChanges getNonFlushedChanges() throws HibernateException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void applyNonFlushedChanges(NonFlushedChanges nonFlushedChanges) throws HibernateException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Query getNamedSQLQuery(String name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isEventSource() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void afterScrollOperation() {
            throw new UnsupportedOperationException();
        }

        @Deprecated
        @Override
        public void setFetchProfile(String name) {
            throw new UnsupportedOperationException();
        }

        @Deprecated
        @Override
        public String getFetchProfile() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isClosed() {
            throw new UnsupportedOperationException();
        }

        @Override
        public LoadQueryInfluencers getLoadQueryInfluencers() {
            throw new UnsupportedOperationException();
        }

		@Override
		public Connection connection() {
			throw new UnsupportedOperationException();
		}

		@Override
		public JdbcConnectionAccess getJdbcConnectionAccess() {
			throw new UnsupportedOperationException();
		}

		@Override
		public EntityKey generateEntityKey(Serializable id, EntityPersister persister) {
			throw new UnsupportedOperationException();
		}

		@Override
		public CacheKey generateCacheKey(Serializable id, Type type, String entityOrRoleName) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void disableTransactionAutoJoin() {
			throw new UnsupportedOperationException();
		}

		@Override
		public List list(String query, QueryParameters queryParameters) throws HibernateException {
			throw new UnsupportedOperationException();
		}

		@Override
		public Iterator iterate(String query, QueryParameters queryParameters) throws HibernateException {
			throw new UnsupportedOperationException();
		}

		@Override
		public ScrollableResults scroll(String query, QueryParameters queryParameters) throws HibernateException {
			throw new UnsupportedOperationException();
		}

		@Override
		public List listFilter(Object collection, String filter, QueryParameters queryParameters)
				throws HibernateException {
			throw new UnsupportedOperationException();
		}

		@Override
		public Iterator iterateFilter(Object collection, String filter, QueryParameters queryParameters)
				throws HibernateException {
			throw new UnsupportedOperationException();
		}

		@Override
		public List listCustomQuery(CustomQuery customQuery, QueryParameters queryParameters)
				throws HibernateException {
			throw new UnsupportedOperationException();
		}

		@Override
		public ScrollableResults scrollCustomQuery(CustomQuery customQuery, QueryParameters queryParameters)
				throws HibernateException {
			throw new UnsupportedOperationException();
		}

		@Override
		public List list(NativeSQLQuerySpecification spec, QueryParameters queryParameters) throws HibernateException {
			throw new UnsupportedOperationException();
		}

		@Override
		public ScrollableResults scroll(NativeSQLQuerySpecification spec, QueryParameters queryParameters)
				throws HibernateException {
			throw new UnsupportedOperationException();
		}

		@Override
		public int executeUpdate(String query, QueryParameters queryParameters) throws HibernateException {
			throw new UnsupportedOperationException();
		}

		@Override
		public int executeNativeUpdate(NativeSQLQuerySpecification specification, QueryParameters queryParameters)
				throws HibernateException {
			throw new UnsupportedOperationException();
		}

		@Override
		public TransactionCoordinator getTransactionCoordinator() {
			throw new UnsupportedOperationException();
		}

		@Override
		public <T> T execute(Callback<T> callback) {
			throw new UnsupportedOperationException();
		}
	}
}
