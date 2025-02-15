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


package org.pentaho.mantle.client.dialogs.scheduling;

import java.util.Date;
import java.util.List;

import org.pentaho.gwt.widgets.client.dialogs.IDialogCallback;
import org.pentaho.gwt.widgets.client.dialogs.MessageDialogBox;
import org.pentaho.gwt.widgets.client.dialogs.PromptDialogBox;
import org.pentaho.gwt.widgets.client.utils.TimeUtil;
import org.pentaho.gwt.widgets.client.utils.TimeUtil.DayOfWeek;
import org.pentaho.gwt.widgets.client.utils.TimeUtil.MonthOfYear;
import org.pentaho.gwt.widgets.client.utils.TimeUtil.WeekOfMonth;
import org.pentaho.gwt.widgets.client.utils.string.StringUtils;
import org.pentaho.gwt.widgets.client.wizards.AbstractWizardDialog;
import org.pentaho.gwt.widgets.client.wizards.IWizardPanel;
import org.pentaho.mantle.client.dialogs.scheduling.RecurrenceEditor.DailyRecurrenceEditor;
import org.pentaho.mantle.client.dialogs.scheduling.RecurrenceEditor.MonthlyRecurrenceEditor;
import org.pentaho.mantle.client.dialogs.scheduling.RecurrenceEditor.WeeklyRecurrenceEditor;
import org.pentaho.mantle.client.dialogs.scheduling.RecurrenceEditor.YearlyRecurrenceEditor;
import org.pentaho.mantle.client.dialogs.scheduling.ScheduleEditor.DurationValues;
import org.pentaho.mantle.client.dialogs.scheduling.ScheduleEditor.ScheduleType;
import org.pentaho.mantle.client.messages.Messages;
import org.pentaho.mantle.client.environment.EnvironmentHelper;
import org.pentaho.mantle.client.workspace.BlockoutPanel;
import org.pentaho.mantle.client.workspace.JsBlockStatus;
import org.pentaho.mantle.client.workspace.JsJob;
import org.pentaho.mantle.client.workspace.JsJobParam;
import org.pentaho.mantle.client.workspace.JsJobTrigger;
import org.pentaho.mantle.login.client.MantleLoginDialog;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayInteger;
import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONNull;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.user.client.Random;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.Widget;

public class ScheduleRecurrenceDialog extends AbstractWizardDialog {

  private static final String HOUR_MINUTE_SECOND = "h:mm:ss a";

  protected String filePath;
  protected String outputLocation;
  protected String scheduleName;
  protected String scheduleOwner;

  protected String appendDateFormat;
  protected boolean overwriteFile;

  private IDialogCallback callback;

  boolean isBlockoutDialog = false;
  private ScheduleEmailDialog scheduleEmailDialog;
  private ScheduleParamsDialog scheduleParamsDialog;
  protected ScheduleEditorWizardPanel scheduleEditorWizardPanel;

  protected JsJob editJob = null;
  private Boolean done = false;
  private boolean hasParams = false;
  private boolean isEmailConfValid = false;
  private boolean showSuccessDialog = true;

  private ScheduleEditor scheduleEditor;

  private PromptDialogBox parentDialog;

  private boolean newSchedule = true;

  public ScheduleRecurrenceDialog( PromptDialogBox parentDialog, JsJob jsJob, IDialogCallback callback,
                                   boolean hasParams, boolean isEmailConfValid, final ScheduleDialogType type ) {
    super( type, Messages.getString( type != ScheduleDialogType.BLOCKOUT ? "editSchedule" : "editBlockoutSchedule" ),
      null, false, true );

    isBlockoutDialog = type == ScheduleDialogType.BLOCKOUT;
    setCallback( callback );
    editJob = jsJob;
    this.parentDialog = parentDialog;
    newSchedule = false;
    String dateFormat = jsJob.getJobParamValue( ScheduleParamsHelper.APPEND_DATE_FORMAT_KEY );
    String autoCreateUniqueFilename = jsJob.getJobParamValue( ScheduleParamsHelper.AUTO_CREATE_UNIQUE_FILENAME_KEY );
    boolean overwrite = false;
    if ( autoCreateUniqueFilename != null ) {
      overwrite = !Boolean.parseBoolean( autoCreateUniqueFilename );
    }

    constructDialog( jsJob.getInputFilePath(), jsJob.getOutputPath(), jsJob.getJobName(), dateFormat, overwrite, hasParams,
      isEmailConfValid, jsJob );

    setResponsive( true );
    setSizingMode( DialogSizingMode.FILL_VIEWPORT );
    setWidthCategory( DialogWidthCategory.SMALL );
  }

  public ScheduleRecurrenceDialog( PromptDialogBox parentDialog, String filePath, String outputLocation,
                                   String scheduleName, String dateFormat, boolean overwriteFile,
                                   IDialogCallback callback, boolean hasParams, boolean isEmailConfValid ) {
    super( ScheduleDialogType.SCHEDULER, Messages.getString( "newSchedule" ), null, false, true );

    isBlockoutDialog = false;
    setCallback( callback );
    this.parentDialog = parentDialog;

    constructDialog( filePath, outputLocation, scheduleName, dateFormat, overwriteFile, hasParams, isEmailConfValid,  null );

    setResponsive( true );
    setSizingMode( DialogSizingMode.FILL_VIEWPORT );
    setWidthCategory( DialogWidthCategory.SMALL );
  }

  public ScheduleRecurrenceDialog( PromptDialogBox parentDialog, ScheduleDialogType type, String title,
                                   String filePath, String outputLocation, String scheduleName, IDialogCallback callback, boolean hasParams,
                                   boolean isEmailConfValid ) {
    super( type, title, null, false, true );

    isBlockoutDialog = type == ScheduleDialogType.BLOCKOUT;
    setCallback( callback );
    this.parentDialog = parentDialog;

    constructDialog( filePath, outputLocation, scheduleName, null, false, hasParams, isEmailConfValid, null );

    setResponsive( true );
    setSizingMode( DialogSizingMode.FILL_VIEWPORT );
    setWidthCategory( DialogWidthCategory.SMALL );
  }

  @Override
  public boolean onKeyDownPreview( char key, int modifiers ) {
    if ( key == KeyCodes.KEY_ESCAPE ) {
      hide();
    }
    return true;
  }

  public void setParentDialog( PromptDialogBox parentDialog ) {
    this.parentDialog = parentDialog;
  }

  public void addCustomPanel( Widget w, DockPanel.DockLayoutConstant position ) {
    scheduleEditorWizardPanel.add( w, position );
  }

  private void constructDialog( String filePath, String outputLocation, String scheduleName, String dateFormat, boolean overwriteFile, boolean hasParams,
                                boolean isEmailConfValid, JsJob jsJob ) {

    this.hasParams = hasParams;
    this.filePath = filePath;
    this.isEmailConfValid = isEmailConfValid;
    this.outputLocation = outputLocation;
    this.scheduleName = scheduleName;
    this.appendDateFormat = dateFormat;
    this.overwriteFile = overwriteFile;
    scheduleEditorWizardPanel = new ScheduleEditorWizardPanel( getDialogType() );
    scheduleEditor = scheduleEditorWizardPanel.getScheduleEditor();
    String url = ScheduleHelper.getPluginContextURL() + "api/scheduler/blockout/hasblockouts?ts=" + System.currentTimeMillis(); //$NON-NLS-1$
    RequestBuilder hasBlockoutsRequest = new RequestBuilder( RequestBuilder.GET, url );
    hasBlockoutsRequest.setHeader( "If-Modified-Since", "01 Jan 1970 00:00:00 GMT" );
    hasBlockoutsRequest.setHeader( "accept", "text/plain" );
    try {
      hasBlockoutsRequest.sendRequest( url, new RequestCallback() {

        @Override
        public void onError( Request request, Throwable exception ) {
          MessageDialogBox dialogBox =
            new MessageDialogBox( Messages.getString( "error" ), exception.toString(), false, false, true ); //$NON-NLS-1$
          dialogBox.center();
        }

        @Override
        public void onResponseReceived( Request request, Response response ) {
          Boolean hasBlockouts = Boolean.valueOf( response.getText() );
          if ( hasBlockouts ) {
            scheduleEditor.setBlockoutButtonHandler( new ClickHandler() {
              @Override
              public void onClick( final ClickEvent clickEvent ) {
                PromptDialogBox box =
                  new PromptDialogBox( Messages.getString( "blockoutTimes" ), Messages.getString( "close" ), null,
                    null, false, true, new BlockoutPanel( false ) );
                box.center();
              }
            } );
          }
          scheduleEditor.getBlockoutCheckButton().setVisible( hasBlockouts );
        }
      } );
    } catch ( RequestException e ) {
      MessageDialogBox dialogBox = new MessageDialogBox( Messages.getString( "error" ), e.toString(), //$NON-NLS-1$
        false, false, true );
      dialogBox.center();
    }
    IWizardPanel[] wizardPanels = { scheduleEditorWizardPanel };
    setWizardPanels( wizardPanels );
    setPixelSize( 475, 465 );
    center();
    if ( ( hasParams || isEmailConfValid ) && ( isBlockoutDialog == false ) ) {
      finishButton.setText( Messages.getString( "next" ) ); //$NON-NLS-1$
    } else {
      finishButton.setText( Messages.getString( "finish" ) ); //$NON-NLS-1$
    }
    setupExisting( jsJob );

    //setHeight("100%"); //$NON-NLS-1$
    setSize( "650px", "450px" );
    addStyleName( "schedule-recurrence-dialog" );
  }

  private void setupExisting( JsJob jsJob ) {
    if ( jsJob != null && !jsJob.equals( "" ) ) { //$NON-NLS-1$
      JsJobTrigger jsJobTrigger = jsJob.getJobTrigger();
      ScheduleType scheduleType = ScheduleType.valueOf( jsJobTrigger.getScheduleType() );
      // scheduleEditor.setScheduleName(jsJob.getJobName());
      scheduleEditor.setScheduleType( scheduleType );
      if ( scheduleType == ScheduleType.CRON || jsJobTrigger.getType().equals( "cronJobTrigger" ) ) { //$NON-NLS-1$
        scheduleEditor.getCronEditor().setCronString( jsJobTrigger.getCronString() );
      } else if ( jsJobTrigger.getType().equals( "simpleJobTrigger" ) ) { //$NON-NLS-1$
        if ( scheduleType != ScheduleType.RUN_ONCE ) {
          // Recurring simple Trigger
          int interval = jsJobTrigger.getRepeatInterval();
          scheduleEditor.setRepeatInSecs( interval );
          if ( scheduleType == ScheduleType.DAILY ) {
            DailyRecurrenceEditor dailyRecurrenceEditor = scheduleEditor.getRecurrenceEditor().getDailyEditor();
            dailyRecurrenceEditor.setEveryNDays();
            dailyRecurrenceEditor.enableIgnoreDST();
            dailyRecurrenceEditor.setIgnoreDST(true);
          }
        }
      } else if ( jsJobTrigger.getType().equals( "complexJobTrigger" ) ) { //$NON-NLS-1$
        if ( scheduleType == ScheduleType.DAILY ) {
          // Daily
          DailyRecurrenceEditor dailyRecurrenceEditor = scheduleEditor.getRecurrenceEditor().getDailyEditor();
          if ( jsJobTrigger.isWorkDaysInWeek() ) {
            dailyRecurrenceEditor.setEveryWeekday();
            dailyRecurrenceEditor.setIgnoreDST(false);
            dailyRecurrenceEditor.disableIgnoreDST();
          } else {
            dailyRecurrenceEditor.setEveryNDays();
            int interval = jsJobTrigger.getRepeatInterval();
            scheduleEditor.setRepeatInSecs( interval );
            dailyRecurrenceEditor.enableIgnoreDST();
            dailyRecurrenceEditor.setIgnoreDST(false);
          }
        } else if ( scheduleType == ScheduleType.WEEKLY ) {
          int[] daysOfWeek = jsJobTrigger.getDayOfWeekRecurrences();
          WeeklyRecurrenceEditor weeklyRecurrenceEditor = scheduleEditor.getRecurrenceEditor().getWeeklyEditor();
          String strDays = ""; //$NON-NLS-1$
          for ( int element2 : daysOfWeek ) {
            strDays += Integer.toString( element2 ) + ","; //$NON-NLS-1$
          }
          weeklyRecurrenceEditor.setCheckedDaysAsString( strDays, 1 );
        } else if ( scheduleType == ScheduleType.MONTHLY ) {
          MonthlyRecurrenceEditor monthlyRecurrenceEditor = scheduleEditor.getRecurrenceEditor().getMonthlyEditor();
          if ( jsJobTrigger.isQualifiedDayOfWeekRecurrence() ) {
            // Run Every on ___day of Nth week every month
            monthlyRecurrenceEditor.setDayOfWeek( TimeUtil.DayOfWeek.valueOf( jsJobTrigger.getQualifiedDayOfWeek() ) );
            monthlyRecurrenceEditor
              .setWeekOfMonth( TimeUtil.WeekOfMonth.valueOf( jsJobTrigger.getDayOfWeekQualifier() ) );
            monthlyRecurrenceEditor.setNthDayNameOfMonth();
          } else {
            // Run on Nth day of the month
            monthlyRecurrenceEditor.setDayOfMonth( Integer.toString( jsJobTrigger.getDayOfMonthRecurrences()[0] ) );
          }
        } else if ( scheduleType == ScheduleType.YEARLY ) {
          YearlyRecurrenceEditor yearlyRecurrenceEditor = scheduleEditor.getRecurrenceEditor().getYearlyEditor();
          if ( jsJobTrigger.isQualifiedDayOfWeekRecurrence() ) {
            // Run Every on ___day of Nth week of the month M yearly
            yearlyRecurrenceEditor.setDayOfWeek( TimeUtil.DayOfWeek.valueOf( jsJobTrigger.getQualifiedDayOfWeek() ) );
            yearlyRecurrenceEditor
              .setWeekOfMonth( TimeUtil.WeekOfMonth.valueOf( jsJobTrigger.getDayOfWeekQualifier() ) );
            yearlyRecurrenceEditor.setMonthOfYear1( TimeUtil.MonthOfYear
              .get( jsJobTrigger.getMonthlyRecurrences()[0] - 1 ) );
            yearlyRecurrenceEditor.setNthDayNameOfMonthName();
          } else {
            // Run on Nth day of the month M yearly
            yearlyRecurrenceEditor.setDayOfMonth( Integer.toString( jsJobTrigger.getDayOfMonthRecurrences()[0] ) );
            yearlyRecurrenceEditor.setMonthOfYear0( TimeUtil.MonthOfYear
              .get( jsJobTrigger.getMonthlyRecurrences()[0] - 1 ) );
            yearlyRecurrenceEditor.setEveryMonthOnNthDay();
          }
        }
      }

      for ( int i = 0; i < jsJob.getJobParams().length(); i++ ) {
        JsJobParam param = jsJob.getJobParams().get( i );
        if ( param.getName().equals( "runSafeMode" ) ) {
          scheduleEditor.setEnableSafeMode( Boolean.valueOf( param.getValue() ) );
        } else if ( param.getName().equals( "gatheringMetrics" ) ) {
          scheduleEditor.setGatherMetrics( Boolean.valueOf( param.getValue() ) );
        } else if ( param.getName().equals( "logLevel" ) ) {
          scheduleEditor.setLogLevel( param.getValue() );
        }
      }

      scheduleEditor.setStartDate( jsJobTrigger.getScheduleStartTime() );
      int uiStartHour = jsJobTrigger.getStartHour();
      int uiStartAmPm = 0;
      if ( uiStartHour == 0 ) {
        uiStartHour = 12;
      } else if ( uiStartHour > 12 ) {
        uiStartHour -= 12;
        uiStartAmPm = 1;
      }
      scheduleEditor.setStartTime( uiStartHour, jsJobTrigger.getStartMin(), uiStartAmPm );
      scheduleEditor.setTimeZone( jsJobTrigger.getTimeZone() );

      if ( jsJobTrigger.getEndTime() == null ) {
        scheduleEditor.setNoEndDate();
      } else {
        scheduleEditor.setEndDate( jsJobTrigger.getScheduleEndTime() );
        scheduleEditor.setEndBy();
      }

      if ( isBlockoutDialog ) {
        scheduleEditor.setDurationFields( jsJobTrigger.getBlockDuration() );
      }
    }
  }

  protected JSONObject getJsonSimpleTrigger( int repeatCount, int interval, int startHour, int startMin, int startYear
    , int startMonth, int startDay, Date endDate, boolean enableSafeMode, boolean gatherMetrics, String logLevel ) {
    JSONObject trigger = new JSONObject();
    trigger.put( "uiPassParam", new JSONString( scheduleEditorWizardPanel.getScheduleType().name() ) ); //$NON-NLS-1$
    trigger.put( "repeatInterval", new JSONNumber( interval ) ); //$NON-NLS-1$
    trigger.put( "runSafeMode", new JSONString( String.valueOf( enableSafeMode ) ) );
    trigger.put( "gatheringMetrics", new JSONString( String.valueOf( gatherMetrics ) ) );
    trigger.put( "logLevel", new JSONString( logLevel ) );
    addJsonStartEnd( trigger, startHour, startMin, startYear, startMonth, startDay, endDate );

    return trigger;
  }

  protected JSONObject getJsonComplexTrigger( int repeatCount, int interval, int startHour, int startMin, int startYear
    , int startMonth, int startDay, Date endDate ) {
    JSONObject trigger = new JSONObject();
    trigger.put( "uiPassParam", new JSONString( scheduleEditorWizardPanel.getScheduleType().name() ) ); //$NON-NLS-1$
    trigger.put( "repeatInterval", new JSONNumber( interval ) ); //$NON-NLS-1$
    trigger.put( "repeatCount", new JSONNumber( repeatCount ) ); //$NON-NLS-1$
    trigger.put( "cronString", new JSONString( "TO_BE_GENERATED" ) ); //$NON-NLS-1$
    addJsonStartEnd( trigger, startHour, startMin, startYear, startMonth, startDay, endDate );

    return trigger;
  }

  protected JSONObject getJsonCronTrigger( String cronString, int startHour, int startMin, int startYear
    , int startMonth, int startDay, Date endDate ) {
    JSONObject trigger = new JSONObject();
    trigger.put( "uiPassParam", new JSONString( scheduleEditorWizardPanel.getScheduleType().name() ) ); //$NON-NLS-1$
    trigger.put( "cronString", new JSONString( cronString ) ); //$NON-NLS-1$
    addJsonStartEnd( trigger, startHour, startMin, startYear, startMonth, startDay, endDate );
    return trigger;
  }

  protected JSONObject getJsonComplexTrigger( ScheduleType scheduleType, MonthOfYear month, WeekOfMonth weekOfMonth,
                                              List<DayOfWeek> daysOfWeek, int startHour, int startMin, int startYear
    , int startMonth, int startDay, Date endDate ) {
    JSONObject trigger = new JSONObject();
    trigger.put( "uiPassParam", new JSONString( scheduleEditorWizardPanel.getScheduleType().name() ) ); //$NON-NLS-1$
    if ( month != null ) {
      JSONArray jsonArray = new JSONArray();
      jsonArray.set( 0, new JSONString( Integer.toString( month.ordinal() ) ) );
      trigger.put( "monthsOfYear", jsonArray ); //$NON-NLS-1$
    }
    if ( weekOfMonth != null ) {
      JSONArray jsonArray = new JSONArray();
      jsonArray.set( 0, new JSONString( Integer.toString( weekOfMonth.ordinal() ) ) );
      trigger.put( "weeksOfMonth", jsonArray ); //$NON-NLS-1$
    }
    if ( daysOfWeek != null ) {
      JSONArray jsonArray = new JSONArray();
      int index = 0;

      String targetTimezone = scheduleEditor.getTargetTimezone();
      int weekDayVariance = 0;

      if ( targetTimezone != null ) {
        weekDayVariance = TimeUtil.getDayVariance( startHour, startMin, targetTimezone );
      }

      for ( DayOfWeek dayOfWeek : daysOfWeek ) {
        jsonArray.set( index++, new JSONString( Integer.toString( TimeUtil.getDayOfWeek( dayOfWeek, weekDayVariance ) ) ) );
      }
      trigger.put( "daysOfWeek", jsonArray ); //$NON-NLS-1$
    }
    addJsonStartEnd( trigger, startHour, startMin, startYear, startMonth, startDay, endDate );
    return trigger;
  }

  protected JSONObject getJsonComplexTrigger( ScheduleType scheduleType, MonthOfYear month, int dayOfMonth,
                                              int startHour, int startMin, int startYear
    , int startMonth, int startDay, Date endDate ) {
    JSONObject trigger = new JSONObject();
    trigger.put( "uiPassParam", new JSONString( scheduleEditorWizardPanel.getScheduleType().name() ) ); //$NON-NLS-1$

    if ( month != null ) {
      JSONArray jsonArray = new JSONArray();
      jsonArray.set( 0, new JSONString( Integer.toString( month.ordinal() ) ) );
      trigger.put( "monthsOfYear", jsonArray ); //$NON-NLS-1$
    }

    JSONArray jsonArray = new JSONArray();
    jsonArray.set( 0, new JSONString( Integer.toString( dayOfMonth ) ) );
    trigger.put( "daysOfMonth", jsonArray ); //$NON-NLS-1$

    addJsonStartEnd( trigger, startHour, startMin, startYear, startMonth, startDay, endDate );
    return trigger;
  }

  /**
   * Returns an object suitable for posting into quartz via the the "JOB" rest service.
   *
   * @return
   */
  @SuppressWarnings( "deprecation" )
  public JSONObject getSchedule() {
    ScheduleType scheduleType = scheduleEditorWizardPanel.getScheduleType();
    Date startDate = scheduleEditorWizardPanel.getStartDate();
    String startTime = scheduleEditorWizardPanel.getStartTime();

    // For blockout periods, we need the blockout start time.
    if ( isBlockoutDialog ) {
      startTime = scheduleEditorWizardPanel.getBlockoutStartTime();
    }

    boolean enableSafeMode = scheduleEditorWizardPanel.getEnableSafeMode();
    boolean gatherMetrics = scheduleEditorWizardPanel.getGatherMetrics();
    String logLevel = scheduleEditorWizardPanel.getLogLevel();

    int startHour = getStartHour( startTime );
    int startMin = getStartMin( startTime );
    int startYear = startDate.getYear(); // java.util.Date measures year from 1900
    int startMonth = startDate.getMonth();
    int startDay = startDate.getDate();
    //Date startDateTime = new Date( startYear, startMonth, startDay, startHour, startMin );
    Date endDate = scheduleEditorWizardPanel.getEndDate();
    MonthOfYear monthOfYear = scheduleEditor.getRecurrenceEditor().getSelectedMonth();
    List<DayOfWeek> daysOfWeek = scheduleEditor.getRecurrenceEditor().getSelectedDaysOfWeek();
    Integer dayOfMonth = scheduleEditor.getRecurrenceEditor().getSelectedDayOfMonth();
    WeekOfMonth weekOfMonth = scheduleEditor.getRecurrenceEditor().getSelectedWeekOfMonth();

    JSONObject schedule = new JSONObject();
    schedule.put( "jobName", new JSONString( scheduleName ) );
    if ( appendDateFormat != null ) {
      schedule.put( ScheduleParamsHelper.APPEND_DATE_FORMAT_KEY, new JSONString( appendDateFormat ) );
    }
    schedule.put( ScheduleParamsHelper.OVERWRITE_FILE_KEY, new JSONString( String.valueOf( overwriteFile ) ) );

    if ( scheduleType == ScheduleType.RUN_ONCE ) { // Run once types
      schedule.put( "simpleJobTrigger", getJsonSimpleTrigger( 1, -1, startHour, startMin, startYear, startMonth, startDay, null, enableSafeMode, gatherMetrics, logLevel ) );
    } else if ( ( scheduleType == ScheduleType.SECONDS ) || ( scheduleType == ScheduleType.MINUTES )
      || ( scheduleType == ScheduleType.HOURS ) ) {
      int repeatInterval = 0;
      try { // Simple Trigger Types
        repeatInterval = Integer.parseInt( scheduleEditorWizardPanel.getRepeatInterval() );
      } catch ( Exception e ) {
        // ignored
      }
      schedule.put( "simpleJobTrigger", getJsonSimpleTrigger( -1, repeatInterval, startHour, startMin, startYear, startMonth, startDay, endDate, enableSafeMode, gatherMetrics, logLevel ) );
    } else if ( scheduleType == ScheduleType.DAILY ) {
      if ( scheduleEditor.getRecurrenceEditor().isEveryNDays()
        && !scheduleEditor.getRecurrenceEditor().shouldIgnoreDst()) {
        int repeatInterval = 0;
        try { // Simple Trigger Types
          repeatInterval = Integer.parseInt( scheduleEditorWizardPanel.getRepeatInterval() );
        } catch ( Exception e ) {
          // ignored
        }
        schedule.put( "complexJobTrigger", getJsonComplexTrigger( -1
          , repeatInterval, startHour, startMin, startYear, startMonth, startDay, endDate ) ); //$NON-NLS-1$
      } else if( scheduleEditor.getRecurrenceEditor().isEveryNDays()
        && scheduleEditor.getRecurrenceEditor().shouldIgnoreDst() ) {
        int repeatInterval = 0;
        try { // Simple Trigger Types
          repeatInterval = Integer.parseInt( scheduleEditorWizardPanel.getRepeatInterval() );
        } catch ( Exception e ) {
          // ignored
        }
        schedule.put( "simpleJobTrigger", getJsonSimpleTrigger( -1, repeatInterval
          , startHour, startMin, startYear, startMonth, startDay, endDate, enableSafeMode, gatherMetrics, logLevel ) );
      } else {
        schedule.put("complexJobTrigger", getJsonComplexTrigger( scheduleType, null
          , null, scheduleEditor.getRecurrenceEditor().getSelectedDaysOfWeek()
          , startHour, startMin, startYear, startMonth, startDay, endDate ) );
      }
    } else if ( scheduleType == ScheduleType.CRON ) { // Cron jobs
      schedule.put( "cronJobTrigger", getJsonCronTrigger( scheduleEditor.getCronString(), startHour, startMin, startYear, startMonth, startDay, endDate ) );
    } else if ( ( scheduleType == ScheduleType.WEEKLY ) && ( daysOfWeek.size() > 0 ) ) {
      schedule
        .put(
          "complexJobTrigger", getJsonComplexTrigger( scheduleType, null, null, scheduleEditor.getRecurrenceEditor().getSelectedDaysOfWeek(), startHour, startMin, startYear, startMonth, startDay, endDate ) );
    } else if ( ( scheduleType == ScheduleType.MONTHLY )
      || ( ( scheduleType == ScheduleType.YEARLY ) && ( monthOfYear != null ) ) ) {
      if ( dayOfMonth != null ) {
        // YEARLY Run on specific day in year or MONTHLY Run on specific day in month
        schedule.put( "complexJobTrigger", getJsonComplexTrigger( scheduleType, monthOfYear, dayOfMonth, startHour, startMin, startYear, startMonth, startDay,
          endDate ) );
      } else if ( ( daysOfWeek.size() > 0 ) && ( weekOfMonth != null ) ) {
        // YEARLY
        schedule.put( "complexJobTrigger", getJsonComplexTrigger( scheduleType, monthOfYear, weekOfMonth, daysOfWeek,
          startHour, startMin, startYear, startMonth, startDay, endDate ) );
      }
    }
    schedule.put( "inputFile", new JSONString( filePath ) );
    schedule.put( "outputFile", new JSONString( outputLocation ) );

    if ( scheduleEditorWizardPanel.getTimeZone( ) != null ) {
      schedule.put( "timeZone", new JSONString( scheduleEditorWizardPanel.getTimeZone( ) ) );
    }

    schedule.put( "runSafeMode", new JSONString( String.valueOf( enableSafeMode ) ) );
    schedule.put( "gatheringMetrics", new JSONString( String.valueOf( gatherMetrics ) ) );
    schedule.put( "logLevel", new JSONString( logLevel ) );

    JSONArray jobParameters = new JSONArray();

    if ( !StringUtils.isEmpty( scheduleOwner ) ) {
      jobParameters.set( jobParameters.size(), ScheduleParamsHelper.buildScheduleParam(
        ScheduleParamsHelper.ACTION_USER_KEY, scheduleOwner, "string" ) );
    }

    schedule.put( ScheduleParamsHelper.JOB_PARAMETERS_KEY, jobParameters );

    return schedule;
  }

  @SuppressWarnings( "deprecation" )
  private JSONObject addJsonStartEnd( JSONObject trigger, int startHour, int startMin, int startYear
    , int startMonth, int startDay, Date endDate ) {
    trigger.put( "startHour", new JSONNumber( startHour ) ); //$NON-NLS-1$
    trigger.put( "startMin", new JSONNumber( startMin ) ); //$NON-NLS-1$
    trigger.put( "startYear", new JSONNumber( startYear ) ); //$NON-NLS-1$
    trigger.put( "startMonth", new JSONNumber( startMonth ) ); //$NON-NLS-1$
    trigger.put( "startDay", new JSONNumber( startDay ) ); //$NON-NLS-1$
    if ( endDate != null ) {
      endDate.setHours( 23 );
      endDate.setMinutes( 59 );
      endDate.setSeconds( 59 );
    }
    trigger.put( "endTime", endDate == null ? JSONNull.getInstance() : new JSONString( DateTimeFormat.getFormat(
      PredefinedFormat.ISO_8601 ).format( endDate ) ) );
    return trigger;
  }

  private long calculateBlockoutDuration() {
    long second = 1000;
    long minute = second * 60;
    long hour = minute * 60;
    long day = hour * 24;

    long durationMilli = -1;

    if ( scheduleEditor.getBlockoutEndsType().equals( ScheduleEditor.ENDS_TYPE.TIME ) ) {
      final String startTime = scheduleEditorWizardPanel.getBlockoutStartTime();
      final String endTime = scheduleEditorWizardPanel.getBlockoutEndTime();

      long start = getStartHour( startTime ) * hour + getStartMin( startTime ) * minute;
      long end = getStartHour( endTime ) * hour + getStartMin( endTime ) * minute;

      durationMilli = Math.abs( end - start );

    } else {
      DurationValues durationValues = scheduleEditor.getDurationValues();

      long minutes = durationValues.minutes * minute;
      long hours = durationValues.hours * hour;
      long days = durationValues.days * day;

      durationMilli = minutes + hours + days;
    }

    return durationMilli;
  }

  @SuppressWarnings( "deprecation" )
  public JsJobTrigger getJsJobTrigger() {
    JsJobTrigger jsJobTrigger = JsJobTrigger.instance();

    ScheduleType scheduleType = scheduleEditorWizardPanel.getScheduleType();
    Date startDate = scheduleEditorWizardPanel.getStartDate();
    String startTime = scheduleEditorWizardPanel.getStartTime();

    int startHour = getStartHour( startTime );
    int startMin = getStartMin( startTime );
    int startYear = startDate.getYear();
    int startMonth = startDate.getMonth();
    int startDay = startDate.getDate();
    Date startDateTime = new Date( startYear, startMonth, startDay, startHour, startMin );

    Date endDate = scheduleEditorWizardPanel.getEndDate();
    MonthOfYear monthOfYear = scheduleEditor.getRecurrenceEditor().getSelectedMonth();
    List<DayOfWeek> daysOfWeek = scheduleEditor.getRecurrenceEditor().getSelectedDaysOfWeek();
    Integer dayOfMonth = scheduleEditor.getRecurrenceEditor().getSelectedDayOfMonth();
    WeekOfMonth weekOfMonth = scheduleEditor.getRecurrenceEditor().getSelectedWeekOfMonth();

    if ( isBlockoutDialog ) {
      jsJobTrigger.setBlockDuration( calculateBlockoutDuration() );
    } else {
      // blockDuration is only valid for blockouts
      jsJobTrigger.setBlockDuration( new Long( -1 ) );
    }

    if ( scheduleType == ScheduleType.RUN_ONCE ) { // Run once types
      jsJobTrigger.setType( "simpleJobTrigger" ); //$NON-NLS-1$
      jsJobTrigger.setRepeatInterval( 0 );
      jsJobTrigger.setRepeatCount( 0 );
      jsJobTrigger.setNativeStartTime( DateTimeFormat.getFormat( PredefinedFormat.ISO_8601 ).format( startDateTime ) );
      jsJobTrigger.setTimeZone( scheduleEditorWizardPanel.getTimeZone() );
    } else if ( ( scheduleType == ScheduleType.SECONDS ) || ( scheduleType == ScheduleType.MINUTES )
      || ( scheduleType == ScheduleType.HOURS ) ) {
      int repeatInterval = 0;
      try { // Simple Trigger Types
        repeatInterval = Integer.parseInt( scheduleEditorWizardPanel.getRepeatInterval() );
      } catch ( Exception e ) {
        // ignored
      }
      jsJobTrigger.setType( "simpleJobTrigger" ); //$NON-NLS-1$
      jsJobTrigger.setRepeatInterval( repeatInterval );
      jsJobTrigger.setRepeatCount( -1 );
      jsJobTrigger.setNativeStartTime( DateTimeFormat.getFormat( PredefinedFormat.ISO_8601 ).format( startDateTime ) );
      if ( endDate != null ) {
        jsJobTrigger.setNativeEndTime( DateTimeFormat.getFormat( PredefinedFormat.ISO_8601 ).format( endDate ) );
      }
      jsJobTrigger.setTimeZone( scheduleEditorWizardPanel.getTimeZone() );
    } else if ( scheduleType == ScheduleType.DAILY ) {
      if ( scheduleEditor.getRecurrenceEditor().isEveryNDays() && !scheduleEditor.getRecurrenceEditor().shouldIgnoreDst()) {
        int repeatInterval = 0;
        try { // Simple Trigger Types
          repeatInterval = Integer.parseInt( scheduleEditorWizardPanel.getRepeatInterval() );
        } catch ( Exception e ) {
          // ignored
        }
        jsJobTrigger.setType( "complexTrigger" ); //$NON-NLS-1$
        jsJobTrigger.setCronString("TO_BE_GENERATED");
        jsJobTrigger.setRepeatInterval( repeatInterval );
        jsJobTrigger.setRepeatCount( -1 );
        jsJobTrigger.setNativeStartTime( DateTimeFormat.getFormat( PredefinedFormat.ISO_8601 ).format( startDateTime ) );
        if ( endDate != null ) {
          jsJobTrigger.setNativeEndTime( DateTimeFormat.getFormat( PredefinedFormat.ISO_8601 ).format( endDate ) );
        }
      } else if( scheduleEditor.getRecurrenceEditor().isEveryNDays() && scheduleEditor.getRecurrenceEditor().shouldIgnoreDst()) {
        int repeatInterval = 0;
        try { // Simple Trigger Types
          repeatInterval = Integer.parseInt( scheduleEditorWizardPanel.getRepeatInterval() );
        } catch ( Exception e ) {
          // ignored
        }
        jsJobTrigger.setType( "simpleJobTrigger" ); //$NON-NLS-1$
        jsJobTrigger.setRepeatInterval( repeatInterval );
        jsJobTrigger.setRepeatCount( -1 );
        jsJobTrigger.setNativeStartTime( DateTimeFormat.getFormat( PredefinedFormat.ISO_8601 ).format( startDateTime ) );
        if ( endDate != null ) {
          jsJobTrigger.setNativeEndTime( DateTimeFormat.getFormat( PredefinedFormat.ISO_8601 ).format( endDate ) );
        }
      } else {
        JsArrayInteger jsDaysOfWeek = (JsArrayInteger) JavaScriptObject.createArray();
        int i = 0;
        for ( DayOfWeek dayOfWeek : daysOfWeek ) {
          jsDaysOfWeek.set( i++, dayOfWeek.ordinal() + 1 );
        }
        JsArrayInteger hours = (JsArrayInteger) JavaScriptObject.createArray();
        hours.set( 0, startHour );
        JsArrayInteger minutes = (JsArrayInteger) JavaScriptObject.createArray();
        hours.set( 0, startMin );
        JsArrayInteger seconds = (JsArrayInteger) JavaScriptObject.createArray();
        hours.set( 0, 0 );

        jsJobTrigger.setType( "complexJobTrigger" ); //$NON-NLS-1$
        jsJobTrigger.setDayOfWeekRecurrences( jsDaysOfWeek );
        jsJobTrigger.setHourRecurrences( hours );
        jsJobTrigger.setMinuteRecurrences( minutes );
        jsJobTrigger.setSecondRecurrences( seconds );
        jsJobTrigger.setNativeStartTime( DateTimeFormat.getFormat( PredefinedFormat.ISO_8601 ).format( startDateTime ) );
        if ( endDate != null ) {
          jsJobTrigger.setNativeEndTime( DateTimeFormat.getFormat( PredefinedFormat.ISO_8601 ).format( endDate ) );
        }
      }
      jsJobTrigger.setTimeZone( scheduleEditorWizardPanel.getTimeZone() );
    } else if ( scheduleType == ScheduleType.CRON ) { // Cron jobs
      jsJobTrigger.setType( "cronJobTrigger" ); //$NON-NLS-1$
    } else if ( ( scheduleType == ScheduleType.WEEKLY ) && ( daysOfWeek.size() > 0 ) ) {
      JsArrayInteger jsDaysOfWeek = (JsArrayInteger) JavaScriptObject.createArray();
      int i = 0;
      for ( DayOfWeek dayOfWeek : daysOfWeek ) {
        jsDaysOfWeek.set( i++, dayOfWeek.ordinal() + 1 );
      }
      JsArrayInteger hours = (JsArrayInteger) JavaScriptObject.createArray();
      hours.set( 0, startHour );
      JsArrayInteger minutes = (JsArrayInteger) JavaScriptObject.createArray();
      hours.set( 0, startMin );
      JsArrayInteger seconds = (JsArrayInteger) JavaScriptObject.createArray();
      hours.set( 0, 0 );

      jsJobTrigger.setType( "complexJobTrigger" ); //$NON-NLS-1$
      jsJobTrigger.setDayOfWeekRecurrences( jsDaysOfWeek );
      jsJobTrigger.setHourRecurrences( hours );
      jsJobTrigger.setMinuteRecurrences( minutes );
      jsJobTrigger.setSecondRecurrences( seconds );
      jsJobTrigger.setNativeStartTime( DateTimeFormat.getFormat( PredefinedFormat.ISO_8601 ).format( startDateTime ) );
      if ( endDate != null ) {
        jsJobTrigger.setNativeEndTime( DateTimeFormat.getFormat( PredefinedFormat.ISO_8601 ).format( endDate ) );
      }
      jsJobTrigger.setTimeZone( scheduleEditorWizardPanel.getTimeZone() );
    } else if ( ( scheduleType == ScheduleType.MONTHLY )
      || ( ( scheduleType == ScheduleType.YEARLY ) && ( monthOfYear != null ) ) ) {
      jsJobTrigger.setType( "complexJobTrigger" ); //$NON-NLS-1$

      if ( dayOfMonth != null ) {
        JsArrayInteger jsDaysOfMonth = (JsArrayInteger) JavaScriptObject.createArray();
        jsDaysOfMonth.set( 0, dayOfMonth );

        JsArrayInteger hours = (JsArrayInteger) JavaScriptObject.createArray();
        hours.set( 0, startHour );
        JsArrayInteger minutes = (JsArrayInteger) JavaScriptObject.createArray();
        hours.set( 0, startMin );
        JsArrayInteger seconds = (JsArrayInteger) JavaScriptObject.createArray();
        hours.set( 0, 0 );

        jsJobTrigger.setType( "complexJobTrigger" ); //$NON-NLS-1$
        if ( monthOfYear != null ) {
          JsArrayInteger jsMonthsOfYear = (JsArrayInteger) JavaScriptObject.createArray();
          jsMonthsOfYear.set( 0, monthOfYear.ordinal() + 1 );
          jsJobTrigger.setMonthlyRecurrences( jsMonthsOfYear );
        }
        jsJobTrigger.setDayOfMonthRecurrences( jsDaysOfMonth );
        jsJobTrigger.setHourRecurrences( hours );
        jsJobTrigger.setMinuteRecurrences( minutes );
        jsJobTrigger.setSecondRecurrences( seconds );
        jsJobTrigger.setNativeStartTime( DateTimeFormat.getFormat( PredefinedFormat.ISO_8601 ).format( startDateTime ) );
        if ( endDate != null ) {
          jsJobTrigger.setNativeEndTime( DateTimeFormat.getFormat( PredefinedFormat.ISO_8601 ).format( endDate ) );
        }
      } else if ( ( daysOfWeek.size() > 0 ) && ( weekOfMonth != null ) ) {
        JsArrayInteger jsDaysOfWeek = (JsArrayInteger) JavaScriptObject.createArray();
        int i = 0;
        for ( DayOfWeek dayOfWeek : daysOfWeek ) {
          jsDaysOfWeek.set( i++, dayOfWeek.ordinal() + 1 );
        }

        JsArrayInteger hours = (JsArrayInteger) JavaScriptObject.createArray();
        hours.set( 0, startHour );
        JsArrayInteger minutes = (JsArrayInteger) JavaScriptObject.createArray();
        hours.set( 0, startMin );
        JsArrayInteger seconds = (JsArrayInteger) JavaScriptObject.createArray();
        hours.set( 0, 0 );

        jsJobTrigger.setType( "complexJobTrigger" ); //$NON-NLS-1$
        if ( monthOfYear != null ) {
          JsArrayInteger jsMonthsOfYear = (JsArrayInteger) JavaScriptObject.createArray();
          jsMonthsOfYear.set( 0, monthOfYear.ordinal() + 1 );
          jsJobTrigger.setMonthlyRecurrences( jsMonthsOfYear );
        }
        jsJobTrigger.setHourRecurrences( hours );
        jsJobTrigger.setMinuteRecurrences( minutes );
        jsJobTrigger.setSecondRecurrences( seconds );
        jsJobTrigger.setQualifiedDayOfWeek( daysOfWeek.get( 0 ).name() );
        jsJobTrigger.setDayOfWeekQualifier( weekOfMonth.name() );
        jsJobTrigger.setNativeStartTime( DateTimeFormat.getFormat( PredefinedFormat.ISO_8601 ).format( startDateTime ) );
        if ( endDate != null ) {
          jsJobTrigger.setNativeEndTime( DateTimeFormat.getFormat( PredefinedFormat.ISO_8601 ).format( endDate ) );
        }
      }
      jsJobTrigger.setTimeZone( scheduleEditorWizardPanel.getTimeZone() );
    }
    return jsJobTrigger;
  }

  protected boolean addBlockoutPeriod( final JSONObject schedule, final JsJobTrigger trigger, String urlSuffix ) {
    String url = ScheduleHelper.getPluginContextURL() + "api/scheduler/blockout/" + urlSuffix; //$NON-NLS-1$

    RequestBuilder addBlockoutPeriodRequest = new RequestBuilder( RequestBuilder.POST, url );
    addBlockoutPeriodRequest.setHeader( "accept", "text/plain" ); //$NON-NLS-1$ //$NON-NLS-2$
    addBlockoutPeriodRequest.setHeader( "Content-Type", "application/json" ); //$NON-NLS-1$ //$NON-NLS-2$
    addBlockoutPeriodRequest.setHeader( "If-Modified-Since", "01 Jan 1970 00:00:00 GMT" );

    // Create a unique blockout period name
    final Long duration = trigger.getBlockDuration();
    final String blockoutPeriodName = trigger.getScheduleType() + Random.nextInt() + ":" + //$NON-NLS-1$
      /* PentahoSessionHolder.getSession().getName() */"admin" + ":" + duration; //$NON-NLS-1$ //$NON-NLS-2$

    // Add blockout specific parameters
    JSONObject addBlockoutParams = schedule;
    addBlockoutParams.put( "jobName", new JSONString( blockoutPeriodName ) ); //$NON-NLS-1$
    addBlockoutParams.put( "duration", new JSONNumber( duration ) ); //$NON-NLS-1$
    addBlockoutParams.put( "timeZone", new JSONString( scheduleEditorWizardPanel.getTimeZone() ) );

    try {
      addBlockoutPeriodRequest.sendRequest( addBlockoutParams.toString(), new RequestCallback() {
        @Override
        public void onError( Request request, Throwable exception ) {
          MessageDialogBox dialogBox =
            new MessageDialogBox( Messages.getString( "error" ), exception.toString(), false, false, true ); //$NON-NLS-1$
          dialogBox.center();
          setDone( false );
        }

        @Override
        public void onResponseReceived( Request request, Response response ) {
          if ( response.getStatusCode() == Response.SC_OK ) {
            if ( null != callback ) {
              callback.okPressed();
            }
          }
        }
      } );
    } catch ( RequestException e ) {
      // ignored
    }

    return true;
  }

  private void promptDueToBlockoutConflicts( final boolean alwaysConflict, final boolean conflictsSometimes,
                                             final JSONObject schedule, final JsJobTrigger trigger ) {
    StringBuffer conflictMessage = new StringBuffer();

    final String updateScheduleButtonText = Messages.getString( "blockoutUpdateSchedule" ); //$NON-NLS-1$
    final String continueButtonText = Messages.getString( "blockoutContinueSchedule" ); //$NON-NLS-1$

    boolean showContinueButton = conflictsSometimes;
    boolean isScheduleConflict = alwaysConflict || conflictsSometimes;

    if ( conflictsSometimes ) {
      conflictMessage.append( Messages.getString( "blockoutPartialConflict" ) ); //$NON-NLS-1$
      conflictMessage.append( "\n" ); //$NON-NLS-1$
      conflictMessage.append( Messages.getString( "blockoutPartialConflictContinue" ) ); //$NON-NLS-1$
    } else {
      conflictMessage.append( Messages.getString( "blockoutTotalConflict" ) ); //$NON-NLS-1$
    }

    if ( isScheduleConflict ) {
      final MessageDialogBox dialogBox =
        new MessageDialogBox( Messages.getString( "blockoutTimeExists" ), //$NON-NLS-1$
          conflictMessage.toString(), false, false, true, updateScheduleButtonText, showContinueButton
          ? continueButtonText : null, null );
      dialogBox.setCallback( new IDialogCallback() {
        // If user clicked on 'Continue' we want to add the schedule. Otherwise we dismiss the dialog
        // and they have to modify the recurrence schedule
        @Override
        public void cancelPressed() {
          // User clicked on continue, so we need to proceed adding the schedule
          handleWizardPanels( schedule, trigger );
        }

        @Override
        public void okPressed() {
          // Update Schedule Button pressed
          dialogBox.setVisible( false );
        }
      } );

      dialogBox.center();
    }
  }

  /**
   * Before creating a new schedule, we want to check to see if the schedule that is being created is going to conflict
   * with any one of the blockout periods if one is provisioned.
   *
   * @param schedule
   * @param trigger
   */
  protected void verifyBlockoutConflict( final JSONObject schedule, final JsJobTrigger trigger ) {
    String url = ScheduleHelper.getPluginContextURL() + "api/scheduler/blockout/blockstatus"; //$NON-NLS-1$

    RequestBuilder blockoutConflictRequest = new RequestBuilder( RequestBuilder.POST, url );
    blockoutConflictRequest.setHeader( "accept", "application/json" ); //$NON-NLS-1$ //$NON-NLS-2$
    blockoutConflictRequest.setHeader( "Content-Type", "application/json" ); //$NON-NLS-1$ //$NON-NLS-2$
    blockoutConflictRequest.setHeader( "If-Modified-Since", "01 Jan 1970 00:00:00 GMT" );

    final JSONObject verifyBlockoutParams = schedule;
    verifyBlockoutParams.put( "jobName", new JSONString( scheduleName ) ); //$NON-NLS-1$

    try {
      blockoutConflictRequest.sendRequest( verifyBlockoutParams.toString(), new RequestCallback() {
        @Override
        public void onError( Request request, Throwable exception ) {
          MessageDialogBox dialogBox =
            new MessageDialogBox( Messages.getString( "error" ), exception.toString(), false, false, true ); //$NON-NLS-1$
          dialogBox.center();
          setDone( false );
        }

        @Override
        public void onResponseReceived( Request request, Response response ) {
          if ( response.getStatusCode() == Response.SC_OK ) {
            JsBlockStatus statusResponse =
              (JsBlockStatus) parseJson( JsonUtils.escapeJsonForEval( response.getText() ) );

            // Determine if this schedule conflicts all the time or some of the time
            boolean partiallyBlocked = Boolean.parseBoolean( statusResponse.getPartiallyBlocked() );
            boolean totallyBlocked = Boolean.parseBoolean( statusResponse.getTotallyBlocked() );
            if ( partiallyBlocked || totallyBlocked ) {
              promptDueToBlockoutConflicts( totallyBlocked, partiallyBlocked, schedule, trigger );
            } else {
              // Continue with other panels in the wizard (params, email)
              handleWizardPanels( schedule, trigger );
            }
          } else {
            handleWizardPanels( schedule, trigger );
          }
        }
      } );
    } catch ( RequestException e ) {
      // ignored
    }

    super.nextClicked();
  }

  private void handleWizardPanels( final JSONObject schedule, final JsJobTrigger trigger ) {
    if ( hasParams ) {
      showScheduleParamsDialog( trigger, schedule );
    } else if ( isEmailConfValid ) {
      showScheduleEmailDialog( schedule );
    } else {
      // submit
      JSONObject scheduleRequest = (JSONObject) JSONParser.parseStrict( schedule.toString() );

      if ( editJob != null ) {
        JSONArray scheduleParams = new JSONArray();

        for ( int i = 0; i < editJob.getJobParams().length(); i++ ) {
          JsJobParam param = editJob.getJobParams().get( i );

          scheduleParams.set( i,
            ScheduleParamsHelper.buildScheduleParam( param.getName(), param.getValue(), "string" ) );
        }

        scheduleRequest.put( ScheduleParamsHelper.JOB_PARAMETERS_KEY, scheduleParams );

        String actionClass = editJob.getJobParamValue( "ActionAdapterQuartzJob-ActionClass" );
        if ( !StringUtils.isEmpty( actionClass ) ) {
          scheduleRequest.put( "actionClass", new JSONString( actionClass ) );
        }

      }

      // Handle Schedule Parameters
      JSONArray scheduleParams = ScheduleParamsHelper.getScheduleParams( scheduleRequest );
      scheduleRequest.put( ScheduleParamsHelper.JOB_PARAMETERS_KEY, scheduleParams );

      RequestBuilder scheduleFileRequestBuilder = ScheduleHelper.buildRequestForJob( editJob, scheduleRequest );

      try {
        scheduleFileRequestBuilder.sendRequest( scheduleRequest.toString(), new RequestCallback() {
          @Override
          public void onError( Request request, Throwable exception ) {
            MessageDialogBox dialogBox =
              new MessageDialogBox( Messages.getString( "error" ), exception.toString(), false, false, true );
            dialogBox.center();
            setDone( false );
          }

          @Override
          public void onResponseReceived( Request request, Response response ) {
            if ( response.getStatusCode() == 200 ) {
              setDone( true );
              ScheduleRecurrenceDialog.this.hide();
              if ( callback != null ) {
                callback.okPressed();
              }
            } else {
              String message = response.getText();
              if ( StringUtils.isEmpty( message ) ) {
                message = Messages.getString( "serverErrorColon" ) + " " + response.getStatusCode();
              }

              MessageDialogBox dialogBox = new MessageDialogBox( Messages.getString( "error" ), message,
                false, false, true );

              dialogBox.center();
              setDone( false );
            }
          }
        } );
      } catch ( RequestException e ) {
        MessageDialogBox dialogBox = new MessageDialogBox( Messages.getString( "error" ), e.toString(),
          false, false, true );
        dialogBox.center();
        setDone( false );
      }

      setDone( true );
    }
  }

  private final native JavaScriptObject parseJson( String json )
  /*-{
      if (null == json || "" == json) {
          return null;
      }
      var obj = JSON.parse(json);
      return obj;
  }-*/;

  /*
   * (non-Javadoc)
   *
   * @see org.pentaho.gwt.widgets.client.wizards.AbstractWizardDialog#finish()
   */
  @Override
  protected boolean onFinish() {
    if ( !super.enableFinish( getIndex() ) ) {
      return false;
    }
    // DO NOT DELETE - verifyBlockoutConflict(schedule, trigger);
    JsJobTrigger trigger = getJsJobTrigger();
    JSONObject schedule = getSchedule();

    handleWizardPanels( schedule, trigger );
    return true;
  }

  private void showScheduleEmailDialog( final JSONObject schedule ) {
    try {
      final String url = EnvironmentHelper.getFullyQualifiedURL() + "api/mantle/isAuthenticated"; //$NON-NLS-1$
      RequestBuilder requestBuilder = new RequestBuilder( RequestBuilder.GET, url );
      requestBuilder.setHeader( "accept", "text/plain" ); //$NON-NLS-1$ //$NON-NLS-2$
      requestBuilder.setHeader( "If-Modified-Since", "01 Jan 1970 00:00:00 GMT" );
      requestBuilder.sendRequest( null, new RequestCallback() {

        @Override
        public void onError( Request request, Throwable caught ) {
          MantleLoginDialog.performLogin( new AsyncCallback<Boolean>() {

            @Override
            public void onFailure( Throwable caught ) {
            }

            @Override
            public void onSuccess( Boolean result ) {
              showScheduleEmailDialog( schedule );
            }
          } );
        }

        @Override
        public void onResponseReceived( Request request, Response response ) {
          JSONObject scheduleRequest = (JSONObject) JSONParser.parseStrict( schedule.toString() );
          if ( scheduleEmailDialog == null ) {
            scheduleEmailDialog = ScheduleFactory.getInstance().createScheduleEmailDialog( ScheduleRecurrenceDialog.this, filePath, scheduleRequest, null, editJob );
            scheduleEmailDialog.setCallback( callback );
          } else {
            scheduleEmailDialog.setJobSchedule( scheduleRequest );
          }
          scheduleEmailDialog.setNewSchedule( newSchedule );
          scheduleEmailDialog.center();
          hide();
        }

      } );
    } catch ( RequestException e ) {
      Window.alert( e.getMessage() );
    }

  }

  private void showScheduleParamsDialog( final JsJobTrigger trigger, final JSONObject schedule ) {
    try {
      final String url = EnvironmentHelper.getFullyQualifiedURL() + "api/mantle/isAuthenticated"; //$NON-NLS-1$
      RequestBuilder requestBuilder = new RequestBuilder( RequestBuilder.GET, url );
      requestBuilder.setHeader( "accept", "text/plain" ); //$NON-NLS-1$ //$NON-NLS-2$
      requestBuilder.setHeader( "If-Modified-Since", "01 Jan 1970 00:00:00 GMT" );
      requestBuilder.sendRequest( null, new RequestCallback() {

        @Override
        public void onError( Request request, Throwable caught ) {
          MantleLoginDialog.performLogin( new AsyncCallback<Boolean>() {

            @Override
            public void onFailure( Throwable caught ) {
            }

            @Override
            public void onSuccess( Boolean result ) {
              showScheduleParamsDialog( trigger, schedule );
            }
          } );
        }

        @Override
        public void onResponseReceived( Request request, Response response ) {
          if ( scheduleParamsDialog == null ) {
            scheduleParamsDialog = ScheduleFactory.getInstance()
              .createScheduleParamsDialog( ScheduleRecurrenceDialog.this, isEmailConfValid, editJob );
            scheduleParamsDialog.setCallback( callback );
          } else {
            scheduleParamsDialog.setJobSchedule( schedule );
          }
          scheduleParamsDialog.setNewSchedule( newSchedule );
          scheduleParamsDialog.center();
          hide();
        }

      } );
    } catch ( RequestException e ) {
      Window.alert( e.getMessage() );
    }

  }

  /**
   * @param startTime
   * @return
   */
  private int getStartMin( String startTime ) {
    if ( startTime == null || startTime.length() < 1 ) {
      return 0;
    }
    int firstSeparator = startTime.indexOf( ':' );
    int secondSeperator = startTime.indexOf( ':', firstSeparator + 1 );
    int min = Integer.parseInt( startTime.substring( firstSeparator + 1, secondSeperator ) );
    return min;
  }

  /**
   * @param startTime
   * @return
   */
  private int getStartHour( String startTime ) {
    if ( startTime == null || startTime.length() < 1 ) {
      return 0;
    }
    int afternoonOffset = startTime.endsWith( TimeUtil.TimeOfDay.PM.toString() ) ? 12 : 0; //$NON-NLS-1$
    int hour = Integer.parseInt( startTime.substring( 0, startTime.indexOf( ':' ) ) );
    hour += afternoonOffset;
    return hour;
  }

  public Boolean getDone() {
    return done;
  }

  public void setDone( Boolean done ) {
    this.done = done;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.pentaho.gwt.widgets.client.wizards.AbstractWizardDialog#onNext(org.pentaho.gwt.widgets.client.wizards.
   * IWizardPanel, org.pentaho.gwt.widgets.client.wizards.IWizardPanel)
   */
  @Override
  protected boolean onNext( IWizardPanel nextPanel, IWizardPanel previousPanel ) {
    return super.enableNext( getIndex() );
  }

  /*
   * (non-Javadoc)
   *
   * @see org.pentaho.gwt.widgets.client.wizards.AbstractWizardDialog#onPrevious(org.pentaho.gwt.widgets.client.wizards
   * .IWizardPanel, org.pentaho.gwt.widgets.client.wizards.IWizardPanel)
   */
  @Override
  protected boolean onPrevious( IWizardPanel previousPanel, IWizardPanel currentPanel ) {
    return true;
  }

  @Override
  protected void backClicked() {
    hide();
    if ( parentDialog != null ) {
      parentDialog.center();
    }
  }

  @Override
  public void center() {
    super.center();
    Timer t = new Timer() {
      @Override
      public void run() {
        if ( scheduleEditorWizardPanel.isAttached() && scheduleEditorWizardPanel.isVisible() ) {
          cancel();
        }
      }
    };
    t.scheduleRepeating( 250 );
  }

  @Override
  protected boolean enableBack( int index ) {
    return parentDialog != null;
  }

  @Override
  protected boolean showBack( int index ) {
    return parentDialog != null;
  }

  @Override
  protected boolean showFinish( int index ) {
    return true;
  }

  @Override
  protected boolean showNext( int index ) {
    return false;
  }

  public void setCallback( IDialogCallback callback ) {
    this.callback = callback;
  }

  public IDialogCallback getCallback() {
    return callback;
  }

  public boolean isShowSuccessDialog() {
    return showSuccessDialog;
  }

  public void setShowSuccessDialog( boolean showSuccessDialog ) {
    this.showSuccessDialog = showSuccessDialog;
  }

  public void setNewSchedule( boolean newSchedule ) {
    this.newSchedule = newSchedule;
  }
}
