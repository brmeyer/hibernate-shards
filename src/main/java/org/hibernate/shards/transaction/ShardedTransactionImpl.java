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

package org.hibernate.shards.transaction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.transaction.Status;
import javax.transaction.Synchronization;

import org.jboss.logging.Logger;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.TransactionException;
import org.hibernate.engine.transaction.spi.AbstractTransactionImpl;
import org.hibernate.engine.transaction.spi.IsolationDelegate;
import org.hibernate.engine.transaction.spi.JoinStatus;
import org.hibernate.engine.transaction.spi.LocalStatus;
import org.hibernate.shards.Shard;
import org.hibernate.shards.ShardedTransaction;
import org.hibernate.shards.engine.ShardedSessionImplementor;
import org.hibernate.shards.session.OpenSessionEvent;
import org.hibernate.shards.session.SetupTransactionOpenSessionEvent;
import org.hibernate.shards.util.Lists;

/**
 * @author Tomislav Nad
 * @author Aleksander Dukhno
 */
public class ShardedTransactionImpl extends AbstractTransactionImpl implements ShardedTransaction {

	private static final Logger LOG = Logger.getLogger( ShardedTransactionImpl.class );

	private final List<Transaction> transactions;

	private List<Synchronization> synchronizations;

	public ShardedTransactionImpl(final ShardedSessionImplementor ssi) {
		super( null );
		final OpenSessionEvent osEvent = new SetupTransactionOpenSessionEvent( this );
		transactions = Collections.synchronizedList( new ArrayList<Transaction>() );
		for ( final Shard shard : ssi.getShards() ) {
			if ( shard.getSession() != null ) {
				transactions.add( shard.getSession().getTransaction() );
			}
			else {
				shard.addOpenSessionEvent( osEvent );
			}
		}
	}

	@Override
	protected void doBegin() {
		boolean beginException = false;
		for ( Transaction t : transactions ) {
			try {
				t.begin();
			}
			catch (HibernateException he) {
				LOG.warn( "exception starting underlying transaction", he );
				beginException = true;
			}
		}
		if ( beginException ) {
			for ( Transaction t : transactions ) {
				if ( t.isActive() ) {
					try {
						t.rollback();
					}
					catch (HibernateException he) {
						// TODO(maxr) What do we do?
					}
				}
			}
			throw new TransactionException( "Begin failed" );
		}
	}

	@Override
	protected void doCommit() {
		for ( Transaction t : transactions ) {
			t.commit();
		}
	}

	@Override
	protected void doRollback() {
		for ( Transaction t : transactions ) {
			if ( t.wasCommitted() ) {
				continue;
			}
			t.rollback();
		}
	}

	@Override
	protected void afterTransactionBegin() {

	}

	@Override
	public void afterTransactionCompletion(final int status) {

	}

	@Override
	protected void beforeTransactionCommit() {
		if ( synchronizations != null
				&& !synchronizations.isEmpty() ) {
			for ( Synchronization sync : synchronizations ) {
				try {
					sync.beforeCompletion();
				}
				catch (RuntimeException e) {
					LOG.warn( "exception calling user Synchronization", e );
				}
			}
		}
	}

	@Override
	protected void beforeTransactionRollBack() {

	}

	@Override
	protected void afterAfterCompletion() {
		if ( synchronizations != null
				&& !synchronizations.isEmpty() ) {
			int status = getLocalStatus() == LocalStatus.FAILED_COMMIT
					? Status.STATUS_UNKNOWN
					: Status.STATUS_COMMITTED;
			for ( Synchronization sync : synchronizations ) {
				try {
					sync.afterCompletion( status );
				}
				catch (RuntimeException e) {
					LOG.warn( "exception calling user Synchronization", e );
				}
			}
		}
	}

	@Override
	public IsolationDelegate createIsolationDelegate() {
		return null;
	}

	@Override
	public JoinStatus getJoinStatus() {
		return null;
	}

	@Override
	public void markRollbackOnly() {

	}

	@Override
	public boolean isActive() {
		return getLocalStatus() == LocalStatus.FAILED_COMMIT
				|| getLocalStatus() == LocalStatus.ACTIVE;
	}

	@Override
	public void setupTransaction(final Session session) {
		LOG.debug( "Setting up transaction" );
		transactions.add( session.getTransaction() );
		if ( getLocalStatus() == LocalStatus.ACTIVE ) {
			session.beginTransaction();
		}
		if ( getTimeout() != -1 ) {
			session.getTransaction().setTimeout( getTimeout() );
		}
	}

	@Override
	public void begin() throws HibernateException {
		//if we start transaction which has already begin we get TransactionException
		//with message "nested transaction is not supported"
		if( getLocalStatus() != LocalStatus.ACTIVE ) {
			super.begin();
		}
	}

	@Override
	public void registerSynchronization(final Synchronization sync) throws HibernateException {
		if ( sync == null ) {
			throw new NullPointerException( "null Synchronization" );
		}
		if ( synchronizations == null ) {
			synchronizations = Lists.newArrayList();
		}
		synchronizations.add( sync );
	}

	@Override
	public void setTimeout(final int seconds) {
		super.setTimeout( seconds );
		for ( final Transaction t : transactions ) {
			t.setTimeout( seconds );
		}
	}

	@Override
	public boolean isInitiator() {
		return false;
	}

	@Override
	public boolean isParticipating() {
		return false;
	}
}
