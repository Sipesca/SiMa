/*
 * Copyright (C) 2013  Antonio Fernández Ares (antares.es@gmail.com)
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
package SincronizarFusionTables;

import Entorno.Depuracion.Debug;
import Entorno.Configuracion.Config;
import Entorno.Estadisticas.Estadisticas;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Clase encargada de ejecutar el temporizador para el cálculo y subida de datos actualizados a Google Fusion Tables
 *
 * @author Antonio Fernández Ares (antares.es@gmail.com)
 */
public class ActualizadorFT{

  static Config _c = new Config();
  Debug _d = new Debug();
  //Tarea temporizada
  TimerTask temporizador;

  private int conta_nodos = 0;
  
  private final int MAX_conta_nodos  = _c.getInt("ft.veces_sincronizacion_nodos");;
  
  public ActualizadorFT() {


    this.temporizador = new TimerTask() {
      @Override
      public void run() {

        Logger.getGlobal().log(Level.INFO, "Comenzando sincronizado programado en la nube.");
        
        if(conta_nodos == MAX_conta_nodos){
        Nodos n = new Nodos();
        n.calcular();

        Logger.getGlobal().log(Level.INFO, "Nodos sincronizados");
        conta_nodos = 0;
        }else{
          conta_nodos++;
        }
           
        PasosPorHoras h = new PasosPorHoras();
        Logger.getGlobal().log(Level.INFO, "Sincronizando Pasos desde " + h.getFecha());
        h.check = true;
        h.calcular();
        
        

        TrazasPorHoras t = new TrazasPorHoras();
        Logger.getGlobal().log(Level.INFO, "Pasos sincronizados desde " + t.getFecha());
        t.check = true;
        t.calcular();
        
        Logger.getGlobal().log(Level.INFO, "Trazas sincronizadas.");

        
        Logger.getGlobal().log(Level.INFO, "Sincronizado programado en la nube programado COMPLETO.");

        Estadisticas.prime();
        
      }
    };
  }

  public void start() {
    
    try {
      //Añadimos un delay al comienzo del sincronizado para dar tiempo al sincronizador local. Se puede sacar con un
      //Notify, pero no está aún implementado :)
      wait(1000*60*15);
    } catch (InterruptedException ex) {
      Logger.getLogger(ActualizadorFT.class.getName()).log(Level.SEVERE, null, ex);
    }
    
    if (_c.getBool("ft.primera_vez")) {

      Logger.getGlobal().log(Level.INFO, "Comenzando sincronizado forzado en la nube");

      
      //Si es nuestra primera vez, habrá que hacerlo desde el origen de los tiempos!
      Nodos n = new Nodos();
      n.calcular();

      Logger.getGlobal().log(Level.INFO, "Nodos sincronizados");
      
      PasosPorHoras h = new PasosPorHoras("2013-10-01 00:00:00");
      h.check = true;
      h.calcular();

      Logger.getGlobal().log(Level.INFO, "Pasos sincronizados");
      
      TrazasPorHoras t = new TrazasPorHoras("2013-10-01 00:00:00");
      t.check = false;
      t.calcular();

      Logger.getGlobal().log(Level.INFO, "Trazas sincronizadas");
      
//      PasosPorDias p = new PasosPorDias("2012-10-31 11:15:40");
//      p.check = false; 
//      p.calcular();

      _c.set("ft.primera_vez", "false");
      
     Logger.getGlobal().log(Level.INFO, "Sincronizado forzado en la nube programado COMPLETO.");

    }

    Timer timer = new Timer("ActualizadorFusionTable", true);
    timer.scheduleAtFixedRate(temporizador, _c.getLong("ft.periodo_actualizacion"), _c.getLong("ft.periodo_actualizacion"));

  }
}
