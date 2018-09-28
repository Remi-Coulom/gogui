package net.sf.gogui.gomoku;

public final class Direction {
	private static final int[] EAST = new int[] {1,0};
	private static final int[] NORTHEAST = new int[] {1,1};
	private static final int[] NORTH = new int[] {0,1};
	private static final int[] NORTHWEST = new int[] {-1,1};
	private static final int[] WEST = new int[] {-1,0};
	private static final int[] SOUTHWEST = new int[] {-1,-1};
	private static final int[] SOUTH = new int[] {0,-1};
	private static final int[] SOUTHEAST = new int[] {1,-1};
	private static final int[][] DIRECTIONS =  new int[][] {EAST,NORTHEAST, NORTH, NORTHWEST, WEST, SOUTHWEST, SOUTH, SOUTHEAST};
	
	public static final int[][] getDirections () {
		return DIRECTIONS;
	}
	
	public static final int[] getDirection(int direction) {
		return DIRECTIONS[direction];
	}
	
	public static int[] getOpposite(int[] direction) {
		return new int[] {-direction[0],-direction[1]};
	}
}

