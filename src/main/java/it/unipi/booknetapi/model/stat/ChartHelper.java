package it.unipi.booknetapi.model.stat;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class ChartHelper {

    public static final List<String> VALUES = List.of("day", "week", "month", "year");


    public record ChartParams(Date start, Date end, String granularity) {}

    public static ChartParams normalizeParams(Date startDate, Date endDate, String granularity) {

        String goodGranularity = (granularity == null || granularity.isEmpty())
                ? "day"
                : granularity;

        String finalGranularity = VALUES.contains(goodGranularity) ? goodGranularity : "day";

        Date finalEnd = (endDate == null)
                ? new Date()
                : endDate;

        Date finalStart;
        if (startDate == null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(finalEnd);
            cal.add(Calendar.DAY_OF_YEAR, -7); // Subtract 7 days
            finalStart = cal.getTime();
        } else {
            finalStart = startDate;
        }

        return new ChartParams(finalStart, finalEnd, finalGranularity);
    }

}
