package com.marcin.runningParameters;

public class checkParameters {

    public static String CadenceColor(int cadence, int paceSeconds)
    {
        int optimalCadence = (int)(215 - 0.1667*paceSeconds);
        int delta = optimalCadence-cadence;

        if (delta>15) return "#d50000"; //red color
        else if (delta>8) return "#ffc107"; //yellow color
        else if (delta>-8) return "#64dd17"; //green color
        else if (delta>-15) return "#ffc107"; //yellow color
        else return "#d50000"; //red color
    }

    public static String CadenceArrow(int cadence, int paceSeconds)
    {
        int optimalCadence = (int)(215 - 0.1667*paceSeconds);
        int delta = optimalCadence-cadence;

        if (delta>8) return "↑";
        else if (delta>-8) return "✓";
        else return "↓";
    }


}
