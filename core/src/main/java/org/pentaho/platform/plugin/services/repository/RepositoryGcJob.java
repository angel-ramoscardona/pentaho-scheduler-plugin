/*!
 *
 * This program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License, version 2.1 as published by the Free Software
 * Foundation.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, you can obtain a copy at http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * or from the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 *
 * Copyright (c) 2024 Hitachi Vantara. All rights reserved.
 *
 */

package org.pentaho.platform.plugin.services.repository;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openide.util.NotImplementedException;
import org.pentaho.platform.api.action.IAction;

/**
 * @author Andrey Khayrutdinov
 */
public class RepositoryGcJob implements IAction {
  public static final String JOB_NAME = "RepositoryGcJob";

  private static final Log logger = LogFactory.getLog( RepositoryGcJob.class );

  @Override
  public void execute() throws Exception {
//    logger.info( "Starting repository GC" );
//    new RepositoryCleaner().gc();
//    logger.info( "Repository GC has been finished" );
    throw new NotImplementedException();
  }
}
