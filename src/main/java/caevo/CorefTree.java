package caevo;

import java.util.ArrayList;
import java.util.List;

public class CorefTree {

	public class TimexRef {
		Timex timex;
		
	}
	
	
	public interface ASTree<A>  {
		<X> X accept(Visitor<A,X> v);

		interface Visitor<A,X>  {
			X node(List<ASTree<A>> subNodes);
			X leaf(A value);
		}

	}

	public static class INode<A,B> implements ASTree<A> {

		List<ASTree<A>> subNodes;

		public INode() {
			subNodes = new ArrayList<ASTree<A>>();
		}

		public void addNode(ASTree<A> node) {
			subNodes.add(node);
		}

		@Override
		public <X> X accept(ASTree.Visitor<A, X> v) {
			return v.node(subNodes);
		}

		public static class ILeaf<A> implements ASTree<A> {

			A value;

			public ILeaf(A value) {
				this.value = value;
			}

			@Override
			public <X> X accept(ASTree.Visitor<A, X> v) {
				return v.leaf(value);
			}

		}	

	}
}
