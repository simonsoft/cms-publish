package se.simonsoft.cms.publish.databinds.publish.profiling;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PublishProfilingSet implements Set<PublishProfilingRecipe>  {

	private Map<String, PublishProfilingRecipe> map = new HashMap<String, PublishProfilingRecipe>();

	public PublishProfilingRecipe get(String index) {
		return map.get(index);
	}
	public Map<String, PublishProfilingRecipe> getMap() {
		return this.map;
	}
	public void setMap(Map<String, PublishProfilingRecipe> list) {
		this.map = list;
	}
	public PublishProfilingSet() {
	}
	@Override
	public int size() {
		return map.size();
	}
	@Override
	public boolean isEmpty() {
		return map.isEmpty();
	}
	@Override
	public boolean contains(Object o) {
		if (o.getClass() != String.class){
			throw new UnsupportedOperationException("The input of PublishProfilingSet.contains(Object o) must be a string");
		}
		return map.containsKey(o);
	}
	@Override
	public Iterator<PublishProfilingRecipe> iterator() {
		Collection<PublishProfilingRecipe> collection = map.values();
		return collection.iterator();
	}
	@Override
	public Object[] toArray() {
		return map.values().toArray();
	}
	@Override
	public <T> T[] toArray(T[] a) {
		for(Object o : a) {
			this.add((PublishProfilingRecipe) o);
		}
		return (T[])((PublishProfilingSet) this.map).toArray();
	}
	@Override
	public boolean add(PublishProfilingRecipe e) {
		boolean changed = false;
		if (map.containsKey(e.getName())) {
			throw new UnsupportedOperationException("Duplicate names in PublishProfilingSet is not allowed.");
		}
		if(!changed) {
			changed = !map.containsValue(e);
		}
		map.put(e.getName(), e);
		return true;
	}
	@Override
	public boolean remove(Object o) {
		boolean changed = false;
		if (o.getClass() != String.class){
			throw new UnsupportedOperationException("The input of PublishProfilingSet.remove(Object) must be a string");
		}
		if (map.containsKey(o)) {
			changed = true;
		}
		map.remove(o);
		return changed;
	}
	@Override
	public boolean containsAll(Collection<?> c) {
		throw new UnsupportedOperationException("PublishProfilingSet.containsAll(Collection<?> c) is not supported");
	}
	@Override
	public boolean addAll(Collection<? extends PublishProfilingRecipe> c) {
		boolean changed = false;

		for (PublishProfilingRecipe e : c) {
			if (!changed) {
				changed = !map.containsValue(e);
			}
			map.put(e.getName(), e);
		}
		return changed;
	}
	@Override
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException("PublishProfilingSet.retainAll(Collection<?> c) is not supported");
	}
	@Override
	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException("PublishProfilingSet.removeAll(Collection<?> c) is not supported");
	}
	@Override
	public void clear() {
		map.clear();
	}
}
