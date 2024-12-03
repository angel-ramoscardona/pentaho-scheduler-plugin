/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2028-08-13
 ******************************************************************************/


package org.pentaho.platform.plugin.services.exporter;

import org.apache.commons.lang.ArrayUtils;
import org.pentaho.platform.api.scheduler2.ComplexJobTrigger;
import org.pentaho.platform.api.scheduler2.CronJobTrigger;
import org.pentaho.platform.api.scheduler2.IBlockoutManager;
import org.pentaho.platform.api.scheduler2.IScheduler;
import org.pentaho.platform.api.scheduler2.Job;
import org.pentaho.platform.api.scheduler2.SchedulerException;
import org.pentaho.platform.api.scheduler2.SimpleJobTrigger;
import org.pentaho.platform.api.util.IExportHelper;
import org.pentaho.platform.api.util.IPentahoPlatformExporter;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.plugin.services.importexport.exportManifest.ExportManifest;
import org.pentaho.platform.plugin.services.messages.Messages;
import org.pentaho.platform.repository.RepositoryFilenameUtils;
import org.pentaho.platform.web.http.api.resources.JobScheduleParam;
import org.pentaho.platform.web.http.api.resources.JobScheduleRequest;
import org.pentaho.platform.web.http.api.resources.RepositoryFileStreamProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScheduleExportUtil implements IExportHelper {
  private static final Logger log = LoggerFactory.getLogger( ScheduleExportUtil.class );

  public static final String RUN_PARAMETERS_KEY = "parameters";

  private ExportManifest exportManifest;

  public ScheduleExportUtil() {
    // to get 100% coverage
  }

  public void registerAsHelper() {
    PentahoSystem.get( IPentahoPlatformExporter.class, "IPentahoPlatformExporter", null ).addExportHelper( this );
  }
  public static JobScheduleRequest createJobScheduleRequest( Job job ) {
    if ( job == null ) {
      throw new IllegalArgumentException(
        Messages.getInstance().getString( "ScheduleExportUtil.JOB_MUST_NOT_BE_NULL" ) );
    }
    IScheduler scheduler = PentahoSystem.get( IScheduler.class, "IScheduler2", null ); //$NON-NLS-1$
    assert scheduler != null;
    JobScheduleRequest schedule = (JobScheduleRequest) scheduler.createJobScheduleRequest();
    schedule.setJobName( job.getJobName() );
    schedule.setDuration( job.getJobTrigger().getDuration() );
    schedule.setJobState( job.getState() );

    Map<String, Object> jobParams = job.getJobParams();

    Object streamProviderObj = jobParams.get( IScheduler.RESERVEDMAPKEY_STREAMPROVIDER );
    RepositoryFileStreamProvider streamProvider = null;
    if ( streamProviderObj instanceof RepositoryFileStreamProvider ) {
      streamProvider = (RepositoryFileStreamProvider) streamProviderObj;
    } else if ( streamProviderObj instanceof String ) {
      String inputFilePath = null;
      String outputFilePath = null;
      String inputOutputString = (String) streamProviderObj;
      String[] tokens = inputOutputString.split( ":" );
      if ( !ArrayUtils.isEmpty( tokens ) && tokens.length == 2 ) {
        inputFilePath = tokens[ 0 ].split( "=" )[ 1 ].trim();
        outputFilePath = tokens[ 1 ].split( "=" )[ 1 ].trim();

        streamProvider = new RepositoryFileStreamProvider( inputFilePath, outputFilePath, true );
      }
    }

    if ( streamProvider != null ) {
      schedule.setInputFile( streamProvider.getInputFilePath() );
      schedule.setOutputFile( streamProvider.getOutputFilePath() );
    } else {
      // let's look to see if we can figure out the input and output file
      String directory = (String) jobParams.get( "directory" );
      String transName = (String) jobParams.get( "transformation" );
      String jobName = (String) jobParams.get( "job" );
      String artifact = transName == null ? jobName : transName;

      if ( directory != null && artifact != null ) {
        String outputFile = RepositoryFilenameUtils.concat( directory, artifact );
        outputFile += "*";

        if ( artifact.equals( jobName ) ) {
          artifact += ".kjb";
        } else {
          artifact += ".ktr";
        }
        String inputFile = RepositoryFilenameUtils.concat( directory, artifact );
        schedule.setInputFile( inputFile );
        schedule.setOutputFile( outputFile );
      }
    }

    for ( String key : jobParams.keySet() ) {
      Object object = jobParams.get( key );
      if ( RUN_PARAMETERS_KEY.equals( key ) ) {
        if ( schedule.getPdiParameters() == null ) {
          schedule.setPdiParameters( new HashMap<String, String>() );
        }
        schedule.getPdiParameters().putAll( (Map<String, String>) object );
      } else {
        JobScheduleParam param = null;
        if ( object instanceof String ) {
          String value = (String) object;
          if ( IScheduler.RESERVEDMAPKEY_ACTIONCLASS.equals( key ) ) {
            schedule.setActionClass( value );
          } else if ( IBlockoutManager.TIME_ZONE_PARAM.equals( key ) ) {
            schedule.setTimeZone( value );
          }
          param = new JobScheduleParam( key, (String) object );
        } else if ( object instanceof Number ) {
          param = new JobScheduleParam( key, (Number) object );
        } else if ( object instanceof Date ) {
          param = new JobScheduleParam( key, (Date) object );
        } else if ( object instanceof Boolean ) {
          param = new JobScheduleParam( key, (Boolean) object );
        }
        if ( param != null ) {
          schedule.getJobParameters().add(param);
        }
      }
    }

    if ( job.getJobTrigger() instanceof SimpleJobTrigger ) {
      SimpleJobTrigger jobTrigger = (SimpleJobTrigger) job.getJobTrigger();
      schedule.setSimpleJobTrigger( jobTrigger );

    } else if ( job.getJobTrigger() instanceof ComplexJobTrigger ) {
      ComplexJobTrigger jobTrigger = (ComplexJobTrigger) job.getJobTrigger();
      // force it to a cron trigger to get the auto-parsing of the complex trigger
      CronJobTrigger cron = (CronJobTrigger) scheduler.createCronJobTrigger();
      cron.setCronString( jobTrigger.getCronString() );
      cron.setStartTime( jobTrigger.getStartTime() );
      cron.setEndTime( jobTrigger.getEndTime() );
      cron.setDuration( jobTrigger.getDuration() );
      cron.setUiPassParam( jobTrigger.getUiPassParam() );
      schedule.setCronJobTrigger( cron );
    } else if ( job.getJobTrigger() instanceof CronJobTrigger ) {
      CronJobTrigger jobTrigger = (CronJobTrigger) job.getJobTrigger();
      schedule.setCronJobTrigger( jobTrigger );
    } else {
      // don't know what this is, can't export it
      throw new IllegalArgumentException( Messages.getInstance().getString(
        "PentahoPlatformExporter.UNSUPPORTED_JobTrigger", job.getJobTrigger().getClass().getName() ) );

    }
    if ( null != job.getJobTrigger() && null != job.getJobTrigger().getTimeZone() ) {
      schedule.setTimeZone( job.getJobTrigger().getTimeZone() );
    }
    return (JobScheduleRequest) schedule;
  }

  protected void exportSchedules() {
    log.debug( "export schedules" );
    try {
      List<Job> jobs = (List<Job>)(List<?>) PentahoSystem.get( IScheduler.class, "IScheduler2", null ).getJobs( null );
      for ( Job job : jobs ) {
        if ( job.getJobName().equals( "PentahoSystemVersionCheck" ) ) {
          // don't bother exporting the Version Checker schedule, it gets created automatically on server start
          // if it doesn't exist and fails if you try to import it due to a null ActionClass
          continue;
        }
        try {
          JobScheduleRequest scheduleRequest = ScheduleExportUtil.createJobScheduleRequest( job );
          exportManifest.addSchedule( scheduleRequest );
        } catch ( IllegalArgumentException e ) {
          log.warn( e.getMessage(), e );
        }
      }
    } catch ( SchedulerException e ) {
      log.error( Messages.getInstance().getString( "PentahoPlatformExporter.ERROR_EXPORTING_JOBS" ), e );
    }
  }

  @Override public void doExport( Object exportArg ) {
    PentahoPlatformExporter exporter = (PentahoPlatformExporter) exportArg;
    exportManifest = exporter.getExportManifest();
    exportSchedules();
  }
}
