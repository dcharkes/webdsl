package utils;

import java.util.Iterator;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.usertype.UserCollectionType;

@SuppressWarnings("unchecked")
public abstract class AbastractOwnedSetType implements UserCollectionType {

	@Override
	public boolean contains(Object collection, Object entity) {
		return ((java.util.Set) collection).contains(entity);
	}

	@Override
	public Iterator getElementsIterator(Object collection) {
		return ((java.util.Set) collection).iterator();
	}

	@Override
	public Object indexOf(Object collection, Object entity) {
		return null;
	}

	@Override
	public abstract Object instantiate(int anticipatedSize);
	// return new x_entclass#x_prop#Set(anticipatedSize < 1 ? 0 : anticipatedSize );

	@Override
	public PersistentCollection instantiate(SessionImplementor session, CollectionPersister persister) throws HibernateException {
		return new utils.PersistentOwnedSet(session);
	}

	@Override
	public Object replaceElements(Object original, Object target,
			CollectionPersister persister, Object owner, Map copyCache,
			SessionImplementor session) throws HibernateException {
		java.util.Set result = (java.util.Set) target;
		result.clear();
		result.addAll((java.util.Set) original);
		return result;
	}

	@Override
	public PersistentCollection wrap(SessionImplementor session, Object collection) {
		if ( session.getEntityMode() == org.hibernate.EntityMode.DOM4J ) {
			throw new IllegalStateException("dom4j not supported");
		}
		else {
			return new utils.PersistentOwnedSet( session, (java.util.Set) collection );
		}
	}

	public abstract boolean isAffectedBy(String filter);

	public abstract boolean isFilterCompatible(org.hibernate.impl.FilterImpl oldFilter, org.hibernate.impl.FilterImpl newFilter);

}