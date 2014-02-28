package org.hibernate.shards.cfg;

import org.hibernate.cfg.AvailableSettings;

public interface ShardedAvailableSettings extends AvailableSettings {

	/**
	 * Configuration property that determines whether or not we examine all
	 * associated objects for shard conflicts when we save or update.  A shard
	 * conflict is when we attempt to associate one object that lives on shard X
	 * with an object that lives on shard Y.  Turning this on will hurt
	 * performance but will prevent the programmer from ending up with the
	 * same entity on multiple shards, which is bad (at least in the current version).
	 */
	public static final String CHECK_ALL_ASSOCIATED_OBJECTS_FOR_DIFFERENT_SHARDS = "hibernate.shard.enable_cross_shard_relationship_checks";

	/**
	 * Unique identifier for a shard.  Must be an Integer.
	 */
	public static final String SHARD_ID_PROPERTY = "hibernate.connection.shard_id";
}
