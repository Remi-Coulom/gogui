package net.sf.gogui.gomoku;

import net.sf.gogui.go.Board;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.PointList;

public class CheckAlignment{
	
	public static boolean areFiveAligned(Board b, GoPoint initialPoint) {
		return areNAlignedInAnyDirection(b,initialPoint,5);
	}
	
	public static boolean areAtLeastFiveAligned(Board b, GoPoint initialPoint) {
		return areNAtLeastAlignedInAnyDirection(b,initialPoint,5);
	}

	public static boolean areMoreThanFiveAligned(Board b, GoPoint initialPoint) {
		return areNAtLeastAlignedInAnyDirection(b,initialPoint,6);
	}
	
	public static boolean areNAligned(Board b, GoPoint initialPoint, int n_align) {
		return areNAlignedInAnyDirection(b,initialPoint,n_align);
	}
	
	public static boolean areNAtLeastAligned(Board b, GoPoint initialPoint, int n_minAlign) {
		return areNAtLeastAlignedInAnyDirection(b,initialPoint,n_minAlign);
	}

	protected static boolean areNAtLeastAlignedInAnyDirection(Board b, GoPoint initialPoint, int n_minAlign) {
		Direction dirs = new Direction();
		for (int i = 0; i < 4; i++) {
			int align = n_alignedToDirection(b, initialPoint, dirs.getDirection(i));
			if (align >= n_minAlign)
				return true;
		}
		return false;
	}

	protected static boolean areNAlignedInAnyDirection(Board b, GoPoint initialPoint, int n_align) {
		Direction dirs = new Direction();
		for (int i = 0; i < 4; i++) {
			int align = n_alignedToDirection(b, initialPoint, dirs.getDirection(i));
			if (align == n_align)
				return true;
		}
		return false;
	}

	protected static int n_alignedToDirection(Board b, GoPoint initialPoint, int[] direction) {
		int aligned= getAligned(b, initialPoint, direction).size();
		return (aligned);
	}

	/** 
	 * Recursive method that counts the number of aligned GoPoints of the same color
	 * @param b
	 * @param previousPoint
	 * @param direction
	 * @param occurrence
	 * @return
	 */
	protected static PointList getAligned(Board b, GoPoint firstPoint, int[] direction) {
		int[] opposite = Direction.getOpposite(direction);
		PointList aligned = new PointList();
		aligned.add(firstPoint);
		aligned = getAlignedToDirection(b, firstPoint, direction, aligned);
		aligned = getAlignedToDirection(b, firstPoint, opposite, aligned);
		return aligned;
	}

	/**
	 * The research stops when it faces a border or a point that is not of the same color of the first point.
	 * @param b
	 * @param previousPoint
	 * @param direction
	 * @param aligned
	 * @return
	 */
	protected static PointList getAlignedToDirection( Board b, GoPoint previousPoint, int[] direction, PointList aligned) {
		if (b.getColor(previousPoint).equals(GoColor.EMPTY)) {
			return aligned;
		}
		if (previousPoint.getX() + direction[0] < 0 //if reaches a border
				|| previousPoint.getX() + direction[0] >= b.getSize()
				|| previousPoint.getY() + direction[1] < 0
				|| previousPoint.getY() + direction[1] >= b.getSize()) {
			return aligned;
		}
		GoPoint actualPoint = GoPoint.get(previousPoint.getX()+direction[0], previousPoint.getY()+direction[1]);
		if (! b.getColor(actualPoint).equals(b.getColor(previousPoint))) {
			return aligned;
		}
		aligned.add(actualPoint);
		return getAlignedToDirection(b,actualPoint,direction,aligned);
	}
}