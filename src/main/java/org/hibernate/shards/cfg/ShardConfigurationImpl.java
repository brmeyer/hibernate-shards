package org.hibernate.shards.cfg;

import org.hibernate.engine.config.spi.ConfigurationService;

public class ShardConfigurationImpl
		implements ShardConfiguration {

	private ConfigurationService configuration;

	public ShardConfigurationImpl(ConfigurationService configuration) {
		this.configuration = configuration;
	}

	@Override
	public String getShardUrl() {
		return configuration.getSetting( ShardedAvailableSettings.URL, String.class, null );
	}

	@Override
	public String getShardUser() {
		return configuration.getSetting( ShardedAvailableSettings.USER, String.class, null );
	}

	@Override
	public String getShardPassword() {
		return configuration.getSetting( ShardedAvailableSettings.PASS, String.class, null );
	}

	@Override
	public String getShardSessionFactoryName() {
		return configuration.getSetting( ShardedAvailableSettings.SESSION_FACTORY_NAME, String.class, null );
	}

	@Override
	public Integer getShardId() {
		return configuration.getSetting( ShardedAvailableSettings.SHARD_ID_PROPERTY, Integer.class, null );
	}

	@Override
	public String getShardDatasource() {
		return configuration.getSetting( ShardedAvailableSettings.DATASOURCE, String.class, null );
	}

	@Override
	public String getShardCacheRegionPrefix() {
		return configuration.getSetting( ShardedAvailableSettings.CACHE_REGION_FACTORY, String.class, null );
	}

	@Override
	public String getDriverClassName() {
		return configuration.getSetting( ShardedAvailableSettings.DRIVER, String.class, null );
	}

	@Override
	public String getHibernateDialect() {
		return configuration.getSetting( ShardedAvailableSettings.DIALECT, String.class, null );
	}

	@Override
	public boolean getCheckAllAssociatedObjectInShards() {
		return configuration.getSetting(
				ShardedAvailableSettings.CHECK_ALL_ASSOCIATED_OBJECTS_FOR_DIFFERENT_SHARDS,
				Boolean.class,
				false
		);
	}
}
