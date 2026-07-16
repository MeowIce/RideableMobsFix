package me.BigBou.rideableMobs.listener;

public record InputState(
        boolean forward,
        boolean backward,
        boolean left,
        boolean right,
        boolean jump,
        boolean sneak,
        boolean sprint
) {
}
