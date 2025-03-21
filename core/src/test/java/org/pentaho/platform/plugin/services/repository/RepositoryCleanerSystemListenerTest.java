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


package org.pentaho.platform.plugin.services.repository;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.pentaho.platform.api.scheduler2.IJob;
import org.pentaho.platform.api.scheduler2.IJobFilter;
import org.pentaho.platform.api.scheduler2.IJobTrigger;
import org.pentaho.platform.api.scheduler2.IScheduler;
import org.pentaho.platform.api.scheduler2.Job;
import org.pentaho.platform.api.scheduler2.SchedulerException;
import org.pentaho.platform.plugin.services.repository.RepositoryCleanerSystemListener.Frequency;
import org.pentaho.test.platform.engine.core.MicroPlatform;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Andrey Khayrutdinov
 */
public class RepositoryCleanerSystemListenerTest {

  private MicroPlatform mp;
  private IScheduler scheduler;
  private RepositoryCleanerSystemListener listener;

  @Before
  public void setUp() throws Exception {
    scheduler = mock( IScheduler.class );
    listener = new RepositoryCleanerSystemListener();
  }

  @After
  public void tearDown() throws Exception {
    if ( mp != null ) {
      mp.stop();
      mp = null;
    }
    scheduler = null;
    listener = null;
  }


  @Test
  public void gcEnabledIsTrue_executeIsNull_ByDefault() {
    assertTrue( listener.isGcEnabled() );
    assertNull( listener.getExecute() );
  }

  @Test
  public void stops_IfSchedulerIsNotDefined() {
    assertFalse( listener.startup( null ) );
  }

  private void prepareMp() throws Exception {
    mp = new MicroPlatform();
    mp.defineInstance( IScheduler.class, scheduler );
    mp.start();
  }

  private void verifyJobRemoved( String jobId ) throws SchedulerException {
    verify( scheduler ).removeJob( jobId );
  }

  private void verifyJobCreated( Frequency frequency ) throws SchedulerException {
    verify( scheduler ).createJob( eq( RepositoryGcJob.JOB_NAME ), eq( RepositoryGcJob.class ), ArgumentMatchers.nullable( Map.class ),
      isA( frequency.createTrigger().getClass() ) );
  }


  private void verifyJobHaveNotCreated() throws SchedulerException {
    verify( scheduler, never() )
      .createJob( eq( RepositoryGcJob.JOB_NAME ), eq( RepositoryGcJob.class ), anyMap(), any( IJobTrigger.class ) );
  }


  @Test
  public void returnsTrue_EvenGetsExceptions() throws Exception {
    when( scheduler.getJobs( any( IJobFilter.class ) ) ).thenThrow( new SchedulerException( "test exception" ) );
    prepareMp();
    assertTrue( "The listener should not return false to let the system continue working", listener.startup( null ) );
  }


  @Test
  public void removesJobs_WhenDisabled() throws Exception {
    final String jobId = "jobId";
    Job job = new Job();
    job.setJobId( jobId );
    when( scheduler.getJobs( any( IJobFilter.class ) ) ).thenReturn( Collections.singletonList( job ) );

    prepareMp();

    listener.setGcEnabled( false );

    assertTrue( listener.startup( null ) );
    verifyJobRemoved( jobId );
  }

  @Test
  public void schedulesJob_Now() throws Exception {
    testSchedulesJob( Frequency.NOW );
  }

  @Test
  public void schedulesJob_Weekly() throws Exception {
    testSchedulesJob( Frequency.WEEKLY );
  }

  @Test
  public void schedulesJob_Monthly() throws Exception {
    testSchedulesJob( Frequency.MONTHLY );
  }


  private void testSchedulesJob( Frequency frequency ) throws Exception {
//    when( scheduler.getJobs( any( IJobFilter.class ) ) ).thenReturn( Collections.<IJob>emptyList() );
//    prepareMp();
//    listener.setExecute( frequency.getValue() );
//
//    assertTrue( listener.startup( null ) );
//    verifyJobCreated( frequency );
  }

  @Test
  public void schedulesJob_Unknown() throws Exception {
    testSchedulesJob_IncorrectExecute( "unknown" );
  }

  @Test
  public void schedulesJob_Null() throws Exception {
    testSchedulesJob_IncorrectExecute( null );
  }

  private void testSchedulesJob_IncorrectExecute( String execute ) throws Exception {
    when( scheduler.getJobs( any( IJobFilter.class ) ) ).thenReturn( Collections.<IJob>emptyList() );
    prepareMp();
    listener.setExecute( execute );

    listener.startup( null );
    verifyJobHaveNotCreated();
  }


  @Test
  @Ignore
  public void reschedulesJob_IfFoundDifferent() throws Exception {
    final String oldJobId = "oldJobId";

    Job oldJob = (Job) scheduler.createJob( null, (String)null , null, null );
    oldJob.setJobTrigger( scheduler.createCronJobTrigger() );
    oldJob.setJobId( oldJobId );
    when( scheduler.getJobs( any( IJobFilter.class ) ) ).thenReturn( Collections.singletonList( oldJob ) );

    prepareMp();

    listener.setExecute( Frequency.NOW.getValue() );

    assertTrue( listener.startup( null ) );
    verifyJobRemoved( oldJobId );
    verifyJobCreated( Frequency.NOW );
  }

  @Test
  @Ignore
  public void doesNotRescheduleJob_IfFoundSame() throws Exception {
    final String oldJobId = "oldJobId";
    Job oldJob = (Job) scheduler.createJob( null, (String)null , null, null );
    oldJob.setJobTrigger( Frequency.WEEKLY.createTrigger() );
    oldJob.setJobId( oldJobId );
    when( scheduler.getJobs( any( IJobFilter.class ) ) ).thenReturn( Collections.singletonList( oldJob ) );

    prepareMp();

    listener.setExecute( Frequency.WEEKLY.getValue() );

    assertTrue( listener.startup( null ) );
    verify( scheduler, never() ).removeJob( oldJobId );
    verifyJobHaveNotCreated();
  }
}
