package net.sf.gogui.gomoku;

public final class Direction {
	private final int[] EAST = new int[] {1,0};
	private final int[] NORTHEAST = new int[] {1,1};
	private final int[] NORTH = new int[] {0,1};
	private final int[] NORTHWEST = new int[] {-1,1};
	private final int[] WEST = new int[] {-1,0};
	private final int[] SOUTHWEST = new int[] {-1,-1};
	private final int[] SOUTH = new int[] {0,-1};
	private final int[] SOUTHEAST = new int[] {1,-1};
	private final int[][] DIRECTIONS =  new int[][] {EAST,NORTHEAST, NORTH, NORTHWEST, WEST, SOUTHWEST, SOUTH, SOUTHEAST};
	
	public final int[][] getDirections () {
		return this.DIRECTIONS;
	}
	
	public final int[] getDirection(int direction) {
		return DIRECTIONS[direction];
	}
	
	public static int[] getOpposite(int[] direction) {
		return new int[] {-direction[0],-direction[1]};
	}
}

