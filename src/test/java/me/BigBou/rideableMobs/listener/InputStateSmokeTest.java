package me.BigBou.rideableMobs.listener;

import me.BigBou.rideableMobs.util.Util;
import org.bukkit.util.Vector;

public final class InputStateSmokeTest {
    public static void main(String[] args) {
        InputState all = new InputState(true, true, true, true, true, true, true);
        assert all.forward();
        assert all.backward();
        assert all.left();
        assert all.right();
        assert all.jump();
        assert all.sneak();
        assert all.sprint();

        InputState none = new InputState(false, false, false, false, false, false, false);
        assert !none.forward();
        assert !none.backward();
        assert !none.left();
        assert !none.right();
        assert !none.jump();
        assert !none.sneak();
        assert !none.sprint();

        Vector stopped = Util.horizontalVelocity(0, none, 0.4);
        assert stopped.getX() == 0;
        assert stopped.getZ() == 0;

        Vector diagonal = Util.horizontalVelocity(0, new InputState(true, false, true, false, false, false, false), 0.4);
        assert Math.abs(diagonal.length() - 0.4) < 0.000001;
    }
}
