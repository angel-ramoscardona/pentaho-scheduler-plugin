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


package org.pentaho.platform.scheduler2.ws;

import java.util.HashMap;
import javax.xml.bind.annotation.XmlRootElement;

@SuppressWarnings( "serial" )
@XmlRootElement
public class MapParamValue extends HashMap<String, String> implements ParamValue {
}
