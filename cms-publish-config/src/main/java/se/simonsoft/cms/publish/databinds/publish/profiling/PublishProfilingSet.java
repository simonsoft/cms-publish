package se.simonsoft.cms.publish.databinds.publish.profiling;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


public class PublishProfilingSet implements Set<PublishProfilingRecipe>  {

	private List<PublishProfilingRecipe> list = new ArrayList<PublishProfilingRecipe>();






	public PublishProfilingRecipe get(int index) {
		return list.get(index);
	}
	public List<PublishProfilingRecipe> getList() {
		return list;
	}
	public void setList(List<PublishProfilingRecipe> list) {
		this.list = list;
	}
	public PublishProfilingSet(PublishProfilingRecipe...profilingRecipes) {
		this.list = Arrays.asList(profilingRecipes);
	}
	public PublishProfilingSet() {
	}
	@Override
	public int size() {
		return list.size();
	}
	@Override
	public boolean isEmpty() {
		return list.isEmpty();
	}
	@Override
	public boolean contains(Object o) {
		return list.contains(o);
	}
	@Override
	public Iterator<PublishProfilingRecipe> iterator() {
		return list.iterator();
	}
	@Override
	public Object[] toArray() {
		return list.toArray();
	}
	@Override
	public <T> T[] toArray(T[] a) {
		return list.toArray(a);
	}
	@Override
	public boolean add(PublishProfilingRecipe e) {
		return list.add(e);
	}
	@Override
	public boolean remove(Object o) {
		return list.remove(o);
	}
	@Override
	public boolean containsAll(Collection<?> c) {
		return list.containsAll(c);
	}
	@Override
	public boolean addAll(Collection<? extends PublishProfilingRecipe> c) {
		return list.addAll(c);
	}
	@Override
	public boolean retainAll(Collection<?> c) {
		return list.retainAll(c);
	}
	@Override
	public boolean removeAll(Collection<?> c) {
		return list.removeAll(c);
	}
	@Override
	public void clear() {
		list.clear();
	}
}
