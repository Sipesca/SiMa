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
package Entorno.Estadisticas;

import java.util.logging.Logger;

/**
 *
 * @author antares
 */
public class Estadisticas {
  
  public static int PETICIONES_FT_GENERADAS = 0;
  public static int PETICIONES_FT_EXITO = 0;
  public static int PETICIONES_FT_REPETIDAS = 0;
  public static int PETICIONES_FT_CUOTA = 0;
  public static int PETICIONES_FT_RAPIDO = 0;
   
  public static int PETICIONES_DB = 0;
  
  public static int DISPOSITIVOS_PROCESADOS = 0;
  public static int DISPOSITIVOS_INSERTADOS = 0;
  
  public static int PASOS_PROCESADOS = 0;
  public static int PASOS_INSERTADOS = 0;
  
  public String toString(){
    return "Peticiones_FT: " + PETICIONES_FT_GENERADAS;
  }
  
  public static void prime(){
    Logger.getGlobal().info("Estadísticas: Peticiones a FT generadas: "+ PETICIONES_FT_GENERADAS);
    Logger.getGlobal().info("Estadísticas: Peticiones a FT exitosas: "+ PETICIONES_FT_EXITO);
    Logger.getGlobal().info("Estadísticas: Peticiones a FT repetidas: "+ PETICIONES_FT_REPETIDAS);
    
    Logger.getGlobal().info("Estadísticas: Peticiones a DB generadas: "+ PETICIONES_DB);
    
    Logger.getGlobal().info("Estadísticas: Dispositivos Procesados: "+ DISPOSITIVOS_PROCESADOS);
    Logger.getGlobal().info("Estadísticas: Dispositivos Insertados: "+ DISPOSITIVOS_INSERTADOS);
    
    Logger.getGlobal().info("Estadísticas: Pasos Procesados: "+ PASOS_PROCESADOS);
    Logger.getGlobal().info("Estadísticas: Pasos Insertados: "+ PASOS_INSERTADOS);
    
    
  }
  
  
  
}
