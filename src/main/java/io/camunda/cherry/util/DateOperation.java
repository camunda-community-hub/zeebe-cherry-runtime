package io.camunda.cherry.util;

import io.camunda.cherry.runtime.HistoryPerformance;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class DateOperation {

  /**
   * Class util
   */
  private DateOperation() {}
  /**
   * Return from a local Time a string,according the offset of the user
   *
   * @param time           time to transform
   * @param timezoneOffset offset of the user
   * @return the date as a String
   */
  public static String dateTimeToHumanString(LocalDateTime time, long timezoneOffset) {
    if (time == null)
      return null;
    // Attention, we have to get the time in UTC first
    //  datecreation: "2021-01-30T18:52:10.973"
    LocalDateTime localDateTime = time.minusMinutes(timezoneOffset);
    DateTimeFormatter sdt = DateTimeFormatter.ofPattern(HistoryPerformance.HUMAN_DATE_FORMATER);
    return localDateTime.format(sdt);
  }

  public static LocalDateTime getLocalDateTimeNow() {
    return LocalDateTime.now(ZoneOffset.UTC);
  }

}
