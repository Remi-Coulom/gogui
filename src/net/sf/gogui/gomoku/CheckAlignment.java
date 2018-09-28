package net.sf.gogui.gomoku;

import net.sf.gogui.go.Board;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.Move;
import net.sf.gogui.go.PointList;

public class CheckAlignment{
	
	public static boolean areFiveAligned(Board b, Move initialPoint) {
		return areNAlignedInAnyDirection(b,initialPoint,5);
	}
	
	public static boolean areAtLeastFiveAligned(Board b, Move initialPoint) {
		return areNAtLeastAlignedInAnyDirection(b,initialPoint,5);
	}

	public static boolean areMoreThanFiveAligned(Board b, Move initialPoint) {
		return areNAtLeastAlignedInAnyDirection(b,initialPoint,6);
	}
	
	public static boolean areNAligned(Board b, Move initialPoint, int n_align) {
		return areNAlignedInAnyDirection(b,initialPoint,n_align);
	}
	
	public static boolean areNAtLeastAligned(Board b, Move initialPoint, int n_minAlign) {
		return areNAtLeastAlignedInAnyDirection(b,initialPoint,n_minAlign);
	}

	private static boolean areNAtLeastAlignedInAnyDirection(Board b, Move initialPoint, int n_minAlign) {
		Direction dirs = new Direction();
		for (int i = 0; i < 4; i++) {
			int align = n_alignedToDirection(b, initialPoint, dirs.getDirection(i));
			if (align >= n_minAlign)
				return true;
		}
		return false;
	}

	private static boolean areNAlignedInAnyDirection(Board b, Move initialPoint, int n_align) {
		Direction dirs = new Direction();
		for (int i = 0; i < 4; i++) {
			int align = n_alignedToDirection(b, initialPoint, dirs.getDirection(i));
			if (align == n_align)
				return true;
		}
		return false;
	}

	private static int n_alignedToDirection(Board b, Move initialPoint, int[] direction) {
		int aligned= getAligned(b, initialPoint, direction).size();
		return (aligned);
	}

	/** 
	 * Recursive method that counts the number of aligned Moves of the same color
	 * @param b
	 * @param previousPoint
	 * @param direction
	 * @param occurrence
	 * @return
	 */
	private static PointList getAligned(Board b, Move firstPoint, int[] direction) {
		int[] opposite = Direction.getOpposite(direction);
		PointList aligned = new PointList();
		aligned.add(firstPoint.getPoint());
		aligned = getAlignedToDirection(b, firstPoint.getPoint(), firstPoint.getColor(), direction, aligned);
		aligned = getAlignedToDirection(b, firstPoint.getPoint(), firstPoint.getColor(), opposite, aligned);
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
	private static PointList getAlignedToDirection( Board b, GoPoint previousPoint, GoColor previousColor, int[] direction, PointList aligned) {
		if (previousColor.equals(GoColor.EMPTY)) {
			return aligned;
		}
		int previousX = previousPoint.getX();
		int previousY = previousPoint.getY();
		if (previousPoint.getX() + direction[0] < 0 //if reaches a border
                || previousPoint.getX() + direction[0] >= b.getSize()
                || previousPoint.getY() + direction[1] < 0
                || previousPoint.getY() + direction[1] >= b.getSize()) {
            return aligned;
        }
		GoPoint actualPoint = GoPoint.get(previousX+direction[0], previousY+direction[1]);
		GoColor actualColor = b.getColor(actualPoint);
		if (! actualColor.equals(previousColor)) {
			return aligned;
		}
		aligned.add(actualPoint);
		return getAlignedToDirection(b,actualPoint,actualColor, direction,aligned);
	}
}