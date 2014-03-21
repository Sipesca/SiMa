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
public class PasosPorDias {

  private Config _c = new Config();
  private Debug _d = new Debug();
  private conectarFusionTables cFT = new conectarFusionTables();
  private String fecha;
  private ResultSet rs;
  private final String TABLAID = _c.get("ft.PASOSPORDIA.ID");
  private final List<String> campos = new ArrayList<>();
  public boolean check = false;

  //ResultSet rs = st.executeQuery("CALL agrupaPasosPorIntervalosNodo('2013-01-07 00:00:00','2013-06-02 00:00:00'," + 60 + ",'" + idNodo + "')");
  public PasosPorDias(String fecha) {
    this.fecha = fecha;
    campos.add("Intervalo");
    campos.add("idNodo");
    campos.add("Total");
    campos.add("Predicho");

    //Añadir campos de información del nodo
    //campos.add("latitud");
    //campos.add("longitud");
    //campos.add("nombre");
    campos.add("poligono");
  }
  
public PasosPorDias() {
    campos.add("Intervalo");
    campos.add("idNodo");
    campos.add("Total");
    this.setFechaUltima();
    campos.add("Predicho");
    //Añadir campos de información del nodo
    //campos.add("latitud");
    //campos.add("longitud");
    //campos.add("nombre");
    campos.add("poligono");
  }
  
  
  
  public String setFechaUltima(){
    Sqlresponse r = cFT.select(TABLAID,"Intervalo","","ORDER BY \'Intervalo\' DESC LIMIT 1" );  
    this.fecha = (String) r.getRows().get(0).get(0);
    return fecha;
  }

    public boolean calcular() {
    Conectar conectar = new Conectar();
    try {
      Statement st = conectar.crearSt();
      
      Logger.getGlobal().log(Level.INFO, "Calculando pasos por día en DB LOCAL");
      
      //System.out.println("CALL agrupaPasosPorIntervalosNodosSeparados('" + fecha + "','" + _d.sdf.format(Calendar.getInstance().getTime()) + "','" + 60 + "')");
      rs = st.executeQuery("CALL agrupaPasosPorIntervalosNodosSeparados('" + fecha + "','" + _d.sdf.format(Calendar.getInstance().getTime()) + "','" + 1440 + "')");

      Logger.getGlobal().log(Level.INFO, "Calculado pasos por días en DB LOCAL: OK");
      
      List<String> valores = new ArrayList<>();

      Logger.getGlobal().log(Level.INFO, "Subiendo información a la Nube");
      
      while (rs.next()) {
        //Logger.getGlobal().log(Level.INFO, "Procesando siguiente elemento a subir");
        //System.err.println("->" + rs.getString(1) + " " + rs.getString(2) + " " + rs.getString(3));
        valores.add(rs.getString(1)); //Intervalo
        valores.add(rs.getString(2)); //idNodo
        valores.add(rs.getString(3)); //Total
        valores.add(""); //El valor predicho Aquí tendríamos que lanzar el predictor y esas cosas :)
        //valores.add(rs.getString(4)); //latitud
        //valores.add(rs.getString(5)); //longitud
        //valores.add(rs.getString(6)); //nombre
        valores.add(rs.getString(7)); //poligono
        
        cFT.insert(TABLAID, campos, valores, check,2);
        valores.clear();
      }
      
      Logger.getGlobal().log(Level.INFO, "Todos los valores procesados.");
      
       cFT.forzarSync();
       
       Logger.getGlobal().log(Level.INFO, "Esperando al envío y confirmación de los valores en la nube");
       
       cFT.esperarSubida();
       
       Logger.getGlobal().log(Level.INFO, "Todos los valores subidos a la nube");
       
    } catch (SQLException ex) {
      Logger.getGlobal().log(Level.SEVERE, "Fallo en cálculo de los pasos. " + ex.getMessage(), ex);
    }


    return false;
  }
}
