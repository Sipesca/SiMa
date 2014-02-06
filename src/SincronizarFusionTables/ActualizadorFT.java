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
import java.util.TimerTask;

/**
 * Clase encargada de ejecutar el temporizador para el cálculo y subida de datos actualizados a Google Fusion Tables
 *
 * @author Antonio Fernández Ares (antares.es@gmail.com)
 */
public class ActualizadorFT {

  Config _c = new Config();
  Debug _d = new Debug();
  //Tarea temporizada
  TimerTask temporizador;

  public ActualizadorFT() {
    
    
    this.temporizador = new TimerTask() {
      @Override
      public void run() {
        PasosPorDia p = new PasosPorDia();
        p.check = true;
        p.calcular();
        
        PasosPorHoras h = new PasosPorHoras();
        h.check = true;
        h.calcular();
        
        TrazasPorHoras t = new TrazasPorHoras();
        t.check = true;
        t.calcular();
      }
    };
  }

    public void start() {
    
      TrazasPorHoras t = new TrazasPorHoras("2012-10-31 11:15:40");
      t.check = false; 
      t.calcular();
      
      /*
      
     if(_c.getBool("ft.primera_vez")){
       //PasosPorDia p = new PasosPorDia("2012-10-31 11:15:40");
       //p.check = false; 
       //p.calcular();
       
       PasosPorHoras h = new PasosPorHoras("2012-10-31 11:15:40");
       h.check = false; 
       h.calcular();
       
      _c.set("ft.primera_vez", "false");
     }
      
    Timer timer = new Timer("ActualizadorFusionTable", true);
    timer.scheduleAtFixedRate(temporizador, _c.getLong("ft.periodo_actualizacion"), _c.getLong("ft.periodo_actualizacion"));

    */
  }
}
