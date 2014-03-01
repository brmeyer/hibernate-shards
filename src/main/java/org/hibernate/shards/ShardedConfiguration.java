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

package org.hibernate.shards;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.jboss.logging.Logger;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.Metadata;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.source.MetadataImplementor;
import org.hibernate.shards.cfg.ShardConfiguration;
import org.hibernate.shards.cfg.ShardedAvailableSettings;
import org.hibernate.shards.cfg.ShardedEnvironment;
import org.hibernate.shards.session.ShardedSessionFactory;
import org.hibernate.shards.session.ShardedSessionFactoryImpl;
import org.hibernate.shards.strategy.ShardStrategyFactory;
import org.hibernate.shards.util.Preconditions;

/**
 * Like regular Hibernate's Configuration, this class helps construct your
 * factories. Not extending Hibernate's Configuration because that is the one place
 * where the notion of a single database is specified (i.e. in the
 * hibernate.properties file). While we would like to maintain the Hibernate paradigm
 * as much as possible, this is one place it might be different.
 *
 * @author Maulik Shah
 */
public class ShardedConfiguration {

	// the prototype config that we'll use when constructing the shard-specific
	// configs
	private final Configuration prototypeConfiguration;

	// shard-specific configs
	private final List<ShardConfiguration> shardConfigs;

	// user-defined sharding behavior
	private final ShardStrategyFactory shardStrategyFactory;

	// maps virtual shard ids to physical shard ids
	private final Map<Integer, Integer> virtualShardToShardMap;

	// maps physical shard ids to sets of virtual shard ids
	private final Map<Integer, Set<ShardId>> shardToVirtualShardIdMap;

	private final List<StandardServiceRegistry> serviceRegistries;
	private List<MetadataSources> metadataSourceses;
	private final Map<StandardServiceRegistry, MetadataImplementor> metadatas;

	// our lovely logger
	private static final Logger LOG = Logger.getLogger( ShardedConfiguration.class );

	public ShardedConfiguration(ShardStrategyFactory strategyFactory, List<StandardServiceRegistry> serviceRegistries) {
		if ( strategyFactory == null ) {
			String message = "Strategy factory can't be NULL";
			LOG.info( message );
			throw new IllegalArgumentException( message );
		}
		if ( serviceRegistries == null
				|| serviceRegistries.isEmpty() ) {
			String message = "Service registries can't be NULL or empty";
			LOG.info( message );
			throw new IllegalArgumentException( message );
		}

		//~~~~~~~~~~~~~~~~~~~~~~~~~
		this.shardConfigs = Collections.emptyList();
		this.prototypeConfiguration = new Configuration();
		//~~~~~~~~~~~~~~~~~~~~~~~~~

		this.shardStrategyFactory = strategyFactory;
		this.shardToVirtualShardIdMap = Collections.emptyMap();
		this.virtualShardToShardMap = Collections.emptyMap();
		this.serviceRegistries = serviceRegistries;
		this.metadatas = new HashMap<StandardServiceRegistry, MetadataImplementor>( serviceRegistries.size() );
		this.metadataSourceses = new ArrayList<MetadataSources>( serviceRegistries.size() );
		for ( int i = 0; i < serviceRegistries.size(); i++ ) {
			StandardServiceRegistry serviceRegistry = serviceRegistries.get( i );
			MetadataSources sources = new MetadataSources( serviceRegistry );
			metadataSourceses.add( sources );
			MetadataImplementor metadataImplementor = (MetadataImplementor) sources.buildMetadata();
			this.metadatas.put( serviceRegistry, metadataImplementor );
		}
	}

	public Collection<MetadataImplementor> shardsMetadata() {
		return Collections.unmodifiableCollection( metadatas.values() );
	}

	public List<MetadataSources> shardsMetadataSources() {
		return Collections.unmodifiableList( metadataSourceses );
	}

	//TODO temporary for testing ?
	public void applyMappings(List<MetadataSources> metadataSourceses ) {
		for( MetadataSources metadataSources : metadataSourceses ) {
			StandardServiceRegistry serviceRegistry = (StandardServiceRegistry)metadataSources.getServiceRegistry();
			this.metadatas.put( serviceRegistry, (MetadataImplementor)metadataSources.buildMetadata() );
		}
		this.metadataSourceses = metadataSourceses;
	}

	public ShardedSessionFactory buildFactory() {
		final Map<SessionFactoryImplementor, Set<ShardId>> sessionFactories = new HashMap<SessionFactoryImplementor, Set<ShardId>>();
		// since all configs get their mappings from the prototype config, and we
		// get the set of classes that don't support top-level saves from the mappings,
		// we can get the set from the prototype and then just reuse it.
		final Set<Class<?>> classesWithoutTopLevelSaveSupport =
				determineClassesWithoutTopLevelSaveSupport( prototypeConfiguration );

		boolean doFullCrossShardRelationshipChecking = true;
		for ( StandardServiceRegistry serviceRegistry : serviceRegistries ) {
			ConfigurationService configurationService = serviceRegistry.getService( ConfigurationService.class );

			doFullCrossShardRelationshipChecking &= configurationService.getSetting(
					ShardedAvailableSettings.CHECK_ALL_ASSOCIATED_OBJECTS_FOR_DIFFERENT_SHARDS,
					Boolean.class,
					false
			);
			final Integer shardID = configurationService.getSetting(
					ShardedAvailableSettings.SHARD_ID_PROPERTY,
					new ConfigurationService.Converter<Integer>() {
						@Override
						public Integer convert(Object value) {
							if ( value == null ) {
								final String msg = "Attempt to build a ShardedSessionFactory using a "
										+ "ShardConfiguration that has a null shard id.";
								LOG.error( msg );
								throw new NullPointerException( msg );
							}
							return Integer.parseInt( value.toString() );
						}
					},
					null
			);
			Set<ShardId> virtualShardIds = Collections.singleton( new ShardId( shardID ) );
			Metadata metadata = metadatas.get( serviceRegistry );
			sessionFactories.put( (SessionFactoryImplementor) metadata.buildSessionFactory(), virtualShardIds );
		}

		return new ShardedSessionFactoryImpl(
				sessionFactories,
				shardStrategyFactory,
				classesWithoutTopLevelSaveSupport,
				doFullCrossShardRelationshipChecking
		);
	}

	/**
	 * Constructs a ShardedConfiguration.
	 *
	 * @param prototypeConfiguration The prototype for all shardConfigs that
	 * will be used to create the {@link SessionFactory} objects
	 * that are internal to the {@link ShardedSessionFactory}.
	 * Every {@link org.hibernate.SessionFactory} within the
	 * {@link org.hibernate.shards.session.ShardedSessionFactory} objects created by the
	 * {@link ShardedConfiguration} will look the same, except for properties that we
	 * consider to be "variable" (they can vary from shard to shard).
	 * The variable properties are defined by the {@link ShardedConfiguration} interface.
	 * @param shardConfigs Shard-specific configuration data for each shard.
	 * @param shardStrategyFactory factory that knows how to create the right type of shard strategy
	 */
	@Deprecated
	public ShardedConfiguration(
			final Configuration prototypeConfiguration,
			final List<ShardConfiguration> shardConfigs,
			final ShardStrategyFactory shardStrategyFactory) {

		this( prototypeConfiguration, shardConfigs, shardStrategyFactory, Collections.<Integer, Integer>emptyMap() );
	}

	/**
	 * Constructs a ShardedConfiguration.
	 *
	 * @param prototypeConfiguration The prototype for all shardConfigs that
	 * will be used to create the {@link SessionFactory} objects
	 * that are internal to the {@link ShardedSessionFactory}.
	 * Every {@link org.hibernate.SessionFactory} within the
	 * {@link org.hibernate.shards.session.ShardedSessionFactory} objects created by the
	 * {@link ShardedConfiguration} will look the same, except for properties that we
	 * consider to be "variable" (they can vary from shard to shard).
	 * The variable properties are defined by the {@link ShardedConfiguration} interface.
	 * @param shardConfigs Shard-specific configuration data for each shard.
	 * @param shardStrategyFactory factory that knows how to create the right kind of shard strategy
	 * @param virtualShardToShardMap A map that maps virtual shard ids to real
	 */
	@Deprecated
	public ShardedConfiguration(
			final Configuration prototypeConfiguration,
			final List<ShardConfiguration> shardConfigs,
			final ShardStrategyFactory shardStrategyFactory,
			final Map<Integer, Integer> virtualShardToShardMap) {

		if ( prototypeConfiguration == null ) {
			String message = "Prototype configuration can't be null";
			LOG.error( message );
			throw new IllegalArgumentException( message );
		}
		if ( shardConfigs == null
				|| shardConfigs.isEmpty() ) {
			String message = "Shard configuration can't be null or empty";
			LOG.error( message );
			throw new IllegalArgumentException( message );
		}
		if ( shardStrategyFactory == null ) {
			String message = "Shard strategy factory can't be null";
			LOG.error( message );
			throw new IllegalArgumentException( message );
		}
		if ( virtualShardToShardMap == null ) {
			String message = "Shard and virtual shard mapping is null";
			LOG.error( message );
			throw new IllegalArgumentException( message );
		}
		//~~~~~~~~~ Just for back porting ~~~~~~~~~~
		this.serviceRegistries = Collections.emptyList();
		this.metadatas = Collections.emptyMap();
		this.metadataSourceses = Collections.emptyList();
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		this.prototypeConfiguration = prototypeConfiguration;
		this.shardConfigs = shardConfigs;
		this.shardStrategyFactory = shardStrategyFactory;
		this.virtualShardToShardMap = Preconditions.checkNotNull( virtualShardToShardMap );

		if ( !virtualShardToShardMap.isEmpty() ) {
			// build the map from shard to set of virtual shards
			shardToVirtualShardIdMap = new HashMap<Integer, Set<ShardId>>();
			for ( Map.Entry<Integer, Integer> entry : virtualShardToShardMap.entrySet() ) {
				Set<ShardId> set = shardToVirtualShardIdMap.get( entry.getValue() );
				// see if we already have a set of virtual shards
				if ( set == null ) {
					// we don't, so create it and add it to the map
					set = new HashSet<ShardId>();
					shardToVirtualShardIdMap.put( entry.getValue(), set );
				}
				set.add( new ShardId( entry.getKey() ) );
			}
		}
		else {
			shardToVirtualShardIdMap = Collections.emptyMap();
		}

		// Initializes the mapping configuration.
		this.prototypeConfiguration.buildMappings();
	}

	/**
	 * @return A ShardedSessionFactory built from the prototype config and
	 * the shard-specific configs passed into the constructor.
	 */
	@Deprecated
	public ShardedSessionFactory buildShardedSessionFactory() {
		final Map<SessionFactoryImplementor, Set<ShardId>> sessionFactories = new HashMap<SessionFactoryImplementor, Set<ShardId>>();
		// since all configs get their mappings from the prototype config, and we
		// get the set of classes that don't support top-level saves from the mappings,
		// we can get the set from the prototype and then just reuse it.
		final Set<Class<?>> classesWithoutTopLevelSaveSupport =
				determineClassesWithoutTopLevelSaveSupport( prototypeConfiguration );

		for ( final ShardConfiguration config : shardConfigs ) {
			populatePrototypeWithVariableProperties( config );
			// get the shardId from the shard-specific config
			final Integer shardId = config.getShardId();
			if ( shardId == null ) {
				final String msg = "Attempt to build a ShardedSessionFactory using a "
						+ "ShardConfiguration that has a null shard id.";
				LOG.error( msg );
				throw new NullPointerException( msg );
			}
			Set<ShardId> virtualShardIds;
			if ( virtualShardToShardMap.isEmpty() ) {
				// simple case, virtual and physical are the same
				virtualShardIds = Collections.singleton( new ShardId( shardId ) );
			}
			else {
				// get the set of shard ids that are mapped to the physical shard
				// described by this config
				virtualShardIds = shardToVirtualShardIdMap.get( shardId );
			}
			sessionFactories.put( buildSessionFactory(), virtualShardIds );
		}

		final boolean doFullCrossShardRelationshipChecking =
				ConfigurationHelper.getBoolean(
						ShardedEnvironment.CHECK_ALL_ASSOCIATED_OBJECTS_FOR_DIFFERENT_SHARDS,
						prototypeConfiguration.getProperties(),
						true
				);

		return new ShardedSessionFactoryImpl(
				sessionFactories,
				shardStrategyFactory,
				classesWithoutTopLevelSaveSupport,
				doFullCrossShardRelationshipChecking
		);
	}

	/**
	 * @return the Set of mapped classes that don't support top level saves
	 */
	@SuppressWarnings("unchecked")
	static Set<Class<?>> determineClassesWithoutTopLevelSaveSupport(final Configuration prototypeConfig) {
		final Set<Class<?>> classesWithoutTopLevelSaveSupport = new HashSet<Class<?>>();
		for ( final Iterator<PersistentClass> pcIter = prototypeConfig.getClassMappings(); pcIter.hasNext(); ) {
			final PersistentClass pc = pcIter.next();
			for ( final Iterator<Property> propIter = pc.getPropertyIterator(); propIter.hasNext(); ) {
				if ( doesNotSupportTopLevelSave( propIter.next() ) ) {
					final Class<?> mappedClass = pc.getMappedClass();
					LOG.info( String.format( "Class %s does not support top-level saves.", mappedClass.getName() ) );
					classesWithoutTopLevelSaveSupport.add( mappedClass );
					break;
				}
			}
		}
		return classesWithoutTopLevelSaveSupport;
	}

	/**
	 * there may be other scenarios, but mappings that contain one-to-one mappings
	 * definitely can't be saved as top-level objects (not part of a cascade and
	 * no properties from which the shard can be inferred)
	 */
	static boolean doesNotSupportTopLevelSave(final Property property) {
		return property.getValue() != null &&
				OneToOne.class.isAssignableFrom( property.getValue().getClass() );
	}

	/**
	 * Takes the values of the properties exposed by the ShardConfiguration
	 * interface and sets them as the values of the corresponding properties
	 * in the prototype config.
	 */
	@Deprecated
	void populatePrototypeWithVariableProperties(final ShardConfiguration config) {
		safeSet( prototypeConfiguration, Environment.USER, config.getShardUser() );
		safeSet( prototypeConfiguration, Environment.PASS, config.getShardPassword() );
		safeSet( prototypeConfiguration, Environment.URL, config.getShardUrl() );
		safeSet( prototypeConfiguration, Environment.DATASOURCE, config.getShardDatasource() );
		safeSet( prototypeConfiguration, Environment.CACHE_REGION_PREFIX, config.getShardCacheRegionPrefix() );
		safeSet( prototypeConfiguration, Environment.SESSION_FACTORY_NAME, config.getShardSessionFactoryName() );
		safeSet( prototypeConfiguration, Environment.DRIVER, config.getDriverClassName() );
		safeSet( prototypeConfiguration, Environment.DIALECT, config.getHibernateDialect() );
		safeSet( prototypeConfiguration, ShardedEnvironment.SHARD_ID_PROPERTY, config.getShardId().toString() );
	}

	/**
	 * Set the key to the given value on the given config, but only if the
	 * value is not null.
	 */
	@Deprecated
	static void safeSet(final Configuration config, final String key, final String value) {
		if ( value != null ) {
			config.setProperty( key, value );
		}
	}

	/**
	 * Helper function that creates an actual SessionFactory.
	 */
	@Deprecated
	private SessionFactoryImplementor buildSessionFactory() {
		return (SessionFactoryImplementor) prototypeConfiguration.buildSessionFactory();
	}
}
