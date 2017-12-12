/**
 * Copyright (C) 2009-2017 Simonsoft Nordic AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.simonsoft.cms.publish.config.databinds.profiling;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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
		if (!(o instanceof String) ){
			throw new IllegalArgumentException("The input of PublishProfilingSet.contains(Object o) must be a string");
		}
		return map.containsKey(o);
	}
	@Override
	public Iterator<PublishProfilingRecipe> iterator() {
		return map.values().iterator();
	}
	@Override
	public Object[] toArray() {
		return map.values().toArray();
	}
	@Override
	public <T> T[] toArray(T[] a) {
		throw new UnsupportedOperationException("PublishProfilingSet.toArray(T[] a) is not supported");
	}
	@Override
	public boolean add(PublishProfilingRecipe e) {
		boolean changed = false;
		if (map.containsKey(e.getName())) {
			throw new IllegalArgumentException("Duplicate names in PublishProfilingSet is not allowed.");
		}
		if(!changed) {
			changed = !map.containsValue(e);
		}
		map.put(e.getName(), e);
		return true;
	}
	@Override
	public boolean remove(Object o) {
		throw new UnsupportedOperationException("PublishingProfilingSet.remove() is not supported");
	}
	@Override
	public boolean containsAll(Collection<?> c) {
		throw new UnsupportedOperationException("PublishProfilingSet.containsAll(Collection<?> c) is not supported");
	}
	@Override
	public boolean addAll(Collection<? extends PublishProfilingRecipe> c) {
		throw new UnsupportedOperationException("PublishingProfilingSet.addAll(Collectio<? extends PublishProfilingRecipe> c) is not supported");
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
		throw new UnsupportedOperationException("PublishingProfilingSet.clear() is not supported");
	}
}
