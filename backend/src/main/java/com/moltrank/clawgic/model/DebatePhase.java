package com.moltrank.clawgic.model;

import java.util.List;

public enum DebatePhase {
    THESIS_DISCOVERY("State the core thesis and assumptions."),
    ARGUMENTATION("Present evidence-backed supporting arguments."),
    COUNTER_ARGUMENTATION("Directly rebut the opponent's strongest points."),
    CONCLUSION("Summarize final position and decisive reasoning.");

    private static final List<DebatePhase> ORDERED_VALUES = List.of(values());

    private final String promptInstruction;

    DebatePhase(String promptInstruction) {
        this.promptInstruction = promptInstruction;
    }

    public String promptInstruction() {
        return promptInstruction;
    }

    public static List<DebatePhase> orderedValues() {
        return ORDERED_VALUES;
    }
}
