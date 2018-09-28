package net.sf.gogui.gomoku;

import java.util.ArrayList;

import javax.swing.plaf.synth.SynthScrollBarUI;

import net.sf.gogui.go.Board;
import net.sf.gogui.go.ConstPointList;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.Move;
import net.sf.gogui.go.PointList;

public class RenjuPattern {

    private final int LENGTHOFPATTERN = 5;
    private ArrayList<PointList> allFourPatterns;
    private Move move;
    private Board board;
    private boolean isMainPattern;

    public RenjuPattern(Board b, Move move) {
        this.move = move;
        this.board = b;
        this.isMainPattern = true;
        allFourPatterns = new ArrayList<PointList>();
        for (int i = 0; i < 4; i++) {
            PointList pattern;
            pattern = this.getPattern(Direction.getDirection(i));
            pattern.sort(null);
            allFourPatterns.add(pattern);
        }
    }
    
    private RenjuPattern(Board b, Move move, boolean isSecPattern) {
        this.move = move;
        this.board = b;
        this.isMainPattern = false;
        allFourPatterns = new ArrayList<PointList>();
        for (int i = 0; i < 4; i++) {
            PointList pattern;
            pattern = this.getPattern(Direction.getDirection(i));
            pattern.sort(null);
            allFourPatterns.add(pattern);
        }
    }

    /*
     * If open four, is double four is true too
     */
    private boolean isFour(int direction, boolean reverse) {
        GoColor[] pattern = new GoColor[]{GoColor.BLACK, GoColor.BLACK, GoColor.BLACK, GoColor.BLACK, GoColor.EMPTY};
        GoColor[] pattern2 = new GoColor[] {GoColor.BLACK, GoColor.BLACK, GoColor.EMPTY, GoColor.BLACK, GoColor.BLACK};
        GoColor[] pattern3 = new GoColor[] {GoColor.BLACK, GoColor.EMPTY, GoColor.BLACK, GoColor.BLACK, GoColor.BLACK};
        GoColor[] pattern22 = new GoColor[] {GoColor.BLACK,  GoColor.BLACK, GoColor.EMPTY, GoColor.BLACK, GoColor.BLACK, GoColor.EMPTY, GoColor.BLACK, GoColor.BLACK};
        boolean res = false, res2 = false, res3 = false;
        if (!reverse) {
            res = matchesPattern(pattern, direction);
            res2 = matchesPattern(pattern2, direction);
            res3 = matchesPattern(pattern3, direction);
        }else {
            res = matchesPattern(reverse(pattern), direction);
            res2 = matchesPattern(pattern22, direction);
            res3 = matchesPattern(reverse(pattern3), direction);
        }
        return ( res || res2 || res3);
    }

    /**
     * This method includes the isOpenFour method.
     * @return
     */
    public boolean isDoubleFour() {
        int four = 0;
        for (int i = 0; i < 4; i++)
            for (int j = 0; j < 2; j++)
                if (isFour(i, j==1)) {
                    four ++;
                    if (four == 2) 
                        return true;
                }
        return false;
    }

    private boolean isOpenThree(int direction) {
        GoColor[] pattern = new GoColor[] {GoColor.EMPTY, GoColor.BLACK, GoColor.BLACK, GoColor.EMPTY, GoColor.BLACK, GoColor.EMPTY};
        GoColor[] pattern2 = new GoColor[] {GoColor.EMPTY, GoColor.EMPTY, GoColor.BLACK, GoColor.BLACK, GoColor.BLACK, GoColor.EMPTY};
        return (matchesPattern(pattern, direction)
                || matchesPattern(pattern2, direction)
                ||matchesPattern(reverse(pattern), direction)
                ||matchesPattern(reverse(pattern2), direction));
    }

    public boolean isDoubleOpenThree() {
        int three = 0;
        for (int i = 0; i < 4; i++)
            if (isOpenThree(i))  {
                    three ++;
                    if (three == 2) {
                        return true;
                }
            }
        return false;
    }

    public boolean isFork() {
        return isDoubleFour() || isDoubleOpenThree();
    }

    public boolean isMoreThanFiveAligned() {
        final GoColor[] pattern = new GoColor[] {GoColor.BLACK, GoColor.BLACK, GoColor.BLACK, GoColor.BLACK, GoColor.BLACK, GoColor.BLACK};
        for (int i = 0; i < 4; i++) {
            if (matchesPattern(pattern, i)) {
                return true;
            }
        }
        return false;
    }

    public boolean isForbiddenMove() {
        return (this.move.getColor().equals(GoColor.BLACK) && (isFork() || isMoreThanFiveAligned() ));
    }


    public PointList getPattern(int[] direction) {
        int[] opposite = Direction.getOpposite(direction);
        PointList aligned = new PointList();
        aligned.add(move.getPoint());
        aligned = getPattern(move.getPoint(), direction, aligned, LENGTHOFPATTERN);
        aligned = getPattern(move.getPoint(), opposite, aligned, LENGTHOFPATTERN);
        return aligned;
    }

    private PointList getPattern(GoPoint previousPoint, int[] direction, PointList aligned, int stopPatternBuilder) {
        if (previousPoint.getX() + direction[0] < 0 //if reaches a border
                || previousPoint.getX() + direction[0] >= board.getSize()
                || previousPoint.getY() + direction[1] < 0
                || previousPoint.getY() + direction[1] >= board.getSize()) {
            return aligned;
        }
        if (stopPatternBuilder == 0)
            return aligned;
        GoPoint actualPoint = GoPoint.get(previousPoint.getX()+direction[0], previousPoint.getY()+direction[1]);
        aligned.add(actualPoint);
        return getPattern(actualPoint,direction,aligned,stopPatternBuilder-1);
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

    private boolean matchesPattern(GoColor[] pattern, int direction) {
        PointList actualPattern = allFourPatterns.get(direction);
        if (pattern.length > actualPattern.size()) {
            return false;
        }
        GoPoint actualPoint;
        GoColor actualColor;

        for (int i = 0; i <= actualPattern.size() - pattern.length; i++) {
            actualPoint = actualPattern.get(i);
            if (actualPoint.equals(move.getPoint()))
                actualColor = move.getColor();
            else
                actualColor = board.getColor(actualPoint);
            boolean matches = true;
            for (int j = 0; j < pattern.length && matches; j++) {
                actualPoint = actualPattern.get(i+j);
                if (actualPoint.equals(move.getPoint()))
                    actualColor = move.getColor();
                else
                    actualColor = board.getColor(actualPoint);
                matches = actualColor.equals(pattern[j]);
                if (matches)  {

                }
                if (matches && pattern[j].equals(GoColor.EMPTY) && this.isMainPattern) {
                    Board b2 = new Board(this.board.getSize());
                    for (int l = 0; l < board.getNumberMoves() ; l++) {
                         b2.play(board.getMove(l));
                    }
                //    b2.play(this.move);
                    RenjuPattern rjtest = new RenjuPattern(b2, Move.get(GoColor.BLACK, actualPoint),false);
                    if (rjtest.isForbiddenMove()) {
                        return false;
                    }
                }
                if (j == pattern.length - 1 && matches)
                    return true;
            }
        }
        return false;
    }

    private GoColor[] reverse(GoColor[] pattern) {
        GoColor[] reversed = new GoColor[pattern.length];
        for (int i = 0; i < pattern.length; i++)
            reversed[i] = pattern[pattern.length - i - 1];
        return reversed;
    }


    private int getAligned(PointList pattern, GoPoint point, GoColor color) {
        PointList aligned = new PointList();
        if (color.equals(GoColor.EMPTY)) {
            return aligned.size();
        }
        int indexInPattern = pattern.indexOf(point);
        aligned.add(point);

        aligned = getAligned(pattern, indexInPattern, -1, aligned);
        aligned = getAligned(pattern, indexInPattern, 1, aligned);

        return aligned.size();
    }

    private PointList getAligned(PointList pattern, int indexInPattern, int sense, PointList aligned) {
        GoPoint actualPoint = pattern.get(indexInPattern + sense);
        if (!board.getColor(actualPoint).equals(this.move.getColor())) {
            return aligned;
        }
        aligned.add(actualPoint);
        return getAligned(pattern,indexInPattern + sense, sense, aligned);
    }

}