package at.ac.fhcampuswien.worldclock;

public class WorldClockResponse {

    public String city;
    public String timezone;
    public String date;
    public String time;
    public String dayOfWeek;

    public WorldClockResponse(String city,
                              String timezone,
                              String date,
                              String time,
                              String dayOfWeek) {
        this.city = city;
        this.timezone = timezone;
        this.date = date;
        this.time = time;
        this.dayOfWeek = dayOfWeek;
    }
}