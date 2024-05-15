package edu.iastate.cs228.hw3;

import java.util.AbstractSequentialList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import javax.print.attribute.standard.Fidelity;

/**
 * Implementation of the list interface based on linked nodes that store
 * multiple items per node. Rules for adding and removing elements ensure that
 * each node (except possibly the last one) is at least half full.
 * 
 * @author - Hrishikesha Kyathsandra
 * @version - 1.0.
 */
public class StoutList<E extends Comparable<? super E>> extends AbstractSequentialList<E> {
	/**
	 * Default number of elements that may be stored in each node.
	 */
	private static final int DEFAULT_NODESIZE = 4;

	/**
	 * Number of elements that can be stored in each node.
	 */
	private final int nodeSize;

	/**
	 * Dummy node for head. It should be private but set to public here only for
	 * grading purpose. In practice, you should always make the head of a linked
	 * list a private instance variable.
	 */
	public Node head;

	/**
	 * Dummy node for tail.
	 */
	private Node tail;

	/**
	 * Number of elements in the list.
	 */
	private int size;

	/**
	 * Constructs an empty list with the default node size.
	 */
	public StoutList() {
		this(DEFAULT_NODESIZE);
	}

	/**
	 * Constructs an empty list with the given node size.
	 * 
	 * @param nodeSize number of elements that may be stored in each node, must be
	 *                 an even number
	 */
	public StoutList(int nodeSize) 
	{
		if (nodeSize <= 0 || nodeSize % 2 != 0) {
			throw new IllegalArgumentException();
		}
		// dummy nodes
		head = new Node();
		tail = new Node();
		head.next = tail;
		tail.previous = head;
		this.nodeSize = nodeSize;
	}

	/**
	 * Constructor for grading only. Fully implemented.
	 * 
	 * @param head
	 * @param tail
	 * @param nodeSize
	 * @param size
	 */
	public StoutList(Node head, Node tail, int nodeSize, int size) {
		this.head = head;
		this.tail = tail;
		this.nodeSize = nodeSize;
		this.size = size;
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public boolean add(E item) 
	{
		if (item == null) {
			throw new NullPointerException("Cannot add null items");
		}

		Node newNode = new Node();
		newNode.addItem(item);

		if (size() == 0) { // if the list is empty, we make item its first element in a new node.
			link(head, newNode);
		}

		else if (tail.previous.count == nodeSize) { // if the list is full, create a new node and add the element to it.
			link(tail.previous, newNode);
		}

		else { // if the last node is unempty, just add it to the next available offset.
			tail.previous.addItem(item);
		}

		size++; // book-keeping

		return true;
	}

	@Override
	public void add(int pos, E item) 
	{
		if (item == null) {
			throw new NullPointerException();
		} 
		else if (pos < 0 || pos > size) {
			throw new IndexOutOfBoundsException();
		}
	
		NodeInfo nodeInfo = findNodeInfo(pos);

		Node n = nodeInfo.node;
		int atOffset = nodeInfo.offset;

		int mBy2 = nodeSize / 2;

		if (head.next == tail) { // if the list is empty, create a new node and put item at offset 0.
			add(item);
			return;
		}

		if (atOffset == 0) // if offset = 0 then one of the following two cases occurs,
		{
			if (n.previous != head && n.previous.count < nodeSize) // case 1: f n has a predecessor which has fewer than M elements put X in n’s predecessor.
			{
				n.previous.addItem(item);
				size++;
				return;
			} 
			else if (n == tail && n.previous.count == nodeSize) // case 2: if n is the tail node and n’s predecessor has M elements, create a new node and put item.
			{
				add(item);
				return;
			}
		}

		if (n.count < nodeSize) // otherwise if there is space in node n, put item in node n at offset.
		{
			n.addItem(atOffset, item);
			size++;
		} 
		
		else //otherwise, perform a split operation: move the last M/2 elements of node n into a new successor node n.
		{
			Node newNode = new Node();
			int start = nodeSize - mBy2;

			for (int i = start; i < nodeSize; i++) 
			{
				newNode.data[i - start] = n.data[i];
				n.data[i] = null;
			}

			link(n, newNode);

			n.count = start;
			newNode.count = nodeSize - start;

			if (atOffset <= mBy2) // case 1: if  , put X in node n at offset off
			{
				n.addItem(atOffset, item);
			} 
			else // case 2: if  , put X in node n' at offset
			{
				newNode.addItem((atOffset - mBy2), item);
			}

			size++; // book-keeping.
		}
	}

	@Override
	public E remove(int pos) 
	{
		if (pos < 0 || pos > size) 
		{
			throw new IndexOutOfBoundsException("Invalid position");
		}

		NodeInfo nodeInfo = findNodeInfo(pos);

		Node n = nodeInfo.node;
		int atOffset = nodeInfo.offset;
		E data = n.data[atOffset];

		int mBy2 = nodeSize / 2;
		
		if (n.next == tail && n.count == 1) // if the node n containing X is the last node and has only one element, delete it.
		{ 		
			unlink(n);
		}
		else if (n.next == tail || n.count > mBy2) // otherwise, if n is the last node or if n has more than m/2 elements, remove item from n.
		{
			n.removeItem(atOffset);
		}
		else if (n.next != tail) // otherwise look at its successor node and perform a merge operation.
		{
			n.removeItem(atOffset);
			Node successor = n.next;

			if (successor.count > mBy2) // case 1: if the successor node n' has more than M/2 elements, move the first from n' to n. (mini merge)
			{ 
				n.addItem(successor.data[0]);
				successor.removeItem(0);
			}
			else // case 2: if the successor node n' has m/2 or fewer elements, then move all elements from n' to n and delete n' (full merge)
			{ 
				for (int i = 0; i < successor.count; i++) 
				{
					n.addItem(successor.data[i]);
				}

				unlink(successor);
			}

		}

		size--; // book-keeping

		return data;
	}

	/*
	 * Helper method used to link nodes together
	 * 
	 * @param curr - Node which is going to link a new Node to its right.
	 * @param newNode - New node to be added to the end of the list.
	 */
	private void link(Node current, Node newNode) 
	{
		newNode.previous = current;
		newNode.next = current.next;
		current.next.previous = newNode;
		current.next = newNode;
	}

	/*
	 * Helper method to unlink a node.
	 * 
	 * @param curr - The node to unlink.
	 */
	private void unlink(Node current) 
	{
		current.previous.next = current.next;
		current.next.previous = current.previous;
	}

	/**
	 * Sort all elements in the stout list in the NON-DECREASING order. You may do
	 * the following. Traverse the list and copy its elements into an array,
	 * deleting every visited node along the way. Then, sort the array by calling
	 * the insertionSort() method. (Note that sorting efficiency is not a concern
	 * for this project.) Finally, copy all elements from the array back to the
	 * stout list, creating new nodes for storage. After sorting, all nodes but
	 * (possibly) the last one must be full of elements.
	 * 
	 * Comparator<E> must have been implemented for calling insertionSort().
	 */
	public void sort() {
		
		E[] list = (E[]) new Comparable[size];

		//copy all elements into a new array
		int j = 0;
		Node curr = head.next;
		while (curr != tail) {
			for (int i = 0; i < curr.count; i++) {
				list[j++] = curr.data[i];
			}
			curr = curr.next;
		}

		//destroy
		head.next = tail;
		tail.previous = head;
		size = 0;

		//add sorted elements into new list.
		insertionSort(list, new EComparator());
		for (int i = 0; i < list.length; i++) {
			add(list[i]);
		}
	}

	/**
	 * Sort all elements in the stout list in the NON-INCREASING order. Call the
	 * bubbleSort() method. After sorting, all but (possibly) the last nodes must be
	 * filled with elements.
	 * 
	 * Comparable<? super E> must be implemented for calling bubbleSort().
	 */
	public void sortReverse() 
	{	
		E[] list = (E[]) new Comparable[size];

		//copy all elements into a new array
		int j = 0;
		Node curr = head.next;
		while (curr != tail) 
		{
			for (int i = 0; i < curr.count; i++) 
			{
				list[j++] = curr.data[i];
			}
			curr = curr.next;
		}

		//destroy
		head.next = tail;
		tail.previous = head;
		size = 0;

		//add sorted elements into new list, in reverse order.
		bubbleSort(list);
		for (int i = list.length-1; i>=0; i--) 
		{
			add(list[i]);
		}
	}

	@Override
	public Iterator<E> iterator() 
	{
		return new StoutListIterator();

	}

	@Override
	public ListIterator<E> listIterator() 
	{
		return new StoutListIterator();
	}

	@Override
	public ListIterator<E> listIterator(int index) 
	{
		return new StoutListIterator(index);
	}

	/**
	 * Returns a string representation of this list showing the internal structure
	 * of the nodes.
	 */
	public String toStringInternal() 
	{
		return toStringInternal(null);
	}

	/**
	 * Returns a string representation of this list showing the internal structure
	 * of the nodes and the position of the iterator.
	 *
	 * @param iter an iterator for this list
	 */
	public String toStringInternal(ListIterator<E> iter) 
	{
		int count = 0;
		int position = -1;
		if (iter != null) {
			position = iter.nextIndex();
		}

		StringBuilder sb = new StringBuilder();
		sb.append('[');
		Node current = head.next;
		while (current != tail) {
			sb.append('(');
			E data = current.data[0];
			if (data == null) {
				sb.append("-");
			}

			else {
				if (position == count) {
					sb.append("| ");
					position = -1;
				}
				sb.append(data.toString());
				++count;
			}

			for (int i = 1; i < nodeSize; ++i) {
				sb.append(", ");
				data = current.data[i];
				if (data == null) {
					sb.append("-");
				}

				else {
					if (position == count) {
						sb.append("| ");
						position = -1;
					}
					sb.append(data.toString());
					++count;

					// iterator at end
					if (position == size && count == size) {
						sb.append(" |");
						position = -1;
					}
				}
			}
			sb.append(')');
			current = current.next;
			if (current != tail)
				sb.append(", ");
		}
		sb.append("]");
		return sb.toString();
	}

	/*
	 * Helper class for representing a positions node and offset
	 */
	private class NodeInfo 
	{
		/*
		 * Node object to represent the node of a given positon
		 */
		public Node node;
		
		/*
		 * Offset to hold the value of the offset for a given position.
		 */
		public int offset;

		/*
		 * Constructs a NodeInfo object.
		 * 
		 * param node - Node to be set.
		 * param offset - Offset to be set.
		 */
		public NodeInfo(Node node, int offset) 
		{
			this.node = node;
			this.offset = offset;
		}
	}

	/*
	 * Helper method to detect the node and offset of a given position in the list.
	 * 
	 * @param pos - position in the list of which we must find Node and offset
	 */
	private NodeInfo findNodeInfo(int pos) 
	{	
		Node curr = head.next;
		int index = 0;

		if (pos == size) 
		{
			curr = tail.previous;
			int offset = curr.count;
			return new NodeInfo(curr, offset);
		}

		while (pos >= index) // go through the list to get the node and offset of pos.
		{
			index += curr.count;
			curr = curr.next;

			if (pos < index) 
			{
				curr = curr.previous;
			}
		}

		return new NodeInfo(curr, curr.count - (index - pos));
	}

	/**
	 * Node type for this list. Each node holds a maximum of nodeSize elements in an
	 * array. Empty slots are null.
	 */
	private class Node 
	{
		/**
		 * Array of actual data elements.
		 */
		// Unchecked warning unavoidable.
		public E[] data = (E[]) new Comparable[nodeSize];

		/**
		 * Link to next node.
		 */
		public Node next;

		/**
		 * Link to previous node;
		 */
		public Node previous;

		/**
		 * Index of the next available offset in this node, also equal to the number of
		 * elements in this node.
		 */
		public int count;

		/**
		 * Adds an item to this node at the first available offset. Precondition: count
		 * < nodeSize
		 * 
		 * @param item element to be added
		 */
		void addItem(E item) {
			if (count >= nodeSize) {
				return;
			}
			data[count++] = item;
		}

		/**
		 * Adds an item to this node at the indicated offset, shifting elements to the
		 * right as necessary.
		 * 
		 * Precondition: count < nodeSize
		 * 
		 * @param offset array index at which to put the new element
		 * @param item   element to be added
		 */
		void addItem(int offset, E item) {
			if (count >= nodeSize) {
				return;
			}
			for (int i = count - 1; i >= offset; --i) {
				data[i + 1] = data[i];
			}
			++count;
			data[offset] = item;
		}

		/**
		 * Deletes an element from this node at the indicated offset, shifting elements
		 * left as necessary. Precondition: 0 <= offset < count
		 * 
		 * @param offset
		 */
		void removeItem(int offset) {
			E item = data[offset];
			for (int i = offset + 1; i < nodeSize; ++i) {
				data[i - 1] = data[i];
			}
			data[count - 1] = null;
			--count;
		}
	}

	private class StoutListIterator implements ListIterator<E> {

	   /*
		* Directions for remove and set based on whether the most recent call was to previous or next.
		*/
		private static final int BEHIND = -1;
		private static final int AHEAD = 1;
		private static final int NONE = 0;

		/*
		 * Holds the value for the index of the iterator's cursor.
		 */
		private int index;
		
		/*
		 * Holds the value of the directions for remove and set.
		 */
		private int direction;

		/*
		 * Array to store the elements of the list. Makes list elements easy access.
		 */
		public E[] iterList;

		/**
		 * Default constructor
		 */
		public StoutListIterator() {
			this(0);
		}

		/**
		 * Constructor finds node at a given position.
		 * 
		 * @param pos
		 */
		public StoutListIterator(int pos)
		{
			if (pos < 0 || pos > size) {
				throw new IndexOutOfBoundsException("Invalid position");
			}
			copyEle();
			index = pos;
			direction = NONE;
		}

		/*
		 * Helper method to get elements of the list as and when needed, and keep track. Makes items easy to access, traverse, return or update.
		 */
		private void copyEle() {
			iterList = (E[]) new Comparable[size];
			Node curr = head.next;
			int i = 0;
			while (curr != tail) 
			{
				for (int j = 0; j < curr.count; j++) 
				{
					if(curr.data[j]==null) {
						break;
					}
					else {
						iterList[i++] = curr.data[j];
					}
				}
				curr = curr.next;
			}
		}

		@Override
		public boolean hasNext() {
			return index < size;
		}

		@Override
		public E next() 
		{
			if (!hasNext()) {
				throw new NoSuchElementException();
			}
			direction = BEHIND;
			return iterList[index++];
		}

		@Override
		public void remove() 
		{
			if (direction == NONE) {
				throw new IllegalStateException();
			}
			
			else if (direction == BEHIND) // if next was called before remove, we remove element at index-1.
			{ 
				StoutList.this.remove(index - 1);
				copyEle();
				index--;
			} 
			
			else if (direction == AHEAD) // if previous was called before remove, remove value at index.
			{ 
				StoutList.this.remove(index);
				copyEle();
			}
		
			direction = NONE;
		}
		
		@Override
		public boolean hasPrevious() {
			return index > 0;
		}

		@Override
		public E previous() 
		{
			if (!hasPrevious()) {
				throw new IllegalStateException();
			}
			direction = AHEAD;
			return iterList[--index];
		}

		@Override
		public int nextIndex() {
			return index;
		}

		@Override
		public int previousIndex() {
			return index - 1;
		}

		@Override
		public void set(E e) 
		{
			if (direction == NONE) {
				throw new IllegalStateException();
			} 
			
			else if (direction == BEHIND) // if next was called before set, update value at index-1.
			{ 
				NodeInfo nodeInfo = findNodeInfo(index - 1);
				Node node = nodeInfo.node;
				int atOffset = nodeInfo.offset;
				node.data[atOffset] = e;
				iterList[index - 1] = e; 
			} 
			
			else if (direction == AHEAD) // if previous was called before set, update value at index.
			{ 
				NodeInfo nodeInfo = findNodeInfo(index);
				Node node = nodeInfo.node;
				int atOffset = nodeInfo.offset;
				node.data[atOffset] = e;
				iterList[index] = e;
			}

		}

		@Override
		public void add(E e) 
		{
			if (e == null) {
				throw new NullPointerException();
			}
						
			StoutList.this.add(index, e);
			index++;
			direction = NONE;
			copyEle();
		}
	}

	/**
	 * Sort an array arr[] using the insertion sort algorithm in the NON-DECREASING
	 * order.
	 * 
	 * @param arr  array storing elements from the list
	 * @param comp comparator used in sorting
	 */
	private void insertionSort(E[] arr, Comparator<? super E> comp) 
	{
		for (int i = 1; i < arr.length; i++) {
			E curr = arr[i];
			int j = i - 1;
			while (j > -1 && comp.compare(arr[j], curr) > 0) 
			{
				arr[j + 1] = arr[j];
				j--;
			}
			arr[j + 1] = curr;
		}
	}

	/**
	 * Sort arr[] using the bubble sort algorithm in the NON-INCREASING order. For a
	 * description of bubble sort please refer to Section 6.1 in the project
	 * description. You must use the compareTo() method from an implementation of
	 * the Comparable interface by the class E or ? super E.
	 * 
	 * @param arr array holding elements from the list
	 */
	private void bubbleSort(E[] arr) 
	{
		int n = arr.length;
		for (int i = 0; i < n - 1; i++) 
		{
			for (int j = 0; j < n - i - 1; j++) 
			{
				if (arr[j].compareTo(arr[j + 1]) > 0) 
				{
					E temp = arr[j];
					arr[j] = arr[j + 1];
					arr[j + 1] = temp;
				}
			}
		}
	}

}