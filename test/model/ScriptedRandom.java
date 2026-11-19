package model;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

/**
 * Test helper: a Random whose nextInt(bound) returns values the test scripts in advance,
 * so dice, surprise events and turn order are known. With nothing scripted it returns
 * bound - 1, which keeps the players in the order they were given (Collections.shuffle).
 */
final class ScriptedRandom extends Random {
    private static final long serialVersionUID = 1L;
    private final Deque<Integer> values = new ArrayDeque<>();

    ScriptedRandom push(int... next) {
        for (int v : next) values.add(v);
        return this;
    }

    @Override
    public int nextInt(int bound) {
        if (values.isEmpty()) return bound - 1;
        int v = values.poll();
        if (v < 0 || v >= bound) throw new AssertionError("Scripted value " + v + " is outside 0.." + (bound - 1));
        return v;
    }
}
