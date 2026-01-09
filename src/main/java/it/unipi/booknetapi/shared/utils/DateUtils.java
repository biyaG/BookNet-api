package it.unipi.booknetapi.shared.utils;

import java.util.Calendar;
import java.util.Date;

public class DateUtils {

    public Date createDate(String yearStr, String monthStr, String dayStr) {
        if (yearStr == null || yearStr.isBlank()) {
            return null;
        }

        try {
            int year = Integer.parseInt(yearStr.trim());

            // Note: We subtract 1 because Calendar months are 0-11
            int month = (monthStr == null || monthStr.isBlank())
                    ? 0
                    : Integer.parseInt(monthStr.trim()) - 1;

            // Default to 1 if missing
            int day = (dayStr == null || dayStr.isBlank())
                    ? 1
                    : Integer.parseInt(dayStr.trim());

            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.YEAR, year);
            cal.set(Calendar.MONTH, month);
            cal.set(Calendar.DAY_OF_MONTH, day);

            // Zero out the time components to get a clean "Date only"
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            return cal.getTime();
        } catch (NumberFormatException e) {
            // Handle cases where strings contain non-numeric data
            System.err.println("Invalid number format in date strings: " + e.getMessage());
            return null;
        }
    }

}
