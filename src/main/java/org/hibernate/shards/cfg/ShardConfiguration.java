/**
 * Copyright (C) 2007 Google Inc.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */
package org.hibernate.shards.cfg;

/**
 * Describes the configuration properties that can vary across the {@link org.hibernate.SessionFactory}
 * instances contained within your {@link org.hibernate.shards.session.ShardedSessionFactory}.
 *
 * @author maxr@google.com (Max Ross)
 * @author Aleksander Dukhno
 */
public interface ShardConfiguration {

	/**
	 * @return the url of the shard.
	 *
	 * @see org.hibernate.cfg.Environment#URL
	 */
	String getShardUrl();

	/**
	 * @return the user that will be sent to the shard for authentication
	 *
	 * @see org.hibernate.cfg.Environment#USER
	 */
	String getShardUser();

	/**
	 * @return the password that will be sent to the shard for authentication
	 *
	 * @see org.hibernate.cfg.Environment#PASS
	 */
	String getShardPassword();

	/**
	 * @return the name that the {@link org.hibernate.SessionFactory} created from
	 * this config will have
	 */
	String getShardSessionFactoryName();

	/**
	 * @return unique id of the shard
	 */
	Integer getShardId();

	/**
	 * @return the datasource for the shard
	 *
	 * @see org.hibernate.cfg.Environment#DATASOURCE
	 */
	String getShardDatasource();

	/**
	 * @return the cache region prefix for the shard
	 *
	 * @see org.hibernate.cfg.Environment#CACHE_REGION_PREFIX
	 */
	String getShardCacheRegionPrefix();

	/**
	 * @return the class name of driver
	 *
	 * @see org.hibernate.cfg.Environment#DRIVER
	 */
	String getDriverClassName();

	/**
	 * @return the class name of hibernate dialect for current database
	 *
	 * @see org.hibernate.cfg.Environment#DIALECT
	 */
	String getHibernateDialect();
}
