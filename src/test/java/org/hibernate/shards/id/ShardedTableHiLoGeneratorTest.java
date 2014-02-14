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

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.shards.PermutationHelper;
import org.hibernate.shards.integration.BaseShardingIntegrationTestCase;
import org.hibernate.shards.integration.Permutation;
import org.hibernate.shards.session.ControlSessionProvider;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.Test;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertEquals;

/**
 * @author maxr@google.com (Max Ross)
 */
@RunWith(Parameterized.class)
//todo BaseShardingIntegrationTestCase temporary
public class ShardedTableHiLoGeneratorTest extends BaseShardingIntegrationTestCase {

	public ShardedTableHiLoGeneratorTest( Permutation permutation) {
		super(permutation);
	}

	@Test
	public void testGenerate() {
		final SessionImplementor controlSessionToReturn = openHibernateSession();
		ControlSessionProvider provider = new ControlSessionProvider() {
			public SessionImplementor openControlSession() {
				return controlSessionToReturn;
			}
		};
		final SessionImplementor session = openHibernateSession();
		ShardedTableHiLoGenerator gen = new ShardedTableHiLoGenerator() {
			@Override
			Serializable superGenerate(SessionImplementor controlSession, Object obj) {
				assertSame( controlSessionToReturn, controlSession );
				return 33;
			}
		};
		gen.setControlSessionProvider( provider );
		assertEquals( 33, gen.generate( session, null ) );
	}

	//todo temporary
	@Parameterized.Parameters()
	public static Iterable<Object[]> data() {
		return PermutationHelper.data();
	}
}
