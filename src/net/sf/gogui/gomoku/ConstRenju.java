package net.sf.gogui.gomoku;

import net.sf.gogui.go.Board;
import net.sf.gogui.go.GoPoint;

public interface ConstRenju {
	
	public boolean isFour(Board b, GoPoint move, Direction dir);
	
	public boolean isDoubleFour(Board b, GoPoint move);
	
	public boolean isOpenFour(Board b, GoPoint move, Direction dir);

	public boolean isHalfOpenFour(Board b, GoPoint move, Direction dir);
	
	public boolean isDoubleOpenThree(Board b, GoPoint move);
	
	//Need Direction argument because then we will need
	//to check if there are two open three at the same time
	public boolean isOpenThree(Board b, GoPoint move, Direction dir);

	public boolean isFork(Board b, GoPoint move);
	
	public boolean isMoreThanFiveAligned(Board b, GoPoint move);
	
	public boolean isForbiddenMove(Board b, GoPoint move);
	
	

}
