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
import com.google.api.services.fusiontables.model.Sqlresponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Clase encargada del cálculo y la subida de los
 *
 * @author Antonio Fernández Ares (antares.es@gmail.com)
 */
public class TrazasPorHoras {

  private Config _c = new Config();
  private Debug _d = new Debug();
  private conectarFusionTables cFT = new conectarFusionTables();
  private String fecha;
  private ResultSet rs;
  private final String TABLAID = _c.get("ft.TRAZASPORHORAS.ID");
  private final List<String> campos = new ArrayList<>();
  public boolean check = false;

  //ResultSet rs = st.executeQuery("CALL agrupaPasosPorIntervalosNodo('2013-01-07 00:00:00','2013-06-02 00:00:00'," + 60 + ",'" + idNodo + "')");
  public TrazasPorHoras(String fecha) {
    this.fecha = fecha;
    campos.add("Fecha");
    campos.add("Origen");
    campos.add("Destino");
    campos.add("total");
    campos.add("Diferencia");
    campos.add("poligono");
    campos.add("distancia");
  }
  
public TrazasPorHoras() {
    campos.add("Fecha");
    campos.add("Origen");
    campos.add("Destino");
    this.setFechaUltima();
    campos.add("total");
    //Añadir campos de información del nodo
    campos.add("Diferencia");
    campos.add("poligono");
    campos.add("distancia");
  }

  public String getFecha() {
    return fecha;
  }
  
  public String setFechaUltima(){
    Sqlresponse r = cFT.select(TABLAID,"Fecha","","ORDER BY \'Fecha\' DESC LIMIT 1" );  
    this.fecha = (String) r.getRows().get(0).get(0);
    return fecha;
  }

  public boolean calcular() {
    Conectar conectar = new Conectar();
    try {
      Statement st = conectar.crearSt();
      //System.out.println("CALL agrupaPasosPorIntervalosNodosSeparados('" + fecha + "','" + _d.sdf.format(Calendar.getInstance().getTime()) + "','" + 60 + "')");
      rs = st.executeQuery("CALL localizaTrazasNodos2('" + fecha + "','" + _d.sdf.format(Calendar.getInstance().getTime()) + "','" + 60 + "')");
      
//Depuración del método
      //rs = st.executeQuery("CALL localizaTrazasNodos2('" + "2010-12-10 00:00:00" + "','" + "2014-12-12 00:00:00"  + "','" + 60 + "')");

      List<String> valores = new ArrayList<>();

      while (rs.next()) {
        //System.err.println("->" + rs.getString(1) + " " + rs.getString(2) + " " + rs.getString(3));
        valores.add(rs.getString(1)); //Fecha
        valores.add(rs.getString(2)); //Origen
        valores.add(rs.getString(3)); //Destino
        valores.add(rs.getString(4)); //total
        valores.add(rs.getString(5)); //Diferencia
        
        
        
        //Aquí empieza lo bueno
        String poligono = "<LineString>  <coordinates>   "+rs.getString(7)+", "+rs.getString(6)+", 0.     "+rs.getString(9)+", "+rs.getString(8)+", 0.  </coordinates> </LineString>";
   
        valores.add(poligono); //nombre
        
        double earthRadius = 3958.75;
        double dLat = Math.toRadians(rs.getDouble(8)-rs.getDouble(6));
        double dLng = Math.toRadians(rs.getDouble(9)-rs.getDouble(7));
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
               Math.cos(Math.toRadians(rs.getDouble(6))) * Math.cos(Math.toRadians(rs.getDouble(8))) *
               Math.sin(dLng/2) * Math.sin(dLng/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double dist = earthRadius * c;

        int meterConversion = 1609;

        float t = (float) dist * meterConversion;
        
        valores.add(Float.toString(t));
        
        
        cFT.insert(TABLAID, campos, valores, check);
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
