package org.hibernate.shards.test;

import java.io.InputStream;
import java.sql.Blob;
import java.sql.Clob;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Interceptor;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.SessionFactoryBuilder;
import org.hibernate.metamodel.binding.AbstractPluralAttributeBinding;
import org.hibernate.metamodel.binding.AttributeBinding;
import org.hibernate.metamodel.binding.Caching;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.source.MetadataImplementor;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.shards.ShardId;
import org.hibernate.shards.ShardedConfiguration;
import org.hibernate.shards.cfg.ShardConfiguration;
import org.hibernate.shards.cfg.ShardConfigurationImpl;
import org.hibernate.shards.engine.ShardedSessionFactoryImplementor;
import org.hibernate.shards.loadbalance.RoundRobinShardLoadBalancer;
import org.hibernate.shards.session.ShardedSession;
import org.hibernate.shards.session.ShardedSessionFactoryImpl;
import org.hibernate.shards.strategy.ShardStrategy;
import org.hibernate.shards.strategy.ShardStrategyFactory;
import org.hibernate.shards.strategy.ShardStrategyImpl;
import org.hibernate.shards.strategy.access.SequentialShardAccessStrategy;
import org.hibernate.shards.strategy.access.ShardAccessStrategy;
import org.hibernate.shards.strategy.resolution.AllShardsShardResolutionStrategy;
import org.hibernate.shards.strategy.resolution.ShardResolutionStrategy;
import org.hibernate.shards.strategy.selection.RoundRobinShardSelectionStrategy;
import org.hibernate.shards.strategy.selection.ShardSelectionStrategy;
import org.hibernate.type.Type;

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
		StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder();
		String[] configurationFiles = getConfigurationFiles();
		List<ShardConfiguration> shardConfigurations = new ArrayList<ShardConfiguration>();
		Map<SessionFactoryImplementor, Set<ShardId>> sessionFactories = new HashMap<SessionFactoryImplementor, Set<ShardId>>();
		for( int i = 0; i < configurationFiles.length; i++ ) {
			StandardServiceRegistry registry = builder.configure( configurationFiles[ i ] ).build();
			MetadataImplementor metadata = buildMetadata( registry );
			afterConstructAndConfigureMetadata( metadata );
			final SessionFactoryBuilder sessionFactoryBuilder = metadata.getSessionFactoryBuilder();
			SessionFactory sessionFactory = sessionFactoryBuilder.build();
			afterSessionFactoryBuilt();
			ShardConfiguration configuration = new ShardConfigurationImpl( registry.getService( ConfigurationService.class ) );
			shardConfigurations.add( configuration );
			sessionFactories.put( sessionFactory,  )
		}
		ssf = new ShardedSessionFactoryImpl(
				shardConfigurations,
				buildShardStrategyFactory(),
				ShardedConfiguration.determineClassesWithoutTopLevelSaveSupport(
						new Configuration().configure("hibernate0.cfg.xml") ),

				);

	}

	@AfterClassOnce
	public void endTesting() {
		if ( session != null
				&& session.isOpen() ) {
			session.close();
			session = null;
		}
		if ( ssf != null
				&& !ssf.isClosed() ) {
			ssf.close();
			ssf = null;
		}
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

	protected void afterSessionFactoryBuilt() {
	}

	private MetadataImplementor buildMetadata( StandardServiceRegistry serviceRegistry ) {
		assert BootstrapServiceRegistry.class.isInstance( serviceRegistry.getParentServiceRegistry() );
		MetadataSources sources = new MetadataSources( serviceRegistry.getParentServiceRegistry() );
		addMappings( sources );
		return (MetadataImplementor) sources.getMetadataBuilder( serviceRegistry ).build();
	}


	protected void afterConstructAndConfigureMetadata(MetadataImplementor metadataImplementor) {
		applyCacheSettings( metadataImplementor );
	}

	public void applyCacheSettings(MetadataImplementor metadataImplementor) {
		if ( StringHelper.isEmpty( getCacheConcurrencyStrategy() ) ) {
			return;
		}

		for ( EntityBinding entityBinding : metadataImplementor.getEntityBindings() ) {
			boolean hasLob = false;
			for ( AttributeBinding attributeBinding : entityBinding.getAttributeBindingClosure() ) {
				if ( attributeBinding.getAttribute().isSingular() ) {
					Type type = attributeBinding.getHibernateTypeDescriptor().getResolvedTypeMapping();
					String typeName = type.getName();
					if ( "blob".equals( typeName ) || "clob".equals( typeName ) ) {
						hasLob = true;
					}
					if ( Blob.class.getName().equals( typeName ) || Clob.class.getName().equals( typeName ) ) {
						hasLob = true;
					}
				}
			}
			if ( !hasLob && entityBinding.getSuperEntityBinding() == null && overrideCacheStrategy() ) {
				Caching caching = entityBinding.getHierarchyDetails().getCaching();
				if ( caching == null ) {
					caching = new Caching();
				}
				caching.setRegion( entityBinding.getEntity().getName() );
				caching.setCacheLazyProperties( true );
				caching.setAccessType( AccessType.fromExternalName( getCacheConcurrencyStrategy() ) );
				entityBinding.getHierarchyDetails().setCaching( caching );
			}
			for ( AttributeBinding attributeBinding : entityBinding.getAttributeBindingClosure() ) {
				if ( !attributeBinding.getAttribute().isSingular() ) {
					AbstractPluralAttributeBinding binding = AbstractPluralAttributeBinding.class.cast( attributeBinding );
					Caching caching = binding.getCaching();
					if ( caching == null ) {
						caching = new Caching();
					}
					caching.setRegion(
							StringHelper.qualify(
									entityBinding.getEntity().getName(),
									attributeBinding.getAttribute().getName()
							)
					);
					caching.setCacheLazyProperties( true );
					caching.setAccessType( AccessType.fromExternalName( getCacheConcurrencyStrategy() ) );
					binding.setCaching( caching );
				}
			}
		}
	}

	protected String[] getConfigurationFiles() {
		return new String[] {
				"hibernate0.cfg.xml",
				"hibernate1.cfg.xml",
				"hibernate2.cfg.xml"
		};
	}

	protected boolean overrideCacheStrategy() {
		return true;
	}

	protected String getCacheConcurrencyStrategy() {
		return null;
	}

	protected void addMappings(MetadataSources sources) {
		String[] mappings = getMappings();
		if ( mappings != null ) {
			for ( String mapping : mappings ) {
				sources.addResource(
						getBaseForMappings() + mapping
				);
			}
		}
		Class<?>[] annotatedClasses = getAnnotatedClasses();
		if ( annotatedClasses != null ) {
			for ( Class<?> annotatedClass : annotatedClasses ) {
				sources.addAnnotatedClass( annotatedClass );
			}
		}
		String[] annotatedPackages = getAnnotatedPackages();
		if ( annotatedPackages != null ) {
			for ( String annotatedPackage : annotatedPackages ) {
				sources.addPackage( annotatedPackage );
			}
		}
		String[] xmlFiles = getOrmXmlFiles();
		if ( xmlFiles != null ) {
			for ( String xmlFile : xmlFiles ) {
				InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream( xmlFile );
				sources.addInputStream( is );
			}
		}
	}

	protected static final String[] NO_MAPPINGS = new String[0];

	protected String[] getMappings() {
		return NO_MAPPINGS;
	}

	protected static final Class<?>[] NO_CLASSES = new Class[0];

	protected Class<?>[] getAnnotatedClasses() {
		return NO_CLASSES;
	}

	protected String[] getAnnotatedPackages() {
		return NO_MAPPINGS;
	}

	protected String getBaseForMappings() {
		return "org/hibernate/shards/test/";
	}

	protected String[] getOrmXmlFiles() {
		return NO_MAPPINGS;
	}

}
