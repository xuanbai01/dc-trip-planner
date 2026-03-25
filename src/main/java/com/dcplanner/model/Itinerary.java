package com.dcplanner.model;

import java.util.List;

public class Itinerary {

    private List<Day> days;
    private double totalCost;
    private int totalAttractions;

    public Itinerary() {}

    public Itinerary(List<Day> days) {
        this.days = days;
        this.totalAttractions = days.stream()
                .mapToInt(d -> d.getTimeBlocks().size())
                .sum();
        this.totalCost = days.stream()
                .mapToDouble(Day::getDayCost)
                .sum();
    }

    public List<Day> getDays() { return days; }
    public double getTotalCost() { return totalCost; }
    public int getTotalAttractions() { return totalAttractions; }

    public void setDays(List<Day> days) { this.days = days; }
    public void setTotalCost(double totalCost) { this.totalCost = totalCost; }
    public void setTotalAttractions(int totalAttractions) { this.totalAttractions = totalAttractions; }

    // --- Nested: Day ---

    public static class Day {
        private int dayNumber;
        private List<TimeBlock> timeBlocks;
        private double dayCost;

        public Day() {}

        public Day(int dayNumber, List<TimeBlock> timeBlocks) {
            this.dayNumber = dayNumber;
            this.timeBlocks = timeBlocks;
            this.dayCost = timeBlocks.stream()
                    .mapToDouble(tb -> tb.getAttraction().getEntranceFee()
                            + (tb.getTravel() != null ? tb.getTravel().getTravelCost() : 0))
                    .sum();
        }

        public int getDayNumber() { return dayNumber; }
        public List<TimeBlock> getTimeBlocks() { return timeBlocks; }
        public double getDayCost() { return dayCost; }

        public void setDayNumber(int dayNumber) { this.dayNumber = dayNumber; }
        public void setTimeBlocks(List<TimeBlock> timeBlocks) { this.timeBlocks = timeBlocks; }
        public void setDayCost(double dayCost) { this.dayCost = dayCost; }
    }

    // --- Nested: TimeBlock ---

    public static class TimeBlock {
        private String startTime;
        private String endTime;
        private Attraction attraction;
        private TravelInfo travel;  // null for the first stop

        public TimeBlock() {}

        public TimeBlock(String startTime, String endTime, Attraction attraction, TravelInfo travel) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.attraction = attraction;
            this.travel = travel;
        }

        public String getStartTime() { return startTime; }
        public String getEndTime() { return endTime; }
        public Attraction getAttraction() { return attraction; }
        public TravelInfo getTravel() { return travel; }

        public void setStartTime(String startTime) { this.startTime = startTime; }
        public void setEndTime(String endTime) { this.endTime = endTime; }
        public void setAttraction(Attraction attraction) { this.attraction = attraction; }
        public void setTravel(TravelInfo travel) { this.travel = travel; }
    }

    // --- Nested: TravelInfo ---

    public static class TravelInfo {
        private List<String> metroStations;  // ordered list of stations traversed
        private int walkingMinutes;
        private double travelCost;

        public TravelInfo() {}

        public TravelInfo(List<String> metroStations, int walkingMinutes, double travelCost) {
            this.metroStations = metroStations;
            this.walkingMinutes = walkingMinutes;
            this.travelCost = travelCost;
        }

        public List<String> getMetroStations() { return metroStations; }
        public int getWalkingMinutes() { return walkingMinutes; }
        public double getTravelCost() { return travelCost; }

        public void setMetroStations(List<String> metroStations) { this.metroStations = metroStations; }
        public void setWalkingMinutes(int walkingMinutes) { this.walkingMinutes = walkingMinutes; }
        public void setTravelCost(double travelCost) { this.travelCost = travelCost; }
    }
}
