package me.BigBou.rideableMobs.listener;

import me.BigBou.rideableMobs.util.Util;
import org.bukkit.Input;
import org.bukkit.util.Vector;

import java.nio.file.Files;
import java.nio.file.Path;

public final class MovementSmokeTest {
    private record TestInput(boolean forward, boolean backward, boolean left, boolean right,
                             boolean jump, boolean sneak, boolean sprint) implements Input {
        @Override
        public boolean isForward() {
            return forward;
        }

        @Override
        public boolean isBackward() {
            return backward;
        }

        @Override
        public boolean isLeft() {
            return left;
        }

        @Override
        public boolean isRight() {
            return right;
        }

        @Override
        public boolean isJump() {
            return jump;
        }

        @Override
        public boolean isSneak() {
            return sneak;
        }

        @Override
        public boolean isSprint() {
            return sprint;
        }
    }

    public static void main(String[] args) throws Exception {
        TestInput none = new TestInput(false, false, false, false, false, false, false);
        assert !none.isForward() && !none.isJump() && !none.isSneak();

        Vector stopped = Util.horizontalVelocity(0, none, 0.4);
        assert stopped.getX() == 0;
        assert stopped.getZ() == 0;

        Vector diagonal = Util.horizontalVelocity(0, new TestInput(true, false, true, false, false, false, false), 0.4);
        assert Math.abs(diagonal.length() - 0.4) < 0.000001;

        // vertical branches: flier sinks, ground jump, water sink, plain ground, swimmer floats, swim jump
        assert Util.verticalVelocity(0.1, false, false, true, false, false) == -0.08;
        assert Util.verticalVelocity(0.1, false, false, false, true, true) == 0.5;
        assert Util.verticalVelocity(0.1, true, false, false, false, false) == -0.08;
        assert Util.verticalVelocity(0.1, false, false, false, true, false) == 0.1;
        assert Util.verticalVelocity(0.1, true, true, false, false, false) == 0.1;
        assert Util.verticalVelocity(0.1, true, true, false, false, true) == 0.5;

        String pluginYaml = Files.readString(Path.of("src/main/resources/plugin.yml"));
        assert pluginYaml.contains("rideablemobs.commands:");
        assert pluginYaml.contains("rideablemobs.toggle:");
        assert pluginYaml.contains("rideablemobs.commands.permission: true");
        assert pluginYaml.contains("rideablemobs.commands.toggle: true");
        assert !pluginYaml.contains("ProtocolLib");

        String configYaml = Files.readString(Path.of("src/main/resources/config.yml"));
        assert configYaml.contains("armadillo: true");
        assert configYaml.contains("breeze: true");
        assert configYaml.contains("bogged: true");
        assert configYaml.contains("creaking: true");
        assert configYaml.contains("sniffer: true");
    }
}
