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

import Entorno.Configuracion.Config;
import Entorno.Depuracion.Debug;
import Entorno.Conectar.Conectar;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Clase encargada del cálculo y la subida de los
 *
 * @author Antonio Fernández Ares (antares.es@gmail.com)
 */
public class Nodos {

  private Config _c = new Config();
  private Debug _d = new Debug();
  private conectarFusionTables cFT = new conectarFusionTables();
  private String fecha;
  private ResultSet rs;
  private final String TABLAID = _c.get("ft.NODOS.ID");
  private final List<String> campos = new ArrayList<>();
  public boolean check = true; //En el caso de los nodos, actualizamos SIEMPRE

  public Nodos() {
    campos.add("idNodo");
    campos.add("latitud");
    campos.add("longitud");
    campos.add("nombre");
    campos.add("poligono");
    campos.add("tipo");
  }

  public boolean calcular() {
    Conectar conectar = new Conectar();
    try {
      Statement st = conectar.crearSt();
      rs = st.executeQuery("select * from nodo");

      List<String> valores = new ArrayList<>();

      while (rs.next()) {
        //System.err.println("->" + rs.getString(1) + " " + rs.getString(2) + " " + rs.getString(3));
        valores.add(rs.getString(1)); //idNodo
        valores.add(rs.getString(2)); //latitud
        valores.add(rs.getString(3)); //longitud
        valores.add(rs.getString(4)); //nombre
        valores.add(rs.getString(5)); //poligono
        
        if(rs.getString("nombre").toLowerCase().contains("wifi")){
          valores.add("Wifi");
        }else if(rs.getString("nombre").toLowerCase().contains("wi-fi")){
          valores.add("Wifi");
        }else{
          valores.add("Bluetooth");
        }

        cFT.insert(TABLAID, campos, valores, check, 1);
        //Los nodos son especiales, dado que incorporan mucha más información, por lo que es recomendables no usar el
        //mecanismo de caché ya que se corre el riesgo de desbordar el tamaño de la query.
        //Es por ello, que forzamos la sincronización despúes de cada insercción
        cFT.forzarSync();
        valores.clear();
      }

      cFT.forzarSync();
      cFT.esperarSubida();
    } catch (SQLException ex) {
      Logger.getGlobal().log(Level.SEVERE, null, ex);
    }


    return false;
  }
}
