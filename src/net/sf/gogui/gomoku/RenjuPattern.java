package net.sf.gogui.gomoku;

import java.util.ArrayList;
import java.util.Comparator;

import net.sf.gogui.go.Board;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.PointList;

public class RenjuPattern
extends CheckAlignment
implements ConstRenju{
	
	private final int LENGTHOFPATTERN = 5;
	private ArrayList<PointList> allFourPatterns;
	private PointList alignment; // current alignment that the methods will look at
	private GoColor color;
	
	public RenjuPattern(Board b, GoPoint point, GoColor color) {
		this.color = color;
		allFourPatterns = new ArrayList<PointList>();
		Direction d = new Direction();
		for (int i = 0; i < 4; i++) {
			PointList pattern;
			pattern = this.getPattern(b, point, d.getDirection(i));
			pattern.sort(null);
			System.out.println(pattern.size());
			allFourPatterns.add(pattern);
		}
		alignment = allFourPatterns.get(0);
	}

	@Override
	public boolean isFour(Board b, GoPoint move, Direction dir) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isDoubleFour(Board b, GoPoint move) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isOpenFour(Board b, GoPoint move, Direction dir) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isHalfOpenFour(Board b, GoPoint move, Direction dir) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isDoubleOpenThree(Board b, GoPoint move) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isOpenThree(Board b, GoPoint move, Direction dir) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isFork(Board b, GoPoint move) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isMoreThanFiveAligned(Board b, GoPoint move) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isForbiddenMove(Board b, GoPoint move) {
		// TODO Auto-generated method stub
		return false;
	}
	

	public PointList getPattern(Board b, GoPoint firstPoint, int[] direction) {
		int[] opposite = Direction.getOpposite(direction);
		PointList aligned = new PointList();
		aligned.add(firstPoint);
		aligned = getPattern(b, firstPoint, direction, aligned, LENGTHOFPATTERN);
		aligned = getPattern(b, firstPoint, opposite, aligned, LENGTHOFPATTERN);
		return aligned;
	}

	private PointList getPattern(Board b, GoPoint previousPoint, int[] direction, PointList aligned, int stopPatternBuilder) {
		if (previousPoint.getX() + direction[0] < 0 //if reaches a border
				|| previousPoint.getX() + direction[0] >= b.getSize()
				|| previousPoint.getY() + direction[1] < 0
				|| previousPoint.getY() + direction[1] >= b.getSize()) {
			return aligned;
		}
		if (stopPatternBuilder == 0)
			return aligned;
		GoPoint actualPoint = GoPoint.get(previousPoint.getX()+direction[0], previousPoint.getY()+direction[1]);
		aligned.add(actualPoint);
		return getPattern(b,actualPoint,direction,aligned,stopPatternBuilder-1);
	}
	
	public String toString(Board b) {
		String res = "";
		for (PointList p : allFourPatterns ) {
			for (GoPoint pp: p) {
				res += b.getColor(pp) + " ";
			}
			res += '\n';
		}
		return res;
	}
	
}