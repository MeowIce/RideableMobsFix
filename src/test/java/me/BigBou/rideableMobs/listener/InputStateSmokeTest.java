package me.BigBou.rideableMobs.listener;

import me.BigBou.rideableMobs.util.Util;
import org.bukkit.util.Vector;

import java.nio.file.Files;
import java.nio.file.Path;

public final class InputStateSmokeTest {
    public static void main(String[] args) throws Exception {
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

        String pluginYaml = Files.readString(Path.of("src/main/resources/plugin.yml"));
        assert pluginYaml.contains("rideablemobs.commands:");
        assert pluginYaml.contains("rideablemobs.toggle:");
        assert pluginYaml.contains("rideablemobs.commands.permission: true");
        assert pluginYaml.contains("rideablemobs.commands.toggle: true");

        String configYaml = Files.readString(Path.of("src/main/resources/config.yml"));
        assert configYaml.contains("armadillo: true");
        assert configYaml.contains("breeze: true");
        assert configYaml.contains("bogged: true");
        assert configYaml.contains("creaking: true");
        assert configYaml.contains("sniffer: true");
    }
}
