/*
 * Copyright (C) 2014 antares
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package Entorno.Depuracion;

import java.util.Date;
import java.util.logging.LogRecord;



/**
 * Clase que indica el formato de salida del fichero de log
 * Basado en la clase java.util.logging.SimpleFormatter
 * @author antares
 */
public class formatoLog extends java.util.logging.SimpleFormatter {

    private final Date dat = new Date();
    
  
  @Override
  public  synchronized String format(LogRecord record) {
       dat.setTime(record.getMillis());
       
       return Debug.sdf.format(dat) + "\t" + record.getLevel().getName() +"\t" + record.getSourceClassName() + "\t" + record.getSourceMethodName() + "\t" + record.getMessage() +   "\n" ;
       
    }

  
  
  
}


