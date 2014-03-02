package org.hibernate.shards.test;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Interceptor;
import org.hibernate.cfg.Configuration;
import org.hibernate.shards.ShardId;
import org.hibernate.shards.ShardedConfiguration;
import org.hibernate.shards.cfg.ConfigurationToShardConfigurationAdapter;
import org.hibernate.shards.cfg.ShardConfiguration;
import org.hibernate.shards.engine.ShardedSessionFactoryImplementor;
import org.hibernate.shards.loadbalance.RoundRobinShardLoadBalancer;
import org.hibernate.shards.session.ShardedSession;
import org.hibernate.shards.strategy.ShardStrategy;
import org.hibernate.shards.strategy.ShardStrategyFactory;
import org.hibernate.shards.strategy.ShardStrategyImpl;
import org.hibernate.shards.strategy.access.SequentialShardAccessStrategy;
import org.hibernate.shards.strategy.access.ShardAccessStrategy;
import org.hibernate.shards.strategy.resolution.AllShardsShardResolutionStrategy;
import org.hibernate.shards.strategy.resolution.ShardResolutionStrategy;
import org.hibernate.shards.strategy.selection.RoundRobinShardSelectionStrategy;
import org.hibernate.shards.strategy.selection.ShardSelectionStrategy;

import org.hibernate.testing.AfterClassOnce;
import org.hibernate.testing.BeforeClassOnce;
import org.hibernate.testing.junit4.BaseUnitTestCase;

/**
 * @author Aleksander Dukhno
 */
public abstract class BaseShardFunctionalTestCase extends BaseUnitTestCase {

	private ShardedSessionFactoryImplementor ssf;
	private ShardedSession session;

	protected ShardedSessionFactoryImplementor shardedSessionFactory() {
		return ssf;
	}

	protected ShardedSession openSession() {
		if ( session == null ) {
			session = ssf.openSession();
		}
		return session;
	}

	protected ShardedSession openSession(Interceptor interceptor) {
		return ssf.openSession( interceptor );
	}

	@BeforeClassOnce
	protected void buildShardedSessionFactory() {
		Configuration prototypeConfig = new Configuration().configure( "hibernate0.cfg.xml" );
		List<ShardConfiguration> shardConfigs = new ArrayList<ShardConfiguration>();
		String[] configurationFiles = getConfigurationFiles();
		for ( int i = 0; i < configurationFiles.length; i++ ) {
			shardConfigs.add( buildShardConfig( configurationFiles[i] ) );
		}
		ShardStrategyFactory shardStrategyFactory = buildShardStrategyFactory();
		ShardedConfiguration shardedConfig = new ShardedConfiguration(
				prototypeConfig,
				shardConfigs,
				shardStrategyFactory
		);
		ssf = (ShardedSessionFactoryImplementor) shardedConfig.buildShardedSessionFactory();

	}

	@AfterClassOnce
	public void endTesting() {
		if ( session != null
				&& session.isOpen() ) {
			session.close();
		}
		session = null;
		if ( ssf != null
				&& !ssf.isClosed() ) {
			ssf.close();
		}
		ssf = null;
	}

	protected ShardStrategyFactory buildShardStrategyFactory() {
		return new ShardStrategyFactory() {
			public ShardStrategy newShardStrategy(List<ShardId> shardIds) {
				RoundRobinShardLoadBalancer loadBalancer = new RoundRobinShardLoadBalancer( shardIds );
				ShardSelectionStrategy pss = new RoundRobinShardSelectionStrategy( loadBalancer );
				ShardResolutionStrategy prs = new AllShardsShardResolutionStrategy( shardIds );
				ShardAccessStrategy pas = new SequentialShardAccessStrategy();
				return new ShardStrategyImpl( pss, prs, pas );
			}
		};
	}

	protected ShardConfiguration buildShardConfig(String configFile) {
		Configuration config = new Configuration().configure( configFile );
		return new ConfigurationToShardConfigurationAdapter( config );
	}

	protected String[] getConfigurationFiles() {
		return new String[] {
				"hibernate0.cfg.xml",
				"hibernate1.cfg.xml",
				"hibernate2.cfg.xml"
		};
	}

}
