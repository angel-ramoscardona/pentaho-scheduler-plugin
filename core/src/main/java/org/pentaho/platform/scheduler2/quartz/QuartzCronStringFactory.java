/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2029-07-20
 ******************************************************************************/


package org.pentaho.platform.scheduler2.quartz;

import java.util.List;

import org.pentaho.platform.api.scheduler2.ComplexJobTrigger;
import org.pentaho.platform.api.scheduler2.wrappers.ITimeWrapper;
import org.pentaho.platform.scheduler2.recur.IncrementalRecurrence;
import org.pentaho.platform.scheduler2.recur.QualifiedDayOfMonth;
import org.pentaho.platform.scheduler2.recur.QualifiedDayOfWeek;
import org.pentaho.platform.scheduler2.recur.QualifiedDayOfWeek.DayOfWeekQualifier;
import org.pentaho.platform.scheduler2.recur.RecurrenceList;
import org.pentaho.platform.scheduler2.recur.SequentialRecurrence;

public class QuartzCronStringFactory {
  public static String createCronString( ComplexJobTrigger jobTrigger ) {
    StringBuffer stringBuffer = new StringBuffer();
    String secondRecurrence = getRecurrenceString( jobTrigger.getSecondRecurrences(), "*" ); //$NON-NLS-1$
    String minuteRecurrence = getRecurrenceString( jobTrigger.getMinuteRecurrences(), "*" ); //$NON-NLS-1$
    String hourlyRecurrence = getRecurrenceString( jobTrigger.getHourlyRecurrences(), "*" ); //$NON-NLS-1$
    String dayOfMonthRecurrence = getRecurrenceString( jobTrigger.getDayOfMonthRecurrences(), "*" ); //$NON-NLS-1$
    String monthlyRecurrence = getRecurrenceString( jobTrigger.getMonthlyRecurrences(), "*" ); //$NON-NLS-1$
    String dayOfWeekRecurrence = getRecurrenceString( jobTrigger.getDayOfWeekRecurrences(), "*" ); //$NON-NLS-1$
    String yearlyRecurrence = getRecurrenceString( jobTrigger.getYearlyRecurrences(), "*" ); //$NON-NLS-1$
    if ( dayOfWeekRecurrence.equals( "*" ) && dayOfMonthRecurrence.equals( "*" ) ) { //$NON-NLS-1$ //$NON-NLS-2$
      dayOfWeekRecurrence = "?"; //$NON-NLS-1$
    } else if ( !dayOfMonthRecurrence.equals( "*" ) ) { //$NON-NLS-1$
      dayOfWeekRecurrence = "?"; //$NON-NLS-1$
    } else if ( !dayOfWeekRecurrence.equals( "*" ) ) { //$NON-NLS-1$
      dayOfMonthRecurrence = "?"; //$NON-NLS-1$
    }
    stringBuffer.append( secondRecurrence );
    stringBuffer.append( " " ); //$NON-NLS-1$
    stringBuffer.append( minuteRecurrence );
    stringBuffer.append( " " ); //$NON-NLS-1$
    stringBuffer.append( hourlyRecurrence );
    stringBuffer.append( " " ); //$NON-NLS-1$
    stringBuffer.append( dayOfMonthRecurrence );
    stringBuffer.append( " " ); //$NON-NLS-1$
    stringBuffer.append( monthlyRecurrence );
    stringBuffer.append( " " ); //$NON-NLS-1$
    stringBuffer.append( dayOfWeekRecurrence );
    stringBuffer.append( " " ); //$NON-NLS-1$
    stringBuffer.append( yearlyRecurrence );
    return stringBuffer.toString();
  }

  private static String getRecurrenceString( ITimeWrapper recurrences, String defaultString ) {
    String aString = ""; //$NON-NLS-1$
    StringBuffer stringBuffer = new StringBuffer();
    for ( Object recurrence : recurrences.getRecurrences() ) {
      if ( recurrence instanceof RecurrenceList ) {
        aString = getRecurrenceString( (RecurrenceList) recurrence );
      } else if ( recurrence instanceof SequentialRecurrence ) {
        aString = getRecurrenceString( (SequentialRecurrence) recurrence );
      } else if ( recurrence instanceof IncrementalRecurrence ) {
        aString = getRecurrenceString( (IncrementalRecurrence) recurrence );
      } else if ( recurrence instanceof QualifiedDayOfWeek ) {
        aString = getRecurrenceString( (QualifiedDayOfWeek) recurrence );
      } else if ( recurrence instanceof QualifiedDayOfMonth ) {
        aString = getRecurrenceString( (QualifiedDayOfMonth) recurrence );
      }
      if ( aString.length() > 0 ) {
        stringBuffer.append( aString ).append( "," ); //$NON-NLS-1$
      }
    }

    if ( stringBuffer.length() != 0 ) {
      // Delete the last comma.
      stringBuffer.deleteCharAt( stringBuffer.length() - 1 );
    } else {
      stringBuffer.append( defaultString );
    }
    return stringBuffer.toString();
  }

  private static String getRecurrenceString( QualifiedDayOfWeek qualifiedDayOfWeek ) {
    String aString = ""; //$NON-NLS-1$
    if ( ( qualifiedDayOfWeek.getQualifier() != null ) && ( qualifiedDayOfWeek.getDayOfWeek() != null ) ) {
      if ( qualifiedDayOfWeek.getQualifier() == DayOfWeekQualifier.LAST ) {
        aString = Integer.toString( qualifiedDayOfWeek.getDayOfWeek().ordinal() + 1 ) + "L"; //$NON-NLS-1$
      } else {
        aString =
            Integer.toString( qualifiedDayOfWeek.getDayOfWeek().ordinal() + 1 )
                + "#" + ( qualifiedDayOfWeek.getQualifier().ordinal() + 1 ); //$NON-NLS-1$
      }
    }
    return aString;
  }

  private static String getRecurrenceString( QualifiedDayOfMonth qualifiedDayOfMonth ) {
    return qualifiedDayOfMonth.toString();
  }

  private static String getRecurrenceString( RecurrenceList recurrenceList ) {
    StringBuffer stringBuffer = new StringBuffer();
    if ( recurrenceList.getValues().size() > 0 ) {
      for ( Integer recurrence : recurrenceList.getValues() ) {
        if ( recurrence != null ) {
          stringBuffer.append( recurrence.toString() ).append( "," ); //$NON-NLS-1$
        }
      }
    }

    if ( stringBuffer.length() != 0 ) {
      // Delete the last comma.
      stringBuffer.deleteCharAt( stringBuffer.length() - 1 );
    }
    return stringBuffer.toString();
  }

  private static String getRecurrenceString( SequentialRecurrence sequentialRecurrence ) {
    String aString = ""; //$NON-NLS-1$
    if ( ( sequentialRecurrence.getFirstValue() != null ) && ( sequentialRecurrence.getLastValue() != null ) ) {
      aString = sequentialRecurrence.getFirstValue().toString() + "-" + sequentialRecurrence.getLastValue(); //$NON-NLS-1$
    } else if ( sequentialRecurrence.getFirstValue() != null ) {
      aString = sequentialRecurrence.getFirstValue().toString();
    } else if ( sequentialRecurrence.getLastValue() != null ) {
      aString = sequentialRecurrence.getLastValue().toString();
    }
    return aString;
  }

  private static String getRecurrenceString( IncrementalRecurrence incrementalRecurrence ) {
    String aString = ""; //$NON-NLS-1$
    if ( ( incrementalRecurrence.getStartingValue() != null ) && ( incrementalRecurrence.getIncrement() != null ) ) {
      aString = incrementalRecurrence.getStartingValue() + "/" + incrementalRecurrence.getIncrement(); //$NON-NLS-1$
    } else if ( incrementalRecurrence.getStartingValue() != null ) {
      aString = incrementalRecurrence.getStartingValue();
    }
    return aString;
  }
}
