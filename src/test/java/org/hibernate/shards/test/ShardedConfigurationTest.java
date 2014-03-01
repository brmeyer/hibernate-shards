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
package org.hibernate.shards.test;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.shards.engine.ShardedSessionFactoryImplementor;

import org.junit.Test;
import org.junit.Assert;

/**
 * @author Aleksander Dukhno
 */
public class ShardedConfigurationTest extends BaseShardFunctionalTestCase {

	@Test
	public void testBuildShardedSessionFactory() {
		ShardedSessionFactoryImplementor ssf = shardedSessionFactory();
		Assert.assertNotNull( ssf );
		// make sure the session factory contained in the sharded session factory
		// has the number of session factories we expect
		List<SessionFactory> sfList = ssf.getSessionFactories();
		Assert.assertEquals( 3, sfList.size() );
	}

	@Test
	public void simpleTest() {
		Session s = openSession();
		s.beginTransaction();
		TestEntity entity = new TestEntity();
		entity.setID( 1L );
		entity.setDescription( "Description" );
		entity.setName( "Name" );
		s.save( entity );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		entity = (TestEntity) s.get( TestEntity.class, 1L );
		Assert.assertNotNull( entity );
		Assert.assertEquals( 1L, entity.getID() );
		Assert.assertEquals( "Description", entity.getDescription() );
		Assert.assertEquals( "Name", entity.getName() );
		s.getTransaction().commit();
	}

	public Class[] getAnnotatedClasses() {
		return new Class[] {TestEntity.class};
	}
}
