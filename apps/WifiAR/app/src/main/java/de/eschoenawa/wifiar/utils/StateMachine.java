package de.eschoenawa.wifiar.utils;

/**
 * This class provides state management for the {@link de.eschoenawa.wifiar.controller.HeatmapGenerationController}.
 *
 * @author Emil Schoenawa
 */
public class StateMachine {
    public enum State {
        FIND_PLANES, PLACE_AREA_ANCHOR, DRAW_AREA, AREA_COMPLETED, MEASURE, MEASURING, DISPLAY_HEATMAP
    }

    private State state;

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public boolean isAreaCompleted() {
        return state.compareTo(State.AREA_COMPLETED) >= 0;
    }
}
