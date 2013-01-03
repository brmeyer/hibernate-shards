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

package org.hibernate.shards.strategy.exit;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.hibernate.shards.util.Lists;
import org.hibernate.shards.util.StringUtil;

/**
 * @author Maulik Shah
 */
public class ExitOperationUtils {

	public static List<Object> getNonNullList(final List<Object> list) {
		final List<Object> nonNullList = Lists.newArrayList();
		for ( final Object obj : list ) {
			if ( obj != null ) {
				nonNullList.add( obj );
			}
		}
		return nonNullList;
	}

	@SuppressWarnings("unchecked")
	public static List<Comparable<Object>> getComparableList(final List<Object> results) {
		return (List<Comparable<Object>>) (List) results;
	}

	@SuppressWarnings("unchecked")
	public static Comparable<Object> getPropertyValue(final Object obj, final String propertyName) {
		/**
		 * TODO(maulik) respect the client's choice in how Hibernate accesses
		 * property values.  Also need to implement some caching - this gets called
		 * from a Comparator!
		 *
		 * Currently this method access members of an object using getters only,
		 * event of the client has specifed to use direct field access. Ideally,
		 * we could get an EntityPersister from the SessionFactoryImplementor and
		 * use that. However, hibernate's EntityPersister expects all properties
		 * to be a ComponentType. In pratice, these objects are interconnected in
		 * the mapping and Hibernate instantiates them as BagType or ManyToOneType,
		 * i.e. as they are specified in the mappings. Hence, we cannot use
		 * Hibernate's EntityPersister.
		 */
		try {
			final StringBuilder propertyPath = new StringBuilder();
			for ( int i = 0; i < propertyName.length(); i++ ) {
				final String s = propertyName.substring( i, i + 1 );
				if ( i == 0 || propertyName.charAt( i - 1 ) == '.' ) {
					propertyPath.append( StringUtil.capitalize( s ) );
				}
				else {
					propertyPath.append( s );
				}
			}

			final String[] methods = ( "get" + propertyPath.toString().replaceAll( "\\.", ".get" ) ).split( "\\." );
			Object root = obj;
			for ( final String method : methods ) {
				final Method m = findPotentiallyPrivateMethod( root.getClass(), method );
				m.setAccessible( true );
				root = m.invoke( root );
				if ( root == null ) {
					break;
				}
			}
			return (Comparable<Object>) root;
		}
		catch ( NoSuchMethodException e ) {
			throw new RuntimeException( e );
		}
		catch ( IllegalAccessException e ) {
			throw new RuntimeException( e );
		}
		catch ( InvocationTargetException e ) {
			throw new RuntimeException( e );
		}
	}

	@SuppressWarnings("unchecked")
	static Method findPotentiallyPrivateMethod(Class clazz, String methodName) throws NoSuchMethodException {
		try {
			return clazz.getMethod( methodName );
		}
		catch ( NoSuchMethodException nsme ) {
			// that's ok, we'll try the slower approach
		}

		// we need to make sure we can access private methods on subclasses, and
		// the only way to do that is to work our way up the class hierarchy
		while ( clazz != null ) {
			try {
				return clazz.getDeclaredMethod( methodName );
			}
			catch ( NoSuchMethodException e ) {
				clazz = (Class) clazz.getGenericSuperclass();
			}
		}
		throw new NoSuchMethodException( methodName );
	}
}
