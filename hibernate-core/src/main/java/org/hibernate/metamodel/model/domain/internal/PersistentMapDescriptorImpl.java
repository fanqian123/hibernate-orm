/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;

import org.hibernate.LockMode;
import org.hibernate.MappingException;
import org.hibernate.cache.CacheException;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.IndexedCollection;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.internal.collection.SqlAstHelper;
import org.hibernate.metamodel.model.domain.spi.AbstractPersistentCollectionDescriptor;
import org.hibernate.metamodel.model.domain.spi.AbstractPluralPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.CollectionElement;
import org.hibernate.metamodel.model.domain.spi.CollectionIndex;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeDescriptor;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.internal.domain.collection.CollectionInitializerProducer;
import org.hibernate.sql.results.internal.domain.collection.MapInitializerProducer;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.FetchParent;

/**
 * @author Steve Ebersole
 */
public class PersistentMapDescriptorImpl<O,K,E>
		extends AbstractPersistentCollectionDescriptor<O,Map<K,E>,E> {
	private final Comparator<K> comparator;
	private final boolean hasFormula;

	@SuppressWarnings("unchecked")
	public PersistentMapDescriptorImpl(
			Property pluralProperty,
			ManagedTypeDescriptor runtimeContainer,
			RuntimeModelCreationContext creationContext) throws MappingException, CacheException {
		super( pluralProperty, runtimeContainer, creationContext );
		IndexedCollection collection = (IndexedCollection) pluralProperty.getValue();
		hasFormula = collection.getIndex().hasFormula();

		if ( pluralProperty.getValue() instanceof Collection ) {
			this.comparator = collection.getComparator();
		}
		else {
			this.comparator = null;
		}
	}

	@Override
	public Comparator<?> getSortingComparator() {
		return comparator;
	}


	@Override
	public boolean contains(Object collection, Object childObject) {
		// todo (6.0) : do we need to check key as well?
		// todo (6.0) : or perhaps make distinction between #containsValue and #containsKey/Index?
		return ( (Map) collection ).containsValue( childObject );
	}

	@Override
	protected CollectionInitializerProducer createInitializerProducer(
			NavigablePath navigablePath,
			FetchParent fetchParent,
			boolean selected,
			String resultVariable,
			LockMode lockMode,
			DomainResultCreationState creationState) {
		return new MapInitializerProducer(
				this,
				selected,
				// map-key
				SqlAstHelper.generateCollectionIndexDomainResult(
						navigablePath.append( CollectionIndex.NAVIGABLE_NAME ),
						getIndexDescriptor(),
						selected,
						null,
						creationState
				),
				// map-value
				SqlAstHelper.generateCollectionElementDomainResult(
						navigablePath.append( CollectionElement.NAVIGABLE_NAME ),
						getElementDescriptor(),
						selected,
						null,
						creationState
				)
		);
	}

	@Override
	protected AbstractPluralPersistentAttribute createAttribute(
			Property pluralProperty,
			PropertyAccess propertyAccess,
			RuntimeModelCreationContext creationContext) {
		return new MapAttributeImpl<>( this, pluralProperty, propertyAccess, creationContext );
	}

	@Override
	protected void doProcessQueuedOps(
			PersistentCollection collection, Object id, SharedSessionContractImplementor session) {
//		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	protected boolean hasIndex() {
		return true;
	}

	@Override
	protected boolean indexContainsFormula(){
		return hasFormula;
	}

	@Override
	public Iterator getElementsIterator(Object collection, SharedSessionContractImplementor session) {
		return ( (java.util.Map) collection ).values().iterator();
	}

	@Override
	public Object indexOf(Object collection, Object element) {
		for ( Object o : ( (Map) collection ).entrySet() ) {
			Map.Entry me = (Map.Entry) o;
			//TODO: proxies!
			if ( me.getValue() == element ) {
				return me.getKey();
			}
		}
		return null;
	}
}
