package com.kuky.backend.placement.model;

/** Teacher-configurable minimum score (%) a section must reach to be awarded a given CEFR level. */
public class PlacementLevelThreshold {

    private CefrLevel level;
    private int minScorePercent;

    public CefrLevel getLevel() { return level; }
    public void setLevel(CefrLevel level) { this.level = level; }
    public int getMinScorePercent() { return minScorePercent; }
    public void setMinScorePercent(int minScorePercent) { this.minScorePercent = minScorePercent; }
}
