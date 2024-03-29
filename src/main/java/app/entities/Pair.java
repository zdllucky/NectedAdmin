package app.entities;

public class Pair<L, R> {

	private final L left;
	private final R right;

	public Pair(L left, R right) {
		this.left = left;
		this.right = right;
	}

	private L getLeft() {
		return left;
	}

	private R getRight() {
		return right;
	}

	@Override
	public int hashCode() {
		return left.hashCode() ^ right.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Pair)) return false;
		Pair pairObject = (Pair) o;
		return this.left.equals(pairObject.getLeft()) &&
				this.right.equals(pairObject.getRight());
	}

	public L getKey() {
		return left;
	}

	public R getValue() {
		return right;
	}
}